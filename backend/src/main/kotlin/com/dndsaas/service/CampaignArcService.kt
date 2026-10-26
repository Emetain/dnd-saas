package com.dndsaas.service

import com.dndsaas.domain.GenerationType
import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.domain.User
import com.dndsaas.dto.CampaignGenerationRequest
import com.dndsaas.dto.CampaignGenerationResult
import com.dndsaas.dto.CampaignJobResponse
import com.dndsaas.dto.CampaignJobStatus
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 * Generates a campaign together with all its sessions (Pro+ and up).
 *
 * That is one AI call for the world and Session 1, then one call per batch of
 * sessions — minutes of work for a long campaign — so it runs in the background
 * and the frontend polls [status] for real progress.
 *
 * The whole campaign is charged once at the end, at a discount ([price]). If a
 * step fails, the user keeps what was made and pays only for that, at the
 * normal one-by-one prices, never more than the quote.
 *
 * Jobs live in memory: fine for a single backend instance, and a job lost on a
 * restart has not been charged yet.
 */
@Service
class CampaignArcService(
    private val campaignGeneratorService: CampaignGeneratorService,
    private val sessionPlannerService: SessionPlannerService,
    private val tokenService: TokenService,
    private val userService: UserService,
) {
    private val log = LoggerFactory.getLogger(CampaignArcService::class.java)

    private val executor = Executors.newFixedThreadPool(PARALLEL_JOBS) { task ->
        Thread(task, "campaign-arc").apply { isDaemon = true }
    }
    private val jobs = ConcurrentHashMap<String, Job>()

    /**
     * 50 for the world and Session 1 (like the normal campaign generator), 25 for every
     * further session (like "Next session"), minus 25 for every 5 sessions.
     */
    fun price(sessions: Int): Int =
        GenerationType.CAMPAIGN.tokenCost + (sessions - 1) * GenerationType.NEXT_SESSION.tokenCost -
            (sessions / 5) * DISCOUNT_PER_FIVE

    fun start(userId: Long, request: CampaignGenerationRequest): CampaignJobResponse {
        val sessions = request.sessionCount
        require(sessions in MIN_SESSIONS..MAX_SESSIONS) {
            "A campaign with all its sessions can have $MIN_SESSIONS to $MAX_SESSIONS sessions"
        }
        val user = userService.findEntity(userId)
        if (!user.emailVerified) throw EmailNotVerifiedException()
        if (user.subscriptionTier < SubscriptionTier.PRO_PLUS) {
            throw TierRestrictionException(
                "Generating a campaign with all its sessions requires Pro+ or higher",
                SubscriptionTier.PRO_PLUS,
            )
        }
        require(jobs.values.none { it.userId == userId && it.status == CampaignJobStatus.RUNNING }) {
            "You are already generating a campaign — wait until it is finished"
        }
        val cost = price(sessions)
        tokenService.requireBalance(userId, cost.toLong())

        forgetOldJobs()
        val job = Job(userId = userId, sessionsTotal = sessions, cost = cost)
        jobs[job.id] = job
        executor.execute { run(job, user, request.copy(expectedLength = "$sessions sessions")) }
        return job.toResponse()
    }

    fun status(userId: Long, jobId: String): CampaignJobResponse =
        jobs[jobId]?.takeIf { it.userId == userId }?.toResponse()
            ?: throw NoSuchElementException("Campaign generation $jobId not found")

    private fun run(job: Job, user: User, request: CampaignGenerationRequest) {
        try {
            val result = campaignGeneratorService.generateForArc(request, user)
            job.result = result
            job.campaignId = result.campaign.id
            job.sessionTitles += result.firstSession.title
            job.stepsDone = 1

            while (job.sessionsPlanned < job.sessionsTotal) {
                val from = job.sessionsPlanned + 1
                val to = minOf(from + BATCH_SIZE - 1, job.sessionsTotal)
                val saved = sessionPlannerService.planBatch(result.campaign.id, from, to, job.sessionsTotal, request.mock)
                check(saved.isNotEmpty()) { "The AI returned no sessions for Sessions $from–$to" }
                job.sessionTitles += saved.map { it.title }
                job.stepsDone++
            }

            job.charged = charge(job, job.cost, "Campaign with ${job.sessionsTotal} sessions")
            job.status = CampaignJobStatus.DONE
        } catch (e: Exception) {
            log.error("Campaign generation {} failed after {} sessions", job.id, job.sessionsPlanned, e)
            job.error = e.message ?: "The generation failed"
            if (job.campaignId != null) {
                val made = GenerationType.CAMPAIGN.tokenCost + (job.sessionsPlanned - 1) * GenerationType.NEXT_SESSION.tokenCost
                job.charged = runCatching {
                    charge(job, minOf(job.cost, made), "Campaign with ${job.sessionsPlanned} of ${job.sessionsTotal} sessions (stopped early)")
                }.getOrElse { 0 }
            }
            job.status = CampaignJobStatus.FAILED
        }
    }

    /** Charges up to [amount]; if the user spent tokens elsewhere meanwhile, takes what is left. */
    private fun charge(job: Job, amount: Int, description: String): Int {
        val affordable = minOf(amount.toLong(), tokenService.getBalance(job.userId)).toInt()
        if (affordable < amount) log.warn("User {} could only pay {} of {} tokens for job {}", job.userId, affordable, amount, job.id)
        if (affordable > 0) tokenService.chargeTokens(job.userId, affordable.toLong(), null, description, job.campaignId)
        return affordable
    }

    private fun forgetOldJobs() {
        val cutoff = Instant.now().minus(KEEP_FINISHED_JOBS)
        jobs.values.removeIf { it.status != CampaignJobStatus.RUNNING && it.startedAt.isBefore(cutoff) }
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdownNow()
    }

    private class Job(val userId: Long, val sessionsTotal: Int, val cost: Int) {
        val id: String = UUID.randomUUID().toString()
        val startedAt: Instant = Instant.now()
        val sessionTitles = CopyOnWriteArrayList<String>()

        @Volatile var status = CampaignJobStatus.RUNNING
        @Volatile var stepsDone = 0
        @Volatile var campaignId: Long? = null
        @Volatile var result: CampaignGenerationResult? = null
        @Volatile var charged: Int? = null
        @Volatile var error: String? = null

        val sessionsPlanned: Int get() = sessionTitles.size

        fun toResponse(): CampaignJobResponse {
            val remaining = sessionsTotal - sessionsPlanned
            return CampaignJobResponse(
                id = id,
                status = status,
                sessionsTotal = sessionsTotal,
                sessionsPlanned = sessionsPlanned,
                stepsDone = stepsDone,
                // The world first, then batches; recomputed as batches may come back smaller.
                stepsTotal = if (stepsDone == 0) 1 + ceilDiv(sessionsTotal - 1, BATCH_SIZE) else stepsDone + ceilDiv(remaining, BATCH_SIZE),
                cost = cost,
                charged = charged,
                campaignId = campaignId,
                sessionTitles = sessionTitles.toList(),
                result = result,
                error = error,
            )
        }
    }

    private companion object {
        const val MIN_SESSIONS = 2
        const val MAX_SESSIONS = 30
        const val DISCOUNT_PER_FIVE = 25

        /** Sessions planned per AI call: enough to keep the story coherent, small enough for one reply. */
        const val BATCH_SIZE = 4
        const val PARALLEL_JOBS = 2
        val KEEP_FINISHED_JOBS: Duration = Duration.ofHours(1)

        fun ceilDiv(a: Int, b: Int) = if (a <= 0) 0 else (a + b - 1) / b
    }
}

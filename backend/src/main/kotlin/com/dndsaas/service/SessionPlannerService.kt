package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.CampaignKind
import com.dndsaas.domain.GenerationType
import com.dndsaas.domain.Session
import com.dndsaas.dto.GeneratedFirstSession
import com.dndsaas.dto.GenerationRequest
import com.dndsaas.dto.NextSessionRequest
import com.dndsaas.dto.NextSessionResult
import com.dndsaas.dto.SessionRewriteRequest
import com.dndsaas.repository.SessionRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Plans sessions of a campaign: the next one, a rewrite of one that has not been
 * played yet, or a batch of them when a campaign is generated with all its sessions.
 *
 * Most of the world already exists, so this is cheaper than generating a
 * campaign: it builds on Campaign Memory (collected automatically by the AI
 * pipeline), the campaign's original idea, and — most importantly — the DM's
 * debriefs of what actually happened in the sessions played so far.
 */
@Service
class SessionPlannerService(
    private val aiService: AiService,
    private val campaignService: CampaignService,
    private val sessionRepository: SessionRepository,
    private val sessionService: SessionService,
    private val objectMapper: ObjectMapper,
) {

    /**
     * Adds a session after the last one. If the last session has been played it
     * continues from the DM's debrief; if it is still only a plan (e.g. the DM is
     * making the campaign longer), it continues from where that plan ends.
     */
    @Transactional
    fun planNext(campaignId: Long, request: NextSessionRequest): NextSessionResult {
        val campaign = campaignService.findEntity(campaignId)
        require(campaign.kind == CampaignKind.CAMPAIGN) {
            "Next sessions are for campaigns — a one-shot is played in a single session"
        }

        val sessions = sessionRepository.findByCampaignIdOrderBySessionNumberAsc(campaignId)
        val last = sessions.lastOrNull()
        val nextNumber = (last?.sessionNumber ?: 0) + 1

        val instruction = buildString {
            appendLine("Plan the next session of this campaign: Session $nextNumber.")
            appendIdea(campaign)
            if (last != null) {
                appendLine()
                if (last.isPlayed && last.summary.isBlank()) {
                    appendLine("## The last session was played, roughly as planned (Session ${last.sessionNumber}: ${last.title})")
                    appendLine("The DM added no notes, so continue from where this plan ends:")
                    appendLine(last.notes.take(PLAN_CONTEXT_CHARACTERS))
                } else if (last.isPlayed) {
                    appendLine("## What happened in the last session (Session ${last.sessionNumber}: ${last.title})")
                    appendLine(last.summary)
                    if (last.notes.isNotBlank()) {
                        appendLine()
                        appendLine("## What had been planned for that session")
                        appendLine(last.notes.take(PLAN_CONTEXT_CHARACTERS))
                    }
                } else {
                    appendLine("## The last session has not been played yet (Session ${last.sessionNumber}: ${last.title})")
                    appendLine("Continue the story from where this plan ends:")
                    appendLine(last.notes.take(PLAN_CONTEXT_CHARACTERS))
                }
            }
            appendWishes(request.wishes)
        }

        val plan = generatePlan(campaign, GenerationType.NEXT_SESSION, instruction, request.mock)
        val session = sessionRepository.save(
            Session(
                campaign = campaign,
                title = plan.plan.title.ifBlank { "Session $nextNumber" },
                sessionNumber = nextNumber,
                notes = SessionMarkdown.plan(plan.plan),
                // Left empty on purpose: the summary is what *happened*, filled in by the debrief.
                summary = "",
            ),
        )
        return plan.toResult(session)
    }

    /**
     * Re-plans a session that has not been played yet, taking the DM's wishes and
     * everything that happened (or is planned) before it into account.
     */
    @Transactional
    fun rewrite(sessionId: Long, request: SessionRewriteRequest): NextSessionResult {
        val session = sessionService.findEntity(sessionId)
        require(!session.isPlayed) {
            "Session ${session.sessionNumber} has already been played — its notes are the campaign's history now"
        }
        val campaign = checkNotNull(session.campaign)
        val sessions = sessionRepository.findByCampaignIdOrderBySessionNumberAsc(campaign.id!!)
        val following = sessions.firstOrNull { it.sessionNumber == session.sessionNumber + 1 }

        val instruction = buildString {
            appendLine("Rewrite the plan for Session ${session.sessionNumber} of this campaign.")
            appendIdea(campaign)
            appendHistory(sessions.filter { it.sessionNumber < session.sessionNumber })
            appendLine()
            appendLine("## The current plan for this session (keep what still fits)")
            appendLine(session.notes.take(PLAN_CONTEXT_CHARACTERS))
            if (following != null) {
                appendLine()
                appendLine("## The session after it is planned as \"${following.title}\"")
                appendLine("Lead into it where possible, unless the DM's wishes below change the direction.")
            }
            appendWishes(request.wishes.ifBlank { "No specific wishes — make it fit the sessions before it and make it better." })
        }

        val plan = generatePlan(campaign, GenerationType.SESSION_REWRITE, instruction, request.mock)
        session.title = plan.plan.title.ifBlank { session.title }
        session.notes = SessionMarkdown.plan(plan.plan)
        return plan.toResult(sessionRepository.save(session))
    }

    /**
     * Plans sessions [from]..[to] of a campaign that will run for [total] sessions,
     * in one AI call. Used when a campaign is generated with all its sessions; the
     * caller pays for the whole campaign, so this call is free.
     *
     * Returns the sessions actually saved — the model may return fewer than asked,
     * and the caller simply continues from the last one.
     */
    @Transactional
    fun planBatch(campaignId: Long, from: Int, to: Int, total: Int, mock: Boolean): List<Session> {
        val campaign = campaignService.findEntity(campaignId)
        val count = to - from + 1
        val instruction = buildString {
            appendLine("This campaign will run for $total sessions. Plan Sessions $from to $to ($count sessions).")
            appendLine(
                "Pace the story over all $total sessions: set-up early, rising stakes and twists in the middle, " +
                    "and a finale in Session $total that resolves the central conflict.",
            )
            appendIdea(campaign)
            appendHistory(sessionRepository.findByCampaignIdOrderBySessionNumberAsc(campaignId))
        }

        val response = aiService.generate(
            campaign = campaign,
            request = GenerationRequest(
                type = GenerationType.CAMPAIGN_SESSIONS,
                instruction = instruction,
                temperature = 0.8,
                mock = mock,
            ),
            jsonSchema = """{ "sessions": [ exactly $count objects, one per session in order, each shaped like: $sessionSchema ] }""",
            requiredFields = listOf("sessions"),
            systemGuidance = "$guidance\n- Return exactly $count sessions, in order, each continuing from the one before it.",
            tokenCost = 0,
        )

        return response.content.path("sessions")
            .take(count)
            .mapIndexed { index, node ->
                val plan = objectMapper.treeToValue(node, GeneratedFirstSession::class.java)
                val number = from + index
                sessionRepository.save(
                    Session(
                        campaign = campaign,
                        title = plan.title.ifBlank { "Session $number" },
                        sessionNumber = number,
                        notes = SessionMarkdown.plan(plan),
                        summary = "",
                    ),
                )
            }
    }

    // -----------------------------------------------------------------------

    private fun StringBuilder.appendIdea(campaign: Campaign) {
        appendLine()
        appendLine("## The campaign's idea")
        appendLine(campaign.description.ifBlank { campaign.name })
    }

    /**
     * The sessions so far: the most recent ones in detail (what happened, or what is
     * planned), older ones by title only, so long campaigns don't flood the prompt.
     */
    private fun StringBuilder.appendHistory(sessions: List<Session>) {
        if (sessions.isEmpty()) return
        appendLine()
        appendLine("## Sessions so far")
        val detailedFrom = sessions.size - DETAILED_SESSIONS
        sessions.forEachIndexed { index, session ->
            val state = if (session.isPlayed) "played" else "planned, not played yet"
            appendLine("### Session ${session.sessionNumber}: ${session.title} ($state)")
            if (index >= detailedFrom) {
                appendLine(session.summary.ifBlank { session.notes }.take(HISTORY_CHARACTERS))
            }
        }
    }

    private fun StringBuilder.appendWishes(wishes: String) {
        if (wishes.isBlank()) return
        appendLine()
        appendLine("## What the DM wants (honour this)")
        appendLine(wishes)
    }

    private class Plan(val plan: GeneratedFirstSession, val response: com.dndsaas.dto.GenerationResponse)

    private fun generatePlan(campaign: Campaign, type: GenerationType, instruction: String, mock: Boolean): Plan {
        val response = aiService.generate(
            campaign = campaign,
            request = GenerationRequest(type = type, instruction = instruction, temperature = 0.8, mock = mock),
            jsonSchema = sessionSchema,
            requiredFields = listOf("title", "openingScene"),
            systemGuidance = guidance,
        )
        return Plan(objectMapper.treeToValue(response.content, GeneratedFirstSession::class.java), response)
    }

    private fun Plan.toResult(session: Session) = NextSessionResult(
        session = sessionService.toResponse(session),
        plan = plan,
        usage = response.usage,
        model = response.model,
        mocked = response.mocked,
    )

    private val sessionSchema = """
        {
          "title": "an evocative title for this session",
          "previouslyOn": "a short 'Previously on…' recap to read aloud to the players, in second person",
          "summary": "what this session is about, for the DM",
          "openingScene": "read-aloud text that picks up exactly where the last session ended",
          "beats": ["the scenes to hit, in order"],
          "encounters": ["combat, social or exploration encounters"],
          "cliffhanger": "how to end so players want more",
          "dmTips": ["practical advice for running this session"]
        }
    """.trimIndent()

    private val guidance = """
        You are planning sessions of a campaign that is already being played or prepared.

        Requirements:
        - Continue from where the previous session ended; never ignore or undo what the players did in played sessions.
        - Pay off the players' decisions, react to how they treated NPCs, and follow up loose threads.
        - Honour the DM's wishes.
        - Reuse existing NPCs, locations, factions and quests by their exact names before inventing new ones.
        - Keep the campaign's original idea and central conflict moving forward.
        - Every session must be runnable: an opening scene, several beats and at least one encounter.
    """.trimIndent()

    private companion object {
        /** Enough of the previous plan to show what was skipped, without drowning the prompt. */
        const val PLAN_CONTEXT_CHARACTERS = 2_000

        /** How many of the latest sessions are described in full; older ones only by title. */
        const val DETAILED_SESSIONS = 4
        const val HISTORY_CHARACTERS = 900
    }
}

package com.dndsaas.service

import com.dndsaas.domain.AdSession
import com.dndsaas.domain.AdView
import com.dndsaas.domain.TokenTransactionType
import com.dndsaas.dto.AdTicketResponse
import com.dndsaas.dto.AdViewResponse
import com.dndsaas.repository.AdSessionRepository
import com.dndsaas.repository.AdViewRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Service for rewarded ads: watch an ad, earn tokens.
 *
 * Each ad is a two-step ticket so tokens cannot be claimed by just calling the
 * API: [startAd] issues a one-time ticket when the ad opens, and [completeAd]
 * redeems it once the ad could have finished. Daily and monthly limits apply.
 *
 * Note: web ad networks (e.g. Google Ad Manager rewarded ads) report the reward
 * in the browser only, so a determined user could still script the two calls.
 * The limits keep that harmless; a network with server-side verification
 * callbacks would close it completely.
 */
@Service
@Transactional
class AdService(
    private val adViewRepository: AdViewRepository,
    private val adSessionRepository: AdSessionRepository,
    private val userService: UserService,
    private val tokenService: TokenService,
    /** Shortest time an ad can take; rewarded video ads are usually 15-30 seconds. */
    @Value("\${app.ads.min-watch-seconds:10}") private val minWatchSeconds: Long,
) {

    companion object {
        const val TOKENS_PER_AD_VIEW = 5L
        const val MAX_ADS_PER_DAY = 3 // Keeps free earning well below a paid tier

        /** An unfinished ticket expires after this long. */
        private const val TICKET_LIFETIME_MINUTES = 30L
    }

    /**
     * Step 1: an ad is about to play. Checks the limits up front, so nobody
     * watches an ad that can no longer pay out, and issues a ticket.
     */
    fun startAd(userId: Long, provider: String): AdTicketResponse {
        val user = userService.findEntity(userId)
        if (getRemainingAdsToday(userId) <= 0) throw AdUnavailableException("Daily ad limit reached. Come back tomorrow!")
        if (tokenService.remainingCappedEarnings(userId) <= 0) {
            throw AdUnavailableException("Monthly earning limit reached. It resets on the 1st.")
        }
        val session = adSessionRepository.save(
            AdSession(user = user, provider = provider.take(40).ifBlank { "unknown" }, ticket = UUID.randomUUID().toString()),
        )
        return AdTicketResponse(ticket = session.ticket, minWatchSeconds = minWatchSeconds, tokensReward = TOKENS_PER_AD_VIEW)
    }

    /**
     * Step 2: the ad network reported the ad as watched. Redeems the ticket
     * once — and only if enough time has passed for the ad to have played.
     * Returns the tokens awarded (less than a full reward near the monthly cap).
     */
    fun completeAd(userId: Long, ticket: String): Long {
        val session = adSessionRepository.findByTicket(ticket)
            ?.takeIf { it.user?.id == userId }
            ?: throw NoSuchElementException("Unknown ad ticket")
        if (session.completedAt != null) throw AdUnavailableException("This ad has already been rewarded")

        val age = Duration.between(session.createdAt, Instant.now())
        if (age.toMinutes() >= TICKET_LIFETIME_MINUTES) throw AdUnavailableException("This ad expired — please watch a new one")
        if (age.seconds < minWatchSeconds) throw AdUnavailableException("The ad has not finished yet")
        if (getRemainingAdsToday(userId) <= 0) throw AdUnavailableException("Daily ad limit reached. Come back tomorrow!")

        session.completedAt = Instant.now()
        val awarded = tokenService.rewardTokens(
            userId = userId,
            type = TokenTransactionType.AD_VIEW_REWARD,
            amount = TOKENS_PER_AD_VIEW,
            description = "Watched an ad (${session.provider})",
        )
        adViewRepository.save(AdView(user = session.user, adId = session.provider, tokensEarned = awarded, adDetails = ticket))
        return awarded
    }

    /**
     * Get the user's ad view history.
     */
    fun getViewHistory(userId: Long): List<AdViewResponse> {
        val user = userService.findEntity(userId)
        return adViewRepository.findAll()
            .filter { it.user?.id == userId }
            .sortedByDescending { it.createdAt }
            .map(::toResponse)
    }

    /**
     * Get remaining ads the user can watch today.
     */
    fun getRemainingAdsToday(userId: Long): Int {
        val user = userService.findEntity(userId)
        val viewsToday = adViewRepository.countViewsSince(user, Instant.now().minus(1, ChronoUnit.DAYS))
        return (MAX_ADS_PER_DAY - viewsToday).toInt().coerceAtLeast(0)
    }

    /**
     * Get total tokens earned from ads.
     */
    fun getTotalTokensEarnedFromAds(userId: Long): Long {
        val user = userService.findEntity(userId)
        return adViewRepository.sumTokensEarnedByUser(user)
    }

    private fun toResponse(adView: AdView): AdViewResponse =
        AdViewResponse(
            id = adView.id!!,
            adId = adView.adId,
            tokensEarned = adView.tokensEarned,
            viewedAt = adView.createdAt,
        )
}


/** The ad cannot be started or rewarded right now (limits, reused or unfinished ticket). */
class AdUnavailableException(message: String) : RuntimeException(message)

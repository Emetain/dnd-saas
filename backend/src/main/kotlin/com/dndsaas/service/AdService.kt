package com.dndsaas.service

import com.dndsaas.domain.AdView
import com.dndsaas.domain.TokenTransactionType
import com.dndsaas.dto.AdViewResponse
import com.dndsaas.repository.AdViewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service for managing ad views and earning tokens from ads.
 *
 * Free users can watch ads to earn tokens. This service prevents
 * abuse by tracking which ads have been viewed and limiting rewards.
 */
@Service
@Transactional
class AdService(
    private val adViewRepository: AdViewRepository,
    private val userService: UserService,
    private val tokenService: TokenService,
) {

    companion object {
        const val TOKENS_PER_AD_VIEW = 5L
        const val MAX_ADS_PER_DAY = 3 // Keeps free earning well below a paid tier
        private const val AD_VIEW_COOLDOWN_MINUTES = 60 // Cooldown between same ad views
    }

    /**
     * Record an ad view and reward tokens.
     * Returns the tokens awarded, or 0 if the view was rejected.
     */
    fun recordAdView(userId: Long, adId: String, adDetails: String = ""): Long {
        val user = userService.findEntity(userId)

        // Check if user has already viewed this ad recently
        val recentViewCount = adViewRepository.countViewsByUserAndAdId(user, adId)
        if (recentViewCount > 0) {
            val lastView = adViewRepository.findAll()
                .filter { it.user?.id == userId && it.adId == adId }
                .maxByOrNull { it.createdAt }

            if (lastView != null) {
                val minutesSinceLastView = ChronoUnit.MINUTES.between(lastView.createdAt, Instant.now())
                if (minutesSinceLastView < AD_VIEW_COOLDOWN_MINUTES) {
                    return 0 // Cooldown still active
                }
            }
        }

        // Check daily limit
        val viewsToday = adViewRepository.countViewsSince(user, Instant.now().minus(1, ChronoUnit.DAYS))
        if (viewsToday >= MAX_ADS_PER_DAY) {
            return 0 // Daily limit reached
        }

        if (tokenService.remainingCappedEarnings(userId) <= 0) {
            return 0 // Monthly earning cap reached
        }

        // Record the view
        val adView = AdView(
            user = user,
            adId = adId,
            tokensEarned = TOKENS_PER_AD_VIEW,
            adDetails = adDetails,
        )
        adViewRepository.save(adView)

        // Reward tokens (may be less than a full reward near the monthly cap)
        return tokenService.rewardTokens(
            userId = userId,
            type = TokenTransactionType.AD_VIEW_REWARD,
            amount = TOKENS_PER_AD_VIEW,
            description = "Watched ad: $adId",
        )
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


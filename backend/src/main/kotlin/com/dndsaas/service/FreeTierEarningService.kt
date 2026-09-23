package com.dndsaas.service

import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.dto.EarningOpportunitiesResponse
import com.dndsaas.dto.FreeTierEarningOpportunity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service that coordinates free tier earning opportunities.
 *
 * Aggregates ad views, referrals, login streaks, app reviews, and social sharing
 * into a single dashboard showing users how they can earn tokens without spending money.
 */
@Service
@Transactional
class FreeTierEarningService(
    private val userService: UserService,
    private val adService: AdService,
    private val referralService: ReferralService,
    private val loginStreakService: LoginStreakService,
    private val appReviewService: AppReviewService,
    private val tokenService: TokenService,
) {

    /**
     * Get all available earning opportunities. Every tier can earn; paid users
     * simply rarely need to.
     */
    fun getEarningOpportunities(userId: Long): EarningOpportunitiesResponse {
        val user = userService.findEntity(userId)
        val opportunities = mutableListOf<FreeTierEarningOpportunity>()

        opportunities.add(buildAdOpportunity(userId))
        opportunities.add(buildReferralOpportunity(userId))
        opportunities.add(buildLoginStreakOpportunity(userId))
        opportunities.add(buildAppReviewOpportunity(userId))
        opportunities.add(buildSocialShareOpportunity())

        return EarningOpportunitiesResponse(
            userId = userId,
            currentTokens = user.totalTokens,
            opportunities = opportunities,
        )
    }

    private fun buildAdOpportunity(userId: Long): FreeTierEarningOpportunity {
        val remainingToday = adService.getRemainingAdsToday(userId)
        val remainingThisMonth = tokenService.remainingCappedEarnings(userId)
        val isAvailable = remainingToday > 0 && remainingThisMonth > 0

        return FreeTierEarningOpportunity(
            type = "ad",
            title = "Watch Ads",
            description = "Watch short ads to earn tokens. No strings attached.",
            tokensReward = AdService.TOKENS_PER_AD_VIEW,
            isAvailable = isAvailable,
            reason = when {
                remainingThisMonth <= 0 -> "Monthly earning limit reached. It resets on the 1st."
                remainingToday <= 0 -> "Daily limit reached. Come back tomorrow!"
                else -> null
            },
            metadata = mapOf(
                "remaining_today" to remainingToday,
                "max_per_day" to AdService.MAX_ADS_PER_DAY,
                "monthly_cap" to TokenService.MONTHLY_EARNING_CAP,
                "remaining_this_month" to remainingThisMonth,
                "total_earned" to adService.getTotalTokensEarnedFromAds(userId),
            ) as Map<String, Any>?,
        )
    }

    private fun buildReferralOpportunity(userId: Long): FreeTierEarningOpportunity {
        val referralStats = referralService.getReferralStats(userId)

        return FreeTierEarningOpportunity(
            type = "referral",
            title = "Refer a Friend",
            description = "Share your referral code with friends. Earn tokens when they sign up and take their first action.",
            tokensReward = 50,
            isAvailable = true,
            reason = null,
            metadata = mapOf(
                "referral_code" to referralStats.referralCode,
                "referrals_pending" to referralStats.referredCount,
                "referrals_completed" to referralStats.completedCount,
                "total_earned" to referralService.getTotalTokensEarnedFromReferrals(userId),
            ) as Map<String, Any>?,
        )
    }

    private fun buildLoginStreakOpportunity(userId: Long): FreeTierEarningOpportunity {
        val streakInfo = loginStreakService.getStreakInfo(userId)
        val isAvailable = true // Can always log in tomorrow

        return FreeTierEarningOpportunity(
            type = "login_streak",
            title = "Daily Login Streak",
            description = "Log in every day to build a streak. Earn bonus tokens for maintaining consecutive days.",
            tokensReward = (1 + (if (streakInfo.currentStreak > 0 && streakInfo.currentStreak % 7 == 0) 25 else 0)).toLong(),
            isAvailable = isAvailable,
            reason = null,
            metadata = mapOf(
                "current_streak" to streakInfo.currentStreak,
                "longest_streak" to streakInfo.longestStreak,
                "next_milestone" to streakInfo.nextReward,
                "total_earned" to streakInfo.totalTokensEarned,
            ) as Map<String, Any>?,
        )
    }

    private fun buildAppReviewOpportunity(userId: Long): FreeTierEarningOpportunity {
        val googlePlayRewarded = appReviewService.hasBeenRewarded(userId, "google_play")
        val appStoreRewarded = appReviewService.hasBeenRewarded(userId, "app_store")

        val availablePlatforms = mutableListOf<String>()
        if (!googlePlayRewarded) availablePlatforms.add("google_play")
        if (!appStoreRewarded) availablePlatforms.add("app_store")

        val isAvailable = availablePlatforms.isNotEmpty()

        return FreeTierEarningOpportunity(
            type = "app_review",
            title = "Leave an App Review",
            description = "Help us improve! Leave a review on your app store. One-time reward per platform.",
            tokensReward = 25,
            isAvailable = isAvailable,
            reason = if (!isAvailable) "You've already left reviews on all available platforms" else null,
            metadata = mapOf(
                "available_platforms" to availablePlatforms,
                "already_reviewed" to if (googlePlayRewarded) listOf("google_play") else emptyList(),
                "total_earned" to appReviewService.getTotalReviewRewards(userId),
            ) as Map<String, Any>?,
        )
    }

    private fun buildSocialShareOpportunity(): FreeTierEarningOpportunity {
        // Social share tracking would need additional implementation
        // For now, this is a placeholder showing the feature concept

        return FreeTierEarningOpportunity(
            type = "social_share",
            title = "Share on Social Media",
            description = "Share the app with your gaming community on Twitter, Reddit, or Discord. Limited daily.",
            tokensReward = 10,
            isAvailable = false,
            reason = "Coming soon",
            metadata = mapOf(
                "platforms" to listOf("twitter", "reddit", "discord"),
                "daily_limit" to 2,
                "remaining_today" to 2, // This would need actual tracking
            ) as Map<String, Any>?,
        )
    }

    /**
     * Calculate total tokens a user has earned from all free tier sources.
     */
    fun getTotalFreeTierEarnings(userId: Long): Long {
        var total = 0L
        total += adService.getTotalTokensEarnedFromAds(userId)
        total += referralService.getTotalTokensEarnedFromReferrals(userId)
        total += loginStreakService.getStreakInfo(userId).totalTokensEarned
        total += appReviewService.getTotalReviewRewards(userId)
        // Add social share earnings when implemented
        return total
    }

    /**
     * Get a summary of how much users typically earn from free actions.
     * Useful for marketing material and onboarding copy.
     */
    fun getFreeTierPotentialEarnings(): Map<String, Long> = mapOf(
        "ads_daily" to AdService.TOKENS_PER_AD_VIEW * AdService.MAX_ADS_PER_DAY,
        "ads_and_login_monthly_cap" to TokenService.MONTHLY_EARNING_CAP,
        "referrals_per_friend" to 50,
        "login_streak_daily" to 1,
        "login_streak_weekly_bonus" to 25,
        "app_review_per_platform" to 25,
        "social_share_daily" to (10 * 2), // 10 tokens × 2 max per day
    )
}


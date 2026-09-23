package com.dndsaas.orchestration

import com.dndsaas.config.StripeProperties
import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.domain.TokenPack
import com.dndsaas.dto.AdTicketResponse
import com.dndsaas.dto.EarningOpportunitiesResponse
import com.dndsaas.dto.LoginStreakResponse
import com.dndsaas.dto.ReferralResponse
import com.dndsaas.dto.SubscriptionDetailsResponse
import com.dndsaas.dto.TokenBalanceResponse
import com.dndsaas.dto.TokenEarnedResponse
import com.dndsaas.dto.UserResponse
import com.dndsaas.dto.UserRegistrationRequest
import com.dndsaas.service.AlreadySubscribedException
import com.dndsaas.service.AppReviewService
import com.dndsaas.service.FreeTierEarningService
import com.dndsaas.service.LoginStreakService
import com.dndsaas.service.ReferralService
import com.dndsaas.service.SubscriptionService
import com.dndsaas.service.TokenService
import com.dndsaas.service.UserService
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Orchestration layer for billing and subscription management.
 *
 * Coordinates between multiple services to provide complete billing workflows.
 */
@Component
class BillingOrchestrator(
    private val userService: UserService,
    private val tokenService: TokenService,
    private val subscriptionService: SubscriptionService,
    private val freeTierEarningService: FreeTierEarningService,
    private val referralService: ReferralService,
    private val loginStreakService: LoginStreakService,
    private val appReviewService: AppReviewService,
    private val adService: com.dndsaas.service.AdService,
    private val stripeProperties: StripeProperties,
) {

    /** Free plan changes and pack purchases exist for testing only; with Stripe on, money goes through checkout. */
    private fun requireTestMode() {
        if (stripeProperties.isConfigured) {
            throw AlreadySubscribedException("Plans and token packs are bought through checkout")
        }
    }

    // -------- User Registration & Account --------

    fun registerUser(request: UserRegistrationRequest): UserResponse =
        userService.register(request)

    fun getUserAccount(userId: Long): UserResponse =
        userService.get(userId)

    // -------- Token Management --------

    fun getTokenBalance(userId: Long): TokenBalanceResponse =
        tokenService.getBalanceDetails(userId)

    fun getTokenHistory(userId: Long, limit: Int = 50): List<com.dndsaas.dto.TokenTransactionResponse> =
        tokenService.getTransactionHistory(userId, limit)

    fun purchaseTokens(userId: Long, pack: TokenPack): TokenBalanceResponse {
        requireTestMode()
        tokenService.purchaseTokens(userId, pack)
        return tokenService.getBalanceDetails(userId)
    }

    // -------- Subscription Management --------

    fun upgradeSubscription(userId: Long, newTier: SubscriptionTier) {
        requireTestMode()
        subscriptionService.upgradeTier(userId, newTier)
    }

    fun getSubscriptionDetails(userId: Long): SubscriptionDetailsResponse =
        subscriptionService.getSubscriptionDetails(userId)

    fun downgradeToFree(userId: Long) {
        requireTestMode()
        subscriptionService.downgradeToFree(userId)
    }

    // -------- Free Tier Earnings --------

    fun getEarningOpportunities(userId: Long): EarningOpportunitiesResponse =
        freeTierEarningService.getEarningOpportunities(userId)

    fun getTotalEarningsFromFreeTier(userId: Long): Long =
        freeTierEarningService.getTotalFreeTierEarnings(userId)

    // -------- Ad Watching --------

    fun startAd(userId: Long, provider: String): AdTicketResponse = adService.startAd(userId, provider)

    fun completeAd(userId: Long, ticket: String): TokenEarnedResponse {
        val tokensEarned = adService.completeAd(userId, ticket)

        return TokenEarnedResponse(
            tokensEarned = tokensEarned,
            totalTokens = tokenService.getBalance(userId),
            reason = "Watched ad",
            timestamp = Instant.now(),
        )
    }

    // -------- Referral Management --------

    fun generateReferralCode(userId: Long): ReferralResponse =
        referralService.generateReferralCode(userId)

    fun getReferralStats(userId: Long): ReferralResponse =
        referralService.getReferralStats(userId)

    fun applyReferralCode(referralCode: String, newUserId: Long) {
        referralService.applyReferralCode(referralCode, newUserId)
    }

    fun completeReferral(referralCode: String) {
        referralService.completeReferral(referralCode)
    }

    // -------- Login Streak --------

    fun recordLogin(userId: Long): TokenEarnedResponse {
        val tokensEarned = loginStreakService.recordLogin(userId)
        return TokenEarnedResponse(
            tokensEarned = tokensEarned,
            totalTokens = tokenService.getBalance(userId),
            reason = "Login streak bonus",
            timestamp = Instant.now(),
        )
    }

    fun getLoginStreak(userId: Long): LoginStreakResponse =
        loginStreakService.getStreakInfo(userId)

    // -------- App Reviews --------

    fun submitAppReview(
        userId: Long,
        platformId: String,
        reviewUrl: String,
        reviewContent: String? = null,
    ): TokenEarnedResponse {
        val tokensEarned = appReviewService.submitReview(userId, platformId, reviewUrl, reviewContent)
        return TokenEarnedResponse(
            tokensEarned = tokensEarned,
            totalTokens = tokenService.getBalance(userId),
            reason = "App review reward",
            timestamp = Instant.now(),
        )
    }
}


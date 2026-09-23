package com.dndsaas.dto

import com.dndsaas.domain.SubscriptionTier
import java.time.Instant

/**
 * Response containing user account and subscription information.
 */
data class UserResponse(
    val id: Long,
    val email: String,
    val displayName: String,
    val subscriptionTier: SubscriptionTier,
    /** Total spendable tokens (allowance + purchased). */
    val platformTokens: Long,
    val allowanceTokens: Long,
    val purchasedTokens: Long,
    val createdAt: Instant,
)

/**
 * Request to create a new user account.
 */
data class UserRegistrationRequest(
    val email: String,
    val displayName: String,
    val password: String,
)

/**
 * Response containing token balance and transaction history.
 */
data class TokenBalanceResponse(
    val userId: Long,
    /** Total spendable tokens (allowance + purchased). */
    val currentTokens: Long,
    /** Monthly grant + earned tokens; rolls over up to [allowanceCap]. */
    val allowanceTokens: Long,
    /** Bought tokens; never reset. */
    val purchasedTokens: Long,
    val recentTransactions: List<TokenTransactionResponse>,
    val monthlyAllowance: Long,
    val allowanceCap: Long,
    val tokensUsedThisMonth: Long,
)

/**
 * A single token transaction.
 */
data class TokenTransactionResponse(
    val id: Long,
    val type: String, // TokenTransactionType enum name
    val amount: Long,
    val description: String,
    val campaignId: Long?,
    val generationLogId: Long?,
    val createdAt: Instant,
)

/**
 * Response when a user earns tokens from free actions.
 */
data class TokenEarnedResponse(
    val tokensEarned: Long,
    val totalTokens: Long,
    val reason: String,
    val timestamp: Instant,
)

/**
 * Subscription details response.
 */
data class SubscriptionDetailsResponse(
    val userId: Long,
    val currentTier: SubscriptionTier,
    /** A downgrade that takes effect at [billingCycleEnd], or null. */
    val pendingTier: SubscriptionTier?,
    val monthlyTokenAllowance: Long,
    val allowanceCap: Long,
    val tokensUsedThisMonth: Long,
    /** Null for Free users, who have no billing cycle. */
    val billingCycleStart: Instant?,
    val billingCycleEnd: Instant?,
    val nextBillingDate: Instant?,
)

/**
 * Free tier earning opportunity.
 */
data class FreeTierEarningOpportunity(
    val type: String, // Type of earning: "ad", "referral", "login_streak", "app_review", "social_share"
    val title: String,
    val description: String,
    val tokensReward: Long,
    val isAvailable: Boolean,
    val reason: String?, // Why it's not available if applicable
    val metadata: Map<String, Any>? = null,
)

/**
 * Response containing available earning opportunities for free users.
 */
data class EarningOpportunitiesResponse(
    val userId: Long,
    val currentTokens: Long,
    val opportunities: List<FreeTierEarningOpportunity>,
)

/**
 * Ad view record response.
 */
data class AdViewResponse(
    val id: Long,
    val adId: String,
    val tokensEarned: Long,
    val viewedAt: Instant,
)

/**
 * Login streak information.
 */
data class LoginStreakResponse(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalTokensEarned: Long,
    val lastLoginDate: Instant?,
    val nextReward: Int?, // Days until next reward
)

/**
 * Referral information.
 */
data class ReferralResponse(
    val id: Long,
    val referralCode: String,
    val status: String,
    val referredCount: Long,
    val completedCount: Long,
    val tokensEarned: Long,
)

/**
 * App review tracking.
 */
data class AppReviewResponse(
    val id: Long,
    val platformId: String,
    val tokensAwarded: Long,
    val hasBeenRewarded: Boolean,
    val reviewUrl: String,
)


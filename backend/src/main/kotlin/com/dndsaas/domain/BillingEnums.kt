package com.dndsaas.domain

/**
 * Platform subscription tiers.
 *
 * Each tier grants different monthly token allowances and features.
 */
enum class SubscriptionTier(
    val label: String,
    val monthlyTokens: Long,
) {
    FREE("Free", monthlyTokens = 50), // Starter tokens, can earn more
    PRO("Pro", monthlyTokens = 1_000),
    PRO_PLUS("Pro+", monthlyTokens = 3_000),
    ULTIMATE_DM("Ultimate DM", monthlyTokens = 5_000),
}

/**
 * Type of token transaction (for auditing and analytics).
 */
enum class TokenTransactionType {
    SUBSCRIPTION_GRANT,      // Monthly grant from subscription
    GENERATION_CHARGE,       // Charged for using a generator
    AD_VIEW_REWARD,          // Earned from watching an ad
    REFERRAL_REWARD,         // Earned from successful referral
    SOCIAL_SHARE_REWARD,     // Earned from sharing on social media
    LOGIN_STREAK_REWARD,     // Earned from maintaining login streak
    APP_REVIEW_REWARD,       // Earned from leaving app review
    MANUAL_REFUND,           // Admin refund/adjustment
}

/**
 * Status of a referral.
 */
enum class ReferralStatus {
    PENDING,       // User clicked link but hasn't signed up yet
    INVITED,       // Referred user clicked the link
    COMPLETED,     // Referred user completed the action (e.g. first generation)
    REWARDED,      // Both users have been rewarded
}


package com.dndsaas.domain

/**
 * Platform subscription tiers.
 *
 * Each tier grants different monthly token allowances and features.
 */
enum class SubscriptionTier(
    val label: String,
    val monthlyTokens: Long,
    /** Monthly price in euro cents. */
    val priceEuroCents: Int = 0,
) {
    FREE("Free", monthlyTokens = 50), // Starter tokens, can earn more
    PRO("Pro", monthlyTokens = 1_000, priceEuroCents = 1_000),
    PRO_PLUS("Pro+", monthlyTokens = 3_000, priceEuroCents = 2_500),
    ULTIMATE_DM("Ultimate DM", monthlyTokens = 5_000, priceEuroCents = 5_000),
    ;

    /**
     * Unused allowance rolls over, but never beyond two months' worth.
     * Purchased tokens are separate and not limited by this cap.
     */
    val allowanceCap: Long get() = monthlyTokens * 2
}

/**
 * Token packs that can be bought on top of a subscription; purchased tokens never expire.
 * Packs cost more per token than a subscription, so subscribing stays the better deal.
 */
enum class TokenPack(val tokens: Long, val priceEuroCents: Int) {
    SMALL(250, priceEuroCents = 500),      // €5  — €0.020 per token
    MEDIUM(1_000, priceEuroCents = 1_500), // €15 — €0.015 per token
    LARGE(2_500, priceEuroCents = 2_500),  // €25 — €0.010 per token
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
    TOKEN_PURCHASE,          // Bought a token pack (never expires)
    ALLOWANCE_EXPIRED,       // Allowance above the rollover cap removed at renewal
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


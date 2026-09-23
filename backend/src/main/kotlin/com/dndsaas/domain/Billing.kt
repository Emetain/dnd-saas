package com.dndsaas.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ForeignKey
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

/**
 * Subscription record for a user.
 *
 * Tracks when a user is/was on a particular tier and when the next billing date is.
 */
@Entity
@Table(name = "subscriptions")
class Subscription(
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(name = "fk_subscription_user"))
    var user: User? = null,

    @Enumerated(EnumType.STRING)
    var tier: SubscriptionTier = SubscriptionTier.FREE,

    /** When the current billing cycle started. */
    var billingCycleStart: Instant = Instant.now(),

    /** When the current billing cycle ends and tokens reset. */
    var billingCycleEnd: Instant = Instant.now(),

    /** Whether this subscription is currently active. */
    var isActive: Boolean = true,

    /**
     * A downgrade waiting for the end of the billing cycle. The user keeps the
     * tier they paid for until then — which also stops tokens being farmed by
     * downgrading and re-upgrading.
     */
    @Enumerated(EnumType.STRING)
    var pendingTier: SubscriptionTier? = null,

    /** Set when Stripe bills this subscription; Stripe then drives renewals and tier changes. */
    var stripeSubscriptionId: String? = null,
) : BaseEntity()

/**
 * A Stripe webhook event that has been handled. Stripe can deliver the same
 * event more than once; this makes sure tokens are only granted once.
 */
@Entity
@Table(name = "stripe_events")
class StripeEvent(
    @Column(unique = true, nullable = false)
    var eventId: String = "",

    var type: String = "",
) : BaseEntity()

/**
 * A token transaction — tracks every gain and loss of platform tokens.
 *
 * Used for auditing, analytics, and understanding user token patterns.
 */
@Entity
@Table(name = "token_transactions")
class TokenTransaction(
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(name = "fk_token_transaction_user"))
    var user: User? = null,

    @Enumerated(EnumType.STRING)
    var type: TokenTransactionType = TokenTransactionType.GENERATION_CHARGE,

    var amount: Long = 0, // Positive for gains, negative for charges

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    /** Related campaign ID if applicable. */
    var campaignId: Long? = null,

    /** Related generation log ID if this is a charge. */
    var generationLogId: Long? = null,
) : BaseEntity()

/**
 * Record of an ad view by a user.
 *
 * Tracks which ads have been shown to earn tokens.
 */
@Entity
@Table(name = "ad_views")
class AdView(
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(name = "fk_ad_view_user"))
    var user: User? = null,

    var adId: String = "", // Identifier of the ad network/campaign

    var tokensEarned: Long = 5, // Default 5 tokens per ad view

    @Column(columnDefinition = "TEXT")
    var adDetails: String = "", // Metadata about the ad for analytics
) : BaseEntity()

/**
 * Referral tracking.
 *
 * Tracks when a user refers someone and whether they completed the referral.
 */
@Entity
@Table(name = "referrals")
class Referral(
    @ManyToOne(optional = false)
    @JoinColumn(name = "referrer_user_id", foreignKey = ForeignKey(name = "fk_referral_referrer"))
    var referrer: User? = null,

    @ManyToOne
    @JoinColumn(name = "referred_user_id", foreignKey = ForeignKey(name = "fk_referral_referred"))
    var referredUser: User? = null,

    var referralCode: String = "", // Unique code the referred user should use

    @Enumerated(EnumType.STRING)
    var status: ReferralStatus = ReferralStatus.PENDING,

    var tokensAwarded: Long = 10, // Per-user token reward

    /** Whether the referrer has been paid out for this referral. */
    var referrerRewarded: Boolean = false,

    /** Whether the referred user has been rewarded. */
    var referredRewarded: Boolean = false,
) : BaseEntity()

/**
 * Login streak tracking.
 *
 * Tracks consecutive days a user logs in to encourage engagement.
 */
@Entity
@Table(name = "login_streaks")
class LoginStreak(
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(name = "fk_login_streak_user"))
    var user: User? = null,

    var currentStreak: Int = 0, // How many consecutive days

    var longestStreak: Int = 0, // All-time longest streak

    var lastLoginDate: Instant? = null, // Last time they logged in

    /** Tokens earned from this login streak (can compound). */
    var tokensEarned: Long = 0,
) : BaseEntity()

/**
 * App review management.
 *
 * Tracks when users leave app store reviews to avoid duplicate rewards.
 */
@Entity
@Table(name = "app_reviews")
class AppReview(
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(name = "fk_app_review_user"))
    var user: User? = null,

    var platformId: String = "", // e.g., "google_play", "app_store"

    var reviewUrl: String = "", // Link to the review

    var tokensAwarded: Long = 25, // One-time award for leaving a review

    var hasBeenRewarded: Boolean = false,

    @Column(columnDefinition = "TEXT")
    var reviewContent: String? = null, // Optional: the review text itself
) : BaseEntity()


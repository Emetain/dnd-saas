package com.dndsaas.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnDefault

/**
 * A user of the platform.
 *
 * Tracks authentication, subscription status, and cumulative platform tokens.
 */
@Entity
@Table(name = "users")
class User(
    @Column(unique = true, nullable = false)
    var email: String = "",

    @Column(nullable = false)
    var displayName: String = "",

    @Column(nullable = false)
    var passwordHash: String = "",

    /** Current subscription tier. */
    @Enumerated(EnumType.STRING)
    var subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,

    /**
     * Allowance tokens: the monthly subscription grant plus tokens earned from
     * free actions. Rolls over at renewal, capped at [SubscriptionTier.allowanceCap].
     * Kept on the original column so existing databases need no rename.
     */
    @Column(name = "platform_tokens")
    var allowanceTokens: Long = 50, // Free users start with 50 tokens

    /** Bought tokens. Never reset or capped, and spent only once the allowance runs out. */
    @ColumnDefault("0")
    var purchasedTokens: Long = 0,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var campaigns: MutableList<Campaign> = mutableListOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var subscriptions: MutableList<Subscription> = mutableListOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var tokenTransactions: MutableList<TokenTransaction> = mutableListOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var adViews: MutableList<AdView> = mutableListOf(),

    @OneToMany(mappedBy = "referrer", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var referrals: MutableList<Referral> = mutableListOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var loginStreaks: MutableList<LoginStreak> = mutableListOf(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var appReviews: MutableList<AppReview> = mutableListOf(),
) : BaseEntity() {

    /** Everything the user can spend right now. */
    val totalTokens: Long get() = allowanceTokens + purchasedTokens
}


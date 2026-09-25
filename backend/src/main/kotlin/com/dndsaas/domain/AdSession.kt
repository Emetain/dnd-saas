package com.dndsaas.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

/**
 * A one-time ticket for watching one rewarded ad.
 *
 * Issued when an ad starts and redeemed when it finishes. A ticket can only
 * be redeemed once, and not before the ad could have finished — so tokens
 * cannot be claimed by simply calling the API.
 */
@Entity
@Table(name = "ad_sessions")
class AdSession(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    var user: User? = null,

    /** Which ad provider served the ad, e.g. "google" or "simulated". */
    var provider: String = "",

    @Column(unique = true, nullable = false)
    var ticket: String = "",

    /** When the reward was granted; null while the ad is still open. */
    var completedAt: Instant? = null,
) : BaseEntity()

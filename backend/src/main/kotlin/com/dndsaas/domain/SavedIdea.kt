package com.dndsaas.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * A campaign or one-shot idea the AI generated for a user.
 *
 * Ideas cost tokens, so they are kept on the account and can be reused from
 * the "Your idea" box at any time instead of generating a new one.
 */
@Entity
@Table(name = "saved_ideas")
class SavedIdea(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    var user: User? = null,

    /** Whether the idea was generated for a campaign or a one-shot. */
    @Enumerated(EnumType.STRING)
    var kind: CampaignKind = CampaignKind.ONE_SHOT,

    @Column(columnDefinition = "TEXT")
    var idea: String = "",

    /** The tone and themes that go with the idea (null for ideas saved before these existed). */
    var tone: String? = null,

    @Column(columnDefinition = "TEXT")
    var themes: String? = null,

    /** Suggested length: hours for a one-shot, sessions for a campaign. */
    @Column(name = "suggested_length")
    var length: Int? = null,
) : BaseEntity()

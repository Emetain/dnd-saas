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
 * An organisation in the world — a guild, cult, noble house, army or cabal.
 * Factions drive the political situation the AI uses as context.
 */
@Entity
@Table(name = "factions")
class Faction(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,

    var name: String = "",

    /** e.g. "Thieves' Guild", "Noble House", "Religious Order". */
    var type: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    /** What the faction is trying to achieve. */
    @Column(columnDefinition = "TEXT")
    var goals: String = "",

    /** What the faction believes in / how it behaves. */
    @Column(columnDefinition = "TEXT")
    var ideology: String = "",

    @Enumerated(EnumType.STRING)
    var status: FactionStatus = FactionStatus.ACTIVE,

    /** Where the faction is based. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "headquarters_id")
    var headquarters: Location? = null,

    /** How the party is regarded by this faction, -100 (hated) to 100 (revered). */
    var reputationWithParty: Int = 0,

    @Column(columnDefinition = "TEXT")
    var notes: String = "",
) : BaseEntity()


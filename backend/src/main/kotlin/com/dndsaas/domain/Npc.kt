package com.dndsaas.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * A non-player character. The richest node in Campaign Memory: an NPC knows
 * where it lives, which faction it belongs to, which sessions it appeared in,
 * and (via [Relationship]) how it relates to other NPCs.
 */
@Entity
@Table(name = "npcs")
class Npc(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,

    var name: String = "",

    var race: String = "",

    /** e.g. "Innkeeper", "Captain of the Guard". */
    var occupation: String = "",

    /** Narrative role, e.g. "Quest giver", "Villain", "Comic relief". */
    var role: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    @Column(columnDefinition = "TEXT")
    var appearance: String = "",

    @Column(columnDefinition = "TEXT")
    var personality: String = "",

    /** What drives them — the key to improvising them consistently. */
    @Column(columnDefinition = "TEXT")
    var motivation: String = "",

    /** Hidden information the players do not know yet. DM eyes only. */
    @Column(columnDefinition = "TEXT")
    var secret: String = "",

    /** How they speak — accent, catchphrases, verbal tics. */
    @Column(columnDefinition = "TEXT")
    var voice: String = "",

    @Enumerated(EnumType.STRING)
    var status: NpcStatus = NpcStatus.ALIVE,

    /** How this NPC feels about the party. */
    @Enumerated(EnumType.STRING)
    var disposition: Disposition = Disposition.NEUTRAL,

    /** Where this NPC can usually be found. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    var location: Location? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faction_id")
    var faction: Faction? = null,

    /** Sessions this NPC appeared in — the NPC's own history. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "npc_sessions",
        joinColumns = [JoinColumn(name = "npc_id")],
        inverseJoinColumns = [JoinColumn(name = "session_id")],
    )
    var appearances: MutableSet<Session> = mutableSetOf(),

    @Column(columnDefinition = "TEXT")
    var notes: String = "",

    /** True when this NPC was produced by an AI generator. */
    var aiGenerated: Boolean = false,
) : BaseEntity()


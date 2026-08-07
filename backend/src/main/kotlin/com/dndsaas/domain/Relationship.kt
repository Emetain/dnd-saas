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
 * A directed link between two NPCs, e.g. "Ismark is the BROTHER of Ireena".
 *
 * Modelled as its own entity rather than a plain many-to-many so the link can
 * carry a type, a description and a strength — the social web the AI needs to
 * keep NPC interactions consistent.
 */
@Entity
@Table(name = "relationships")
class Relationship(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,

    /** The NPC the relationship starts from. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_npc_id")
    var fromNpc: Npc? = null,

    /** The NPC the relationship points to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_npc_id")
    var toNpc: Npc? = null,

    @Enumerated(EnumType.STRING)
    var type: RelationshipType = RelationshipType.ACQUAINTANCE,

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    /** -100 (bitter hatred) to 100 (utter devotion). */
    var strength: Int = 0,

    /** Whether the players are aware of this connection. */
    var knownToPlayers: Boolean = false,
) : BaseEntity()


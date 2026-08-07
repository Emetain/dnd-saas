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
 * An object of note in the campaign — a magic weapon, a quest McGuffin or a
 * piece of loot. Items are tracked so the AI never invents a duplicate of a
 * legendary artefact the party already owns.
 */
@Entity
@Table(name = "items")
class Item(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,

    var name: String = "",

    /** e.g. "Weapon", "Wondrous item", "Potion". */
    var type: String = "",

    @Enumerated(EnumType.STRING)
    var rarity: ItemRarity = ItemRarity.COMMON,

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    /** Mechanical effects, e.g. "+1 to attack and damage rolls". */
    @Column(columnDefinition = "TEXT")
    var properties: String = "",

    /** The story behind the item — useful AI context for lore consistency. */
    @Column(columnDefinition = "TEXT")
    var history: String = "",

    var requiresAttunement: Boolean = false,

    /** Player character currently carrying the item, if any. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_character_id")
    var ownerCharacter: Character? = null,

    /** NPC currently holding the item, if any. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_npc_id")
    var ownerNpc: Npc? = null,

    /** Where the item currently is, if it is not carried by anyone. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    var location: Location? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String = "",

    var aiGenerated: Boolean = false,
) : BaseEntity()


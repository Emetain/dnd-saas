package com.dndsaas.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * A player character (PC) taking part in a campaign.
 *
 * The ability scores are the raw inputs the service layer uses for calculations.
 * Every character belongs to a campaign, so the roster of "who is playing" is
 * part of Campaign Memory and feeds AI context automatically.
 */
@Entity
@Table(name = "characters")
class Character(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,

    var name: String = "",

    /** The real-world player behind this character. */
    var playerName: String = "",

    var characterClass: String = "",

    /** e.g. "Human", "Elf", "Dwarf". */
    var race: String = "",

    /** e.g. "Soldier", "Sage", "Criminal". */
    var background: String = "",

    var level: Int = 1,

    // Ability scores (D&D 5e style)
    var strength: Int = 10,
    var dexterity: Int = 10,
    var constitution: Int = 10,
    var intelligence: Int = 10,
    var wisdom: Int = 10,
    var charisma: Int = 10,

    // Combat stats
    var maxHitPoints: Int = 0,
    var currentHitPoints: Int = 0,
    var armorClass: Int = 10,

    /** Narrative history — valuable AI context for hooks and NPC ties. */
    @Column(columnDefinition = "TEXT")
    var backstory: String = "",

    /** Whether the character is still active in the campaign (vs. dead/retired). */
    var active: Boolean = true,
) : BaseEntity()

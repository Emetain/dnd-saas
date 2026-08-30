package com.dndsaas.domain

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/** How hard a fight is for the party, per the D&D 5e encounter building rules. */
enum class EncounterDifficulty {
    TRIVIAL,
    EASY,
    MEDIUM,
    HARD,
    DEADLY,
}

/** What a creature is there to do — the key to fights that feel designed. */
enum class CreatureRole {
    /** High damage, low finesse. Soaks attention. */
    BRUTE,

    /** Mobile, hit-and-run. */
    SKIRMISHER,

    /** Debuffs, terrain, battlefield manipulation. */
    CONTROLLER,

    /** Ranged damage from safety. */
    ARTILLERY,

    /** Buffs and commands the others; killing it changes the fight. */
    LEADER,

    /** Numerous and individually weak. */
    MINION,

    /** The centrepiece of the encounter. */
    SOLO,
}

/**
 * One kind of creature within an [Encounter].
 *
 * Stored as structured rows rather than prose so the encounter can be costed
 * against the party, and so Phase 8 can place these creatures on a battle map.
 */
@Embeddable
class EncounterCreature(
    var name: String = "",

    var count: Int = 1,

    /** Challenge Rating as written, e.g. "1/4", "2", "11". */
    var challengeRating: String = "0",

    @Enumerated(EnumType.STRING)
    var role: CreatureRole = CreatureRole.BRUTE,

    /** How this creature actually behaves in the fight. */
    @Column(columnDefinition = "TEXT")
    var tactics: String = "",
)

/**
 * A prepared combat, social or exploration encounter.
 *
 * Encounters are part of Campaign Memory: they reference a location, can be
 * attached to a session, and their creature list is real data.
 */
@Entity
@Table(name = "encounters")
class Encounter(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,

    var title: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    /** What the party is trying to achieve — not every fight is "kill them all". */
    @Column(columnDefinition = "TEXT")
    var objective: String = "",

    /** Terrain, cover, hazards and anything else that shapes the battlefield. */
    @Column(columnDefinition = "TEXT")
    var terrain: String = "",

    /** Read-aloud text for when the encounter begins. */
    @Column(columnDefinition = "TEXT")
    var setup: String = "",

    /** How the encounter can end without a fight, or how enemies flee. */
    @Column(columnDefinition = "TEXT")
    var resolution: String = "",

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "encounter_creatures", joinColumns = [JoinColumn(name = "encounter_id")])
    var creatures: MutableList<EncounterCreature> = mutableListOf(),

    /** What the model was asked to aim for. */
    @Enumerated(EnumType.STRING)
    var intendedDifficulty: EncounterDifficulty = EncounterDifficulty.MEDIUM,

    /** What the rules actually say, calculated against the real party. */
    @Enumerated(EnumType.STRING)
    var calculatedDifficulty: EncounterDifficulty = EncounterDifficulty.MEDIUM,

    /** Total adjusted XP of the creatures, used for the difficulty calculation. */
    var adjustedExperience: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    var location: Location? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    var session: Session? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String = "",

    var aiGenerated: Boolean = false,
) : BaseEntity()


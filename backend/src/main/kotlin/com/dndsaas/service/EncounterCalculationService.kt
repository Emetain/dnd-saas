package com.dndsaas.service

import com.dndsaas.domain.Character
import com.dndsaas.domain.EncounterCreature
import com.dndsaas.domain.EncounterDifficulty
import org.springframework.stereotype.Service

/**
 * Service layer — where everything is calculated.
 *
 * Implements the D&D 5e encounter building rules, so the platform can tell a
 * Dungeon Master what a fight is *actually* worth against their real party.
 * Language models are notoriously bad at this arithmetic; doing it in code is
 * one of the clearest advantages this product has over a chat window.
 */
@Service
class EncounterCalculationService {

    /** XP awarded per Challenge Rating (Dungeon Master's Guide). */
    private val experienceByChallengeRating: Map<String, Int> = mapOf(
        "0" to 10, "1/8" to 25, "1/4" to 50, "1/2" to 100,
        "1" to 200, "2" to 450, "3" to 700, "4" to 1100, "5" to 1800,
        "6" to 2300, "7" to 2900, "8" to 3900, "9" to 5000, "10" to 5900,
        "11" to 7200, "12" to 8400, "13" to 10000, "14" to 11500, "15" to 13000,
        "16" to 15000, "17" to 18000, "18" to 20000, "19" to 22000, "20" to 25000,
        "21" to 33000, "22" to 41000, "23" to 50000, "24" to 62000, "25" to 75000,
        "26" to 90000, "27" to 105000, "28" to 120000, "29" to 135000, "30" to 155000,
    )

    /**
     * XP thresholds per character level: easy, medium, hard, deadly.
     * Index 0 is unused so the list can be addressed by level directly.
     */
    private val thresholdsByLevel: List<IntArray> = listOf(
        intArrayOf(0, 0, 0, 0),
        intArrayOf(25, 50, 75, 100),
        intArrayOf(50, 100, 150, 200),
        intArrayOf(75, 150, 225, 400),
        intArrayOf(125, 250, 375, 500),
        intArrayOf(250, 500, 750, 1100),
        intArrayOf(300, 600, 900, 1400),
        intArrayOf(350, 750, 1100, 1700),
        intArrayOf(450, 900, 1400, 2100),
        intArrayOf(550, 1100, 1600, 2400),
        intArrayOf(600, 1200, 1900, 2800),
        intArrayOf(800, 1600, 2400, 3600),
        intArrayOf(1000, 2000, 3000, 4500),
        intArrayOf(1100, 2200, 3400, 5100),
        intArrayOf(1250, 2500, 3800, 5700),
        intArrayOf(1400, 2800, 4300, 6400),
        intArrayOf(1600, 3200, 4800, 7200),
        intArrayOf(2000, 3900, 5900, 8800),
        intArrayOf(2100, 4200, 6300, 9500),
        intArrayOf(2400, 4900, 7300, 10900),
        intArrayOf(2800, 5700, 8500, 12700),
    )

    /** Raw XP of the creatures, before the multiple-enemy multiplier. */
    fun rawExperience(creatures: List<EncounterCreature>): Int =
        creatures.sumOf { experienceFor(it.challengeRating) * it.count.coerceAtLeast(1) }

    /** Total number of individual creatures. */
    fun creatureCount(creatures: List<EncounterCreature>): Int =
        creatures.sumOf { it.count.coerceAtLeast(1) }

    /**
     * Adjusted XP: more enemies means a harder fight than their XP suggests,
     * because they get more actions.
     */
    fun adjustedExperience(creatures: List<EncounterCreature>, partySize: Int): Int {
        val raw = rawExperience(creatures)
        if (raw == 0) return 0
        val multiplier = encounterMultiplier(creatureCount(creatures), partySize)
        return (raw * multiplier).toInt()
    }

    /** Compares the adjusted XP against the party's thresholds. */
    fun difficulty(creatures: List<EncounterCreature>, party: List<Character>): EncounterDifficulty {
        val active = party.filter { it.active }
        if (active.isEmpty()) return EncounterDifficulty.MEDIUM
        val adjusted = adjustedExperience(creatures, active.size)
        val (easy, medium, hard, deadly) = partyThresholds(active)
        return when {
            adjusted >= deadly -> EncounterDifficulty.DEADLY
            adjusted >= hard -> EncounterDifficulty.HARD
            adjusted >= medium -> EncounterDifficulty.MEDIUM
            adjusted >= easy -> EncounterDifficulty.EASY
            else -> EncounterDifficulty.TRIVIAL
        }
    }

    /** The party's combined easy/medium/hard/deadly XP budgets. */
    fun partyThresholds(party: List<Character>): Thresholds {
        var easy = 0
        var medium = 0
        var hard = 0
        var deadly = 0
        party.forEach { character ->
            val row = thresholdsByLevel[character.level.coerceIn(1, 20)]
            easy += row[0]
            medium += row[1]
            hard += row[2]
            deadly += row[3]
        }
        return Thresholds(easy, medium, hard, deadly)
    }

    /** The XP budget a generator should aim for to hit a target difficulty. */
    fun budgetFor(party: List<Character>, target: EncounterDifficulty): Int {
        val active = party.filter { it.active }
        if (active.isEmpty()) return 0
        val thresholds = partyThresholds(active)
        return when (target) {
            EncounterDifficulty.TRIVIAL -> thresholds.easy / 2
            EncounterDifficulty.EASY -> thresholds.easy
            EncounterDifficulty.MEDIUM -> thresholds.medium
            EncounterDifficulty.HARD -> thresholds.hard
            EncounterDifficulty.DEADLY -> thresholds.deadly
        }
    }

    /** XP for a Challenge Rating, tolerating "CR 1/2", "1.5" and similar. */
    fun experienceFor(challengeRating: String): Int {
        val cleaned = challengeRating.trim()
            .removePrefix("CR").removePrefix("cr")
            .trim()
            .lowercase()
        experienceByChallengeRating[cleaned]?.let { return it }
        // Tolerate decimals the model may produce instead of fractions.
        return when (cleaned.toDoubleOrNull()) {
            null -> 0
            0.125 -> 25
            0.25 -> 50
            0.5 -> 100
            else -> experienceByChallengeRating[cleaned.toDouble().toInt().toString()] ?: 0
        }
    }

    /**
     * The encounter multiplier from the Dungeon Master's Guide, shifted for
     * unusually small or large parties.
     */
    private fun encounterMultiplier(creatureCount: Int, partySize: Int): Double {
        val tier = when {
            creatureCount <= 1 -> 0
            creatureCount == 2 -> 1
            creatureCount <= 6 -> 2
            creatureCount <= 10 -> 3
            creatureCount <= 14 -> 4
            else -> 5
        }
        // Small parties feel outnumbered sooner; large parties later.
        val shifted = when {
            partySize < 3 -> tier + 1
            partySize > 5 -> tier - 1
            else -> tier
        }.coerceIn(0, 6)
        return listOf(1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0)[shifted]
    }

    /** The four XP budgets for a party. */
    data class Thresholds(
        val easy: Int,
        val medium: Int,
        val hard: Int,
        val deadly: Int,
    )
}


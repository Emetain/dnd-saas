package com.dndsaas.dto

import com.dndsaas.domain.CreatureRole
import com.dndsaas.domain.EncounterDifficulty
import java.time.Instant

/** One kind of creature in an encounter. */
data class EncounterCreatureDto(
    val name: String,
    val count: Int = 1,
    val challengeRating: String = "0",
    val role: CreatureRole = CreatureRole.BRUTE,
    val tactics: String = "",
)

/** Request to create or update an encounter by hand. */
data class EncounterRequest(
    val title: String,
    val description: String = "",
    val objective: String = "",
    val terrain: String = "",
    val setup: String = "",
    val resolution: String = "",
    val creatures: List<EncounterCreatureDto> = emptyList(),
    val intendedDifficulty: EncounterDifficulty = EncounterDifficulty.MEDIUM,
    val locationId: Long? = null,
    val sessionId: Long? = null,
    val notes: String = "",
)

data class EncounterResponse(
    val id: Long,
    val campaignId: Long,
    val title: String,
    val description: String,
    val objective: String,
    val terrain: String,
    val setup: String,
    val resolution: String,
    val creatures: List<EncounterCreatureDto>,
    val intendedDifficulty: EncounterDifficulty,
    /** What the 5e rules say about this fight against the real party. */
    val calculatedDifficulty: EncounterDifficulty,
    val totalCreatures: Int,
    val adjustedExperience: Int,
    /** The party's easy/medium/hard/deadly XP budgets, for context. */
    val partyThresholds: PartyThresholds,
    val location: EntityRef?,
    val session: EntityRef?,
    val notes: String,
    val aiGenerated: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** The party's XP budgets, calculated from their real levels. */
data class PartyThresholds(
    val partySize: Int,
    val easy: Int,
    val medium: Int,
    val hard: Int,
    val deadly: Int,
)


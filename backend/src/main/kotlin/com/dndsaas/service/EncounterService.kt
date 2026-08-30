package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.Encounter
import com.dndsaas.domain.EncounterCreature
import com.dndsaas.dto.EncounterCreatureDto
import com.dndsaas.dto.EncounterRequest
import com.dndsaas.dto.EncounterResponse
import com.dndsaas.dto.EntityRef
import com.dndsaas.dto.PartyThresholds
import com.dndsaas.repository.CharacterRepository
import com.dndsaas.repository.EncounterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Business logic for encounters.
 *
 * Persistence and mapping live here; the 5e maths is delegated to
 * [EncounterCalculationService], which recalculates difficulty on every read so
 * it always reflects the party's *current* levels.
 */
@Service
@Transactional
class EncounterService(
    private val encounterRepository: EncounterRepository,
    private val characterRepository: CharacterRepository,
    private val calculationService: EncounterCalculationService,
    private val locationService: LocationService,
    private val sessionService: SessionService,
) {

    fun create(campaign: Campaign, request: EncounterRequest, aiGenerated: Boolean = false): EncounterResponse {
        val encounter = Encounter(campaign = campaign)
        encounter.aiGenerated = aiGenerated
        apply(encounter, request)
        return toResponse(encounterRepository.save(encounter))
    }

    fun update(id: Long, request: EncounterRequest): EncounterResponse {
        val encounter = findEntity(id)
        apply(encounter, request)
        return toResponse(encounterRepository.save(encounter))
    }

    @Transactional(readOnly = true)
    fun get(id: Long): EncounterResponse = toResponse(findEntity(id))

    @Transactional(readOnly = true)
    fun listByCampaign(campaignId: Long): List<EncounterResponse> =
        encounterRepository.findByCampaignIdOrderByTitleAsc(campaignId).map(::toResponse)

    @Transactional(readOnly = true)
    fun listBySession(sessionId: Long): List<EncounterResponse> =
        encounterRepository.findBySessionIdOrderByTitleAsc(sessionId).map(::toResponse)

    fun delete(id: Long) = encounterRepository.deleteById(id)

    fun findEntity(id: Long): Encounter =
        encounterRepository.findById(id)
            .orElseThrow { NoSuchElementException("Encounter $id not found") }

    fun countForCampaign(campaignId: Long): Long = encounterRepository.countByCampaignId(campaignId)

    fun deleteAllForCampaign(campaignId: Long) =
        encounterRepository.deleteAll(encounterRepository.findByCampaignIdOrderByTitleAsc(campaignId))

    private fun apply(encounter: Encounter, request: EncounterRequest) {
        encounter.title = request.title
        encounter.description = request.description
        encounter.objective = request.objective
        encounter.terrain = request.terrain
        encounter.setup = request.setup
        encounter.resolution = request.resolution
        encounter.intendedDifficulty = request.intendedDifficulty
        encounter.location = locationService.findEntityOrNull(request.locationId)
        encounter.session = request.sessionId?.let { sessionService.findEntity(it) }
        encounter.notes = request.notes

        encounter.creatures.clear()
        request.creatures.filter { it.name.isNotBlank() }.forEach { dto ->
            encounter.creatures.add(
                EncounterCreature(
                    name = dto.name,
                    count = dto.count.coerceAtLeast(1),
                    challengeRating = dto.challengeRating,
                    role = dto.role,
                    tactics = dto.tactics,
                ),
            )
        }

        // Cost the encounter against the party that will actually face it.
        val party = characterRepository.findByCampaignIdOrderByNameAsc(encounter.campaign!!.id!!)
        encounter.adjustedExperience = calculationService.adjustedExperience(
            encounter.creatures,
            party.count { it.active }.coerceAtLeast(1),
        )
        encounter.calculatedDifficulty = calculationService.difficulty(encounter.creatures, party)
    }

    fun toResponse(encounter: Encounter): EncounterResponse {
        val campaignId = encounter.campaign!!.id!!
        val party = characterRepository.findByCampaignIdOrderByNameAsc(campaignId).filter { it.active }
        val thresholds = calculationService.partyThresholds(party)
        return EncounterResponse(
            id = encounter.id!!,
            campaignId = campaignId,
            title = encounter.title,
            description = encounter.description,
            objective = encounter.objective,
            terrain = encounter.terrain,
            setup = encounter.setup,
            resolution = encounter.resolution,
            creatures = encounter.creatures.map {
                EncounterCreatureDto(it.name, it.count, it.challengeRating, it.role, it.tactics)
            },
            intendedDifficulty = encounter.intendedDifficulty,
            // Recalculated on read, so levelling the party re-rates old encounters.
            calculatedDifficulty = calculationService.difficulty(encounter.creatures, party),
            totalCreatures = calculationService.creatureCount(encounter.creatures),
            adjustedExperience = calculationService.adjustedExperience(
                encounter.creatures,
                party.size.coerceAtLeast(1),
            ),
            partyThresholds = PartyThresholds(
                partySize = party.size,
                easy = thresholds.easy,
                medium = thresholds.medium,
                hard = thresholds.hard,
                deadly = thresholds.deadly,
            ),
            location = encounter.location?.let { EntityRef(it.id!!, it.name) },
            session = encounter.session?.let { EntityRef(it.id!!, "Session ${it.sessionNumber}: ${it.title}") },
            notes = encounter.notes,
            aiGenerated = encounter.aiGenerated,
            createdAt = encounter.createdAt,
            updatedAt = encounter.updatedAt,
        )
    }
}


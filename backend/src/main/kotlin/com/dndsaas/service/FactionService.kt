package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.Faction
import com.dndsaas.dto.EntityRef
import com.dndsaas.dto.FactionRequest
import com.dndsaas.dto.FactionResponse
import com.dndsaas.repository.FactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Business logic for factions — the organisations shaping the campaign's politics.
 */
@Service
@Transactional
class FactionService(
    private val factionRepository: FactionRepository,
    private val locationService: LocationService,
) {

    fun create(campaign: Campaign, request: FactionRequest): FactionResponse {
        val faction = Faction(campaign = campaign)
        apply(faction, request)
        return toResponse(factionRepository.save(faction))
    }

    fun update(id: Long, request: FactionRequest): FactionResponse {
        val faction = findEntity(id)
        apply(faction, request)
        return toResponse(factionRepository.save(faction))
    }

    @Transactional(readOnly = true)
    fun get(id: Long): FactionResponse = toResponse(findEntity(id))

    @Transactional(readOnly = true)
    fun listByCampaign(campaignId: Long): List<FactionResponse> =
        factionRepository.findByCampaignIdOrderByNameAsc(campaignId).map(::toResponse)

    fun delete(id: Long) = factionRepository.deleteById(id)

    fun findEntity(id: Long): Faction =
        factionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Faction $id not found") }

    fun findEntityOrNull(id: Long?): Faction? = id?.let { findEntity(it) }

    fun countForCampaign(campaignId: Long): Long = factionRepository.countByCampaignId(campaignId)

    fun deleteAllForCampaign(campaignId: Long) =
        factionRepository.deleteAll(factionRepository.findByCampaignIdOrderByNameAsc(campaignId))

    private fun apply(faction: Faction, request: FactionRequest) {
        faction.name = request.name
        faction.type = request.type
        faction.description = request.description
        faction.goals = request.goals
        faction.ideology = request.ideology
        faction.status = request.status
        faction.headquarters = locationService.findEntityOrNull(request.headquartersId)
        faction.reputationWithParty = request.reputationWithParty.coerceIn(-100, 100)
        faction.notes = request.notes
    }

    fun toResponse(faction: Faction): FactionResponse =
        FactionResponse(
            id = faction.id!!,
            campaignId = faction.campaign!!.id!!,
            name = faction.name,
            type = faction.type,
            description = faction.description,
            goals = faction.goals,
            ideology = faction.ideology,
            status = faction.status,
            headquarters = faction.headquarters?.let { EntityRef(it.id!!, it.name) },
            reputationWithParty = faction.reputationWithParty,
            notes = faction.notes,
            createdAt = faction.createdAt,
            updatedAt = faction.updatedAt,
        )
}


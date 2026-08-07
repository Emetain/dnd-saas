package com.dndsaas.orchestration

import com.dndsaas.dto.FactionRequest
import com.dndsaas.dto.FactionResponse
import com.dndsaas.dto.NpcResponse
import com.dndsaas.service.CampaignService
import com.dndsaas.service.FactionService
import com.dndsaas.service.NpcService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for factions. Coordinates the faction service and,
 * for the membership view, the NPC service.
 */
@Component
class FactionOrchestrator(
    private val factionService: FactionService,
    private val npcService: NpcService,
    private val campaignService: CampaignService,
) {

    fun addFaction(campaignId: Long, request: FactionRequest): FactionResponse {
        val campaign = campaignService.findEntity(campaignId)
        return factionService.create(campaign, request)
    }

    fun listFactions(campaignId: Long): List<FactionResponse> {
        campaignService.findEntity(campaignId)
        return factionService.listByCampaign(campaignId)
    }

    fun getFaction(id: Long): FactionResponse = factionService.get(id)

    /** Task: who belongs to this faction — needs two services, hence the orchestrator. */
    fun listFactionMembers(id: Long): List<NpcResponse> {
        factionService.findEntity(id)
        return npcService.listByFaction(id)
    }

    fun updateFaction(id: Long, request: FactionRequest): FactionResponse =
        factionService.update(id, request)

    fun deleteFaction(id: Long) = factionService.delete(id)
}


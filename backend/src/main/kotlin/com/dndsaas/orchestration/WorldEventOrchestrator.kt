package com.dndsaas.orchestration

import com.dndsaas.dto.WorldEventRequest
import com.dndsaas.dto.WorldEventResponse
import com.dndsaas.service.CampaignService
import com.dndsaas.service.WorldEventService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for world events (the campaign timeline).
 */
@Component
class WorldEventOrchestrator(
    private val worldEventService: WorldEventService,
    private val campaignService: CampaignService,
) {

    fun addWorldEvent(campaignId: Long, request: WorldEventRequest): WorldEventResponse {
        val campaign = campaignService.findEntity(campaignId)
        return worldEventService.create(campaign, request)
    }

    fun listWorldEvents(campaignId: Long): List<WorldEventResponse> {
        campaignService.findEntity(campaignId)
        return worldEventService.listByCampaign(campaignId)
    }

    fun getWorldEvent(id: Long): WorldEventResponse = worldEventService.get(id)

    fun updateWorldEvent(id: Long, request: WorldEventRequest): WorldEventResponse =
        worldEventService.update(id, request)

    fun deleteWorldEvent(id: Long) = worldEventService.delete(id)
}


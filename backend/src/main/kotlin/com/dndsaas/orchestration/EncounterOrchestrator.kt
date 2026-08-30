package com.dndsaas.orchestration

import com.dndsaas.dto.EncounterRequest
import com.dndsaas.dto.EncounterResponse
import com.dndsaas.service.CampaignService
import com.dndsaas.service.EncounterService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for encounters prepared by hand (as opposed to
 * generated via [com.dndsaas.controller.GeneratorController]).
 */
@Component
class EncounterOrchestrator(
    private val encounterService: EncounterService,
    private val campaignService: CampaignService,
) {

    fun addEncounter(campaignId: Long, request: EncounterRequest): EncounterResponse {
        val campaign = campaignService.findEntity(campaignId)
        return encounterService.create(campaign, request)
    }

    fun listEncounters(campaignId: Long): List<EncounterResponse> {
        campaignService.findEntity(campaignId)
        return encounterService.listByCampaign(campaignId)
    }

    fun listEncountersForSession(sessionId: Long): List<EncounterResponse> =
        encounterService.listBySession(sessionId)

    fun getEncounter(id: Long): EncounterResponse = encounterService.get(id)

    fun updateEncounter(id: Long, request: EncounterRequest): EncounterResponse =
        encounterService.update(id, request)

    fun deleteEncounter(id: Long) = encounterService.delete(id)
}


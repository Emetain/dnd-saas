package com.dndsaas.orchestration

import com.dndsaas.dto.CampaignRequest
import com.dndsaas.dto.CampaignResponse
import com.dndsaas.service.CampaignService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for campaigns. The controller hands tasks here;
 * this layer coordinates the service(s) needed to fulfil them. For simple CRUD
 * it delegates to a single service, but it is the extension point for
 * multi-service workflows (e.g. later: seed a new campaign with starter memory).
 */
@Component
class CampaignOrchestrator(
    private val campaignService: CampaignService,
) {
    fun createCampaign(request: CampaignRequest): CampaignResponse = campaignService.create(request)

    fun updateCampaign(id: Long, request: CampaignRequest): CampaignResponse =
        campaignService.update(id, request)

    fun getCampaign(id: Long): CampaignResponse = campaignService.get(id)

    fun listCampaigns(): List<CampaignResponse> = campaignService.list()

    fun deleteCampaign(id: Long) = campaignService.delete(id)
}


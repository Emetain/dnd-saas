package com.dndsaas.orchestration

import com.dndsaas.dto.CampaignRequest
import com.dndsaas.dto.CampaignResponse
import com.dndsaas.service.CampaignMemoryService
import com.dndsaas.service.CampaignService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Orchestration (middle) layer for campaigns. The controller hands tasks here;
 * this layer coordinates the service(s) needed to fulfil them.
 */
@Component
class CampaignOrchestrator(
    private val campaignService: CampaignService,
    private val campaignMemoryService: CampaignMemoryService,
) {
    fun createCampaign(request: CampaignRequest): CampaignResponse = campaignService.create(request)

    fun updateCampaign(id: Long, request: CampaignRequest): CampaignResponse =
        campaignService.update(id, request)

    fun getCampaign(id: Long): CampaignResponse = campaignService.get(id)

    fun listCampaigns(): List<CampaignResponse> = campaignService.list()

    /**
     * Task: delete a campaign and everything it remembers.
     *
     * A genuine multi-service workflow: all memory objects must be removed in a
     * foreign-key-safe order before the campaign row itself can go.
     */
    @Transactional
    fun deleteCampaign(id: Long) {
        campaignService.findEntity(id)
        campaignMemoryService.deleteAllForCampaign(id)
        campaignService.delete(id)
    }
}

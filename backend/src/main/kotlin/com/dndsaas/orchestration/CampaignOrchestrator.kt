package com.dndsaas.orchestration

import com.dndsaas.domain.CampaignKind
import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.dto.CampaignRequest
import com.dndsaas.dto.CampaignResponse
import com.dndsaas.service.CampaignMemoryService
import com.dndsaas.service.CampaignService
import com.dndsaas.service.TierRestrictionException
import com.dndsaas.service.UserService
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
    private val userService: UserService,
) {
    fun createCampaign(userId: Long, request: CampaignRequest): CampaignResponse {
        val owner = userService.findEntity(userId)
        if (owner.subscriptionTier == SubscriptionTier.FREE && request.kind == CampaignKind.CAMPAIGN) {
            throw TierRestrictionException("Full campaigns require Pro or higher — Free users can create one-shots")
        }
        return campaignService.create(request, owner)
    }

    fun updateCampaign(id: Long, request: CampaignRequest): CampaignResponse =
        campaignService.update(id, request)

    fun getCampaign(id: Long): CampaignResponse = campaignService.get(id)

    fun listCampaigns(userId: Long? = null): List<CampaignResponse> =
        if (userId == null) campaignService.list() else campaignService.listForUser(userId)

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

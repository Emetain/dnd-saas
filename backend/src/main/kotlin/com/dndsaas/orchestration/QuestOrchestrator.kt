package com.dndsaas.orchestration

import com.dndsaas.domain.QuestStatus
import com.dndsaas.dto.QuestRequest
import com.dndsaas.dto.QuestResponse
import com.dndsaas.service.CampaignService
import com.dndsaas.service.QuestService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for quests.
 */
@Component
class QuestOrchestrator(
    private val questService: QuestService,
    private val campaignService: CampaignService,
) {

    fun addQuest(campaignId: Long, request: QuestRequest): QuestResponse {
        val campaign = campaignService.findEntity(campaignId)
        return questService.create(campaign, request)
    }

    fun listQuests(campaignId: Long, status: QuestStatus?): List<QuestResponse> {
        campaignService.findEntity(campaignId)
        return if (status == null) {
            questService.listByCampaign(campaignId)
        } else {
            questService.listByCampaignAndStatus(campaignId, status)
        }
    }

    /** Task: what the party is currently caught up in. */
    fun listActiveQuests(campaignId: Long): List<QuestResponse> {
        campaignService.findEntity(campaignId)
        return questService.listActive(campaignId)
    }

    fun getQuest(id: Long): QuestResponse = questService.get(id)

    fun updateQuest(id: Long, request: QuestRequest): QuestResponse = questService.update(id, request)

    fun deleteQuest(id: Long) = questService.delete(id)
}


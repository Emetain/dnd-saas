package com.dndsaas.orchestration

import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.GenerationLogResponse
import com.dndsaas.dto.GenerationRequest
import com.dndsaas.dto.GenerationResponse
import com.dndsaas.dto.GenerationUsageStats
import com.dndsaas.dto.PromptPreview
import com.dndsaas.service.AiService
import com.dndsaas.service.CampaignService
import com.dndsaas.service.GenerationLogService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for AI generation.
 *
 * Resolves the campaign, then hands the task to the central AI service. From
 * Phase 4 this is where a request is routed to the right generator module, and
 * from Phase 5 where the user's token balance is checked before generating.
 */
@Component
class AiOrchestrator(
    private val aiService: AiService,
    private val campaignService: CampaignService,
    private val generationLogService: GenerationLogService,
) {

    /** Task: generate something for a campaign, grounded in its memory. */
    fun generate(campaignId: Long, request: GenerationRequest): GenerationResponse {
        val campaign = campaignService.findEntity(campaignId)
        return aiService.generate(campaign, request)
    }

    /** Task: show the prompt that would be sent, without calling the model. */
    fun previewPrompt(campaignId: Long, type: GenerationType, instruction: String): PromptPreview {
        campaignService.findEntity(campaignId)
        return aiService.preview(campaignId, type, instruction)
    }

    /** Task: the campaign's AI generation history. */
    fun listHistory(campaignId: Long): List<GenerationLogResponse> {
        campaignService.findEntity(campaignId)
        return generationLogService.listForCampaign(campaignId)
    }

    /** Task: how much AI usage this campaign has accumulated. */
    fun usageStats(campaignId: Long): GenerationUsageStats {
        campaignService.findEntity(campaignId)
        return generationLogService.usageStats(campaignId)
    }
}


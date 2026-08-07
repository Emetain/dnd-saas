package com.dndsaas.orchestration

import com.dndsaas.dto.CampaignGenerationRequest
import com.dndsaas.dto.CampaignGenerationResult
import com.dndsaas.dto.CampaignInterviewRequest
import com.dndsaas.dto.CampaignInterviewResponse
import com.dndsaas.service.CampaignGeneratorService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for campaign generation.
 *
 * Unlike the other orchestrators there is no campaign to resolve — this is the
 * flow that *creates* one. It coordinates the two-step experience: interview
 * the Dungeon Master, then build the campaign from their answers.
 */
@Component
class CampaignGeneratorOrchestrator(
    private val campaignGeneratorService: CampaignGeneratorService,
) {

    /** Task: ask the Dungeon Master the questions that will shape their campaign. */
    fun interview(request: CampaignInterviewRequest): CampaignInterviewResponse =
        campaignGeneratorService.interview(request)

    /**
     * Task: turn the idea and the interview answers into a real campaign,
     * written into Campaign Memory as structured data.
     */
    fun generate(request: CampaignGenerationRequest): CampaignGenerationResult =
        campaignGeneratorService.generate(request)
}


package com.dndsaas.orchestration

import com.dndsaas.dto.CampaignGenerationRequest
import com.dndsaas.dto.CampaignGenerationResult
import com.dndsaas.dto.CampaignInterviewRequest
import com.dndsaas.dto.CampaignInterviewResponse
import com.dndsaas.service.CampaignGeneratorService
import com.dndsaas.service.UserService
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
    private val userService: UserService,
) {

    /** Task: ask the Dungeon Master the questions that will shape their campaign. */
    fun interview(userId: Long, request: CampaignInterviewRequest): CampaignInterviewResponse =
        campaignGeneratorService.interview(request, userService.findEntity(userId))

    /**
     * Task: turn the idea and the interview answers into a real campaign,
     * written into Campaign Memory as structured data.
     */
    fun generate(userId: Long, request: CampaignGenerationRequest): CampaignGenerationResult =
        campaignGeneratorService.generate(request, userService.findEntity(userId))

    /** Task: build a complete, self-contained adventure for one sitting. */
    fun generateOneShot(userId: Long, request: CampaignGenerationRequest): CampaignGenerationResult =
        campaignGeneratorService.generateOneShot(request, userService.findEntity(userId))
}


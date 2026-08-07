package com.dndsaas.controller

import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.GenerationLogResponse
import com.dndsaas.dto.GenerationRequest
import com.dndsaas.dto.GenerationResponse
import com.dndsaas.dto.GenerationUsageStats
import com.dndsaas.dto.PromptPreview
import com.dndsaas.orchestration.AiOrchestrator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * REST entry point for AI generation. Every request is automatically grounded
 * in the campaign's memory — clients never send context themselves.
 */
@RestController
@RequestMapping("/v1/campaigns/{campaignId}/ai")
@Tag(name = "AI Generation", description = "Generate content grounded in Campaign Memory")
class AiGenerationController(
    private val orchestrator: AiOrchestrator,
) {

    @PostMapping("/generate")
    @Operation(summary = "Generate content using the campaign's memory as context")
    fun generate(
        @PathVariable campaignId: Long,
        @RequestBody request: GenerationRequest,
    ): GenerationResponse = orchestrator.generate(campaignId, request)

    @GetMapping("/preview")
    @Operation(summary = "See the exact prompt that would be sent, without calling the model (free)")
    fun preview(
        @PathVariable campaignId: Long,
        @RequestParam(defaultValue = "NPC") type: GenerationType,
        @RequestParam(defaultValue = "") instruction: String,
    ): PromptPreview = orchestrator.previewPrompt(campaignId, type, instruction)

    @GetMapping("/history")
    @Operation(summary = "List everything generated for this campaign")
    fun history(@PathVariable campaignId: Long): List<GenerationLogResponse> =
        orchestrator.listHistory(campaignId)

    @GetMapping("/usage")
    @Operation(summary = "Token usage totals for this campaign")
    fun usage(@PathVariable campaignId: Long): GenerationUsageStats =
        orchestrator.usageStats(campaignId)
}


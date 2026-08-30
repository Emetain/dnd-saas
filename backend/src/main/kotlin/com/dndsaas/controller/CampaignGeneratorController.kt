package com.dndsaas.controller

import com.dndsaas.dto.CampaignGenerationRequest
import com.dndsaas.dto.CampaignGenerationResult
import com.dndsaas.dto.CampaignInterviewRequest
import com.dndsaas.dto.CampaignInterviewResponse
import com.dndsaas.orchestration.CampaignGeneratorOrchestrator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST entry point for the Campaign Generator.
 *
 * The intended flow is two calls:
 *
 *   1. POST /interview — send a rough idea, get tailored questions back
 *   2. POST /generate  — send the idea plus the answers, get a full campaign
 *
 * Step 1 is optional: generating without answers still works.
 */
@RestController
@RequestMapping("/v1/ai/campaign-generator")
@Tag(name = "Campaign Generator", description = "Interview the DM, then generate a whole campaign")
class CampaignGeneratorController(
    private val orchestrator: CampaignGeneratorOrchestrator,
) {

    @PostMapping("/interview")
    @Operation(
        summary = "Step 1: ask the Dungeon Master questions about their idea",
        description = "Returns tailored questions with suggested answers. Nothing is saved yet.",
    )
    fun interview(@RequestBody request: CampaignInterviewRequest): CampaignInterviewResponse =
        orchestrator.interview(request)

    @PostMapping("/generate")
    @Operation(
        summary = "Step 2: generate a full campaign from the idea and the interview answers",
        description = "Creates the campaign, its world lore, locations, factions, NPCs, quests " +
            "and a ready-to-run first session — all saved as structured Campaign Memory.",
    )
    fun generate(@RequestBody request: CampaignGenerationRequest): ResponseEntity<CampaignGenerationResult> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.generate(request))

    @PostMapping("/one-shot")
    @Operation(
        summary = "Generate a complete one-shot adventure",
        description = "Creates a compact, connected adventure with a decisive ending, " +
            "saved as a campaign and structured Campaign Memory.",
    )
    fun generateOneShot(@RequestBody request: CampaignGenerationRequest): ResponseEntity<CampaignGenerationResult> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.generateOneShot(request))
}


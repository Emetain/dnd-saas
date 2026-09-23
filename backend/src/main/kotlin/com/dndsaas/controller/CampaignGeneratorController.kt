package com.dndsaas.controller

import com.dndsaas.dto.CampaignGenerationRequest
import com.dndsaas.dto.CampaignGenerationResult
import com.dndsaas.domain.CampaignKind
import com.dndsaas.dto.SavedIdeaResponse
import com.dndsaas.dto.CampaignInterviewRequest
import com.dndsaas.dto.CampaignInterviewResponse
import com.dndsaas.orchestration.CampaignGeneratorOrchestrator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
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

    @PostMapping("/idea")
    @Operation(
        summary = "Generate an idea to start from",
        description = "Returns a one- or two-sentence campaign or one-shot idea shaped by the chosen preferences, " +
            "and saves it to the account for reuse. Costs 5 tokens.",
    )
    fun suggestIdea(
        @RequestHeader(USER_ID_HEADER) userId: Long,
        @RequestBody request: CampaignInterviewRequest,
    ): ResponseEntity<SavedIdeaResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.suggestIdea(userId, request))

    @GetMapping("/ideas")
    @Operation(summary = "List the caller's saved ideas, newest first (optionally ?kind=ONE_SHOT or CAMPAIGN)")
    fun savedIdeas(
        @RequestHeader(USER_ID_HEADER) userId: Long,
        @RequestParam(required = false) kind: CampaignKind?,
    ): List<SavedIdeaResponse> =
        orchestrator.savedIdeas(userId, kind)

    @DeleteMapping("/ideas/{ideaId}")
    @Operation(summary = "Delete one of the caller's saved ideas")
    fun deleteIdea(
        @RequestHeader(USER_ID_HEADER) userId: Long,
        @PathVariable ideaId: Long,
    ): ResponseEntity<Void> {
        orchestrator.deleteIdea(userId, ideaId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/interview")
    @Operation(
        summary = "Step 1: ask the Dungeon Master questions about their idea",
        description = "Returns tailored questions with suggested answers. Nothing is saved yet.",
    )
    fun interview(
        @RequestHeader(USER_ID_HEADER) userId: Long,
        @RequestBody request: CampaignInterviewRequest,
    ): CampaignInterviewResponse =
        orchestrator.interview(userId, request)

    @PostMapping("/generate")
    @Operation(
        summary = "Step 2: generate a full campaign from the idea and the interview answers",
        description = "Creates the campaign, its world lore, locations, factions, NPCs, quests " +
            "and a ready-to-run first session — all saved as structured Campaign Memory.",
    )
    fun generate(
        @RequestHeader(USER_ID_HEADER) userId: Long,
        @RequestBody request: CampaignGenerationRequest,
    ): ResponseEntity<CampaignGenerationResult> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.generate(userId, request))

    @PostMapping("/one-shot")
    @Operation(
        summary = "Generate a complete one-shot adventure",
        description = "Creates a compact, connected adventure with a decisive ending, " +
            "saved as a campaign and structured Campaign Memory.",
    )
    fun generateOneShot(
        @RequestHeader(USER_ID_HEADER) userId: Long,
        @RequestBody request: CampaignGenerationRequest,
    ): ResponseEntity<CampaignGenerationResult> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.generateOneShot(userId, request))
}


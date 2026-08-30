package com.dndsaas.controller

import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.GeneratorInfo
import com.dndsaas.dto.TypedGenerationRequest
import com.dndsaas.dto.TypedGenerationResponse
import com.dndsaas.orchestration.GeneratorOrchestrator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST entry point for every generator module (NPC, Quest, Encounter, Boss,
 * Loot, Shop, Puzzle, Random Encounter, Character Backstory).
 *
 * All of them share one shape:
 *   POST /v1/campaigns/{id}/generate/{type}                — generate (and usually save)
 *   POST /v1/campaigns/{id}/generate/{type}/commit/{logId} — save a previewed result
 *
 * Send `"preview": true` to generate without saving; the result can still be
 * committed later using the returned `logId`, without spending tokens again.
 */
@RestController
@RequestMapping("/v1/campaigns/{campaignId}/generate")
@Tag(name = "Generators", description = "NPC, Quest, Encounter, Boss, Loot, Shop, Puzzle and more")
class GeneratorController(
    private val orchestrator: GeneratorOrchestrator,
) {

    @GetMapping
    @Operation(summary = "List the generator modules available, with their token cost")
    fun catalogue(): List<GeneratorInfo> = orchestrator.catalogue()

    @PostMapping("/{type}")
    @Operation(
        summary = "Run a generator",
        description = "Generates content grounded in Campaign Memory. Saved immediately unless " +
            "\"preview\": true is sent, in which case it can be committed later.",
    )
    fun generate(
        @PathVariable campaignId: Long,
        @PathVariable type: GenerationType,
        @RequestBody request: TypedGenerationRequest,
    ): TypedGenerationResponse = orchestrator.generate(campaignId, type, request)

    @PostMapping("/{type}/commit/{logId}")
    @Operation(
        summary = "Save a previewed generation",
        description = "Re-uses the logged result from a previous preview call — no model call, no extra tokens.",
    )
    fun commit(
        @PathVariable campaignId: Long,
        @PathVariable type: GenerationType,
        @PathVariable logId: Long,
        @RequestBody request: TypedGenerationRequest,
    ): TypedGenerationResponse = orchestrator.commit(campaignId, logId, request)
}


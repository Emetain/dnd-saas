package com.dndsaas.controller

import com.dndsaas.dto.CampaignMemorySnapshot
import com.dndsaas.dto.CampaignMemoryStats
import com.dndsaas.orchestration.CampaignMemoryOrchestrator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST entry point for Campaign Memory as a whole.
 *
 * The snapshot endpoint returns everything the application remembers about a
 * campaign in one structured payload — the same data the AI service will use
 * as context from Phase 3 onwards.
 */
@RestController
@RequestMapping("/v1/campaigns/{campaignId}/memory")
@Tag(name = "Campaign Memory", description = "Everything the application remembers about a campaign")
class CampaignMemoryController(
    private val orchestrator: CampaignMemoryOrchestrator,
) {

    @GetMapping("/stats")
    @Operation(summary = "Counts of everything stored for this campaign")
    fun stats(@PathVariable campaignId: Long): CampaignMemoryStats =
        orchestrator.getStats(campaignId)

    @GetMapping
    @Operation(summary = "Full structured snapshot of the campaign's memory (AI context source)")
    fun snapshot(@PathVariable campaignId: Long): CampaignMemorySnapshot =
        orchestrator.getSnapshot(campaignId)
}


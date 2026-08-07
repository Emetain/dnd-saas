package com.dndsaas.orchestration

import com.dndsaas.dto.CampaignMemorySnapshot
import com.dndsaas.dto.CampaignMemoryStats
import com.dndsaas.service.CampaignMemoryService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for Campaign Memory as a whole.
 *
 * From Phase 3 onwards the AI service will call [snapshot] to collect context
 * before building a prompt, so every generated NPC, quest or item fits the
 * world that already exists.
 */
@Component
class CampaignMemoryOrchestrator(
    private val campaignMemoryService: CampaignMemoryService,
) {

    fun getStats(campaignId: Long): CampaignMemoryStats = campaignMemoryService.stats(campaignId)

    fun getSnapshot(campaignId: Long): CampaignMemorySnapshot =
        campaignMemoryService.snapshot(campaignId)
}


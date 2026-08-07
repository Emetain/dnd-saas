package com.dndsaas.service

import com.dndsaas.dto.CampaignMemorySnapshot
import com.dndsaas.dto.CampaignMemoryStats
import com.dndsaas.repository.CharacterRepository
import com.dndsaas.repository.SessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reads Campaign Memory as a whole.
 *
 * Individual services own one kind of object each; this service assembles them
 * into a single structured snapshot of everything the application remembers
 * about a campaign.
 *
 * In Phase 3 the AI context collector will call [snapshot] before every
 * generation, so the model always knows the existing cities, factions, NPCs,
 * active quests and campaign history.
 */
@Service
@Transactional(readOnly = true)
class CampaignMemoryService(
    private val campaignService: CampaignService,
    private val locationService: LocationService,
    private val factionService: FactionService,
    private val npcService: NpcService,
    private val questService: QuestService,
    private val itemService: ItemService,
    private val worldEventService: WorldEventService,
    private val relationshipService: RelationshipService,
    private val characterService: CharacterService,
    private val sessionService: SessionService,
    private val generationLogService: GenerationLogService,
    private val sessionRepository: SessionRepository,
    private val characterRepository: CharacterRepository,
) {

    /** Counts only — cheap enough for a dashboard. */
    fun stats(campaignId: Long): CampaignMemoryStats {
        campaignService.findEntity(campaignId)
        return CampaignMemoryStats(
            locations = locationService.countForCampaign(campaignId),
            factions = factionService.countForCampaign(campaignId),
            npcs = npcService.countForCampaign(campaignId),
            quests = questService.countForCampaign(campaignId),
            items = itemService.countForCampaign(campaignId),
            worldEvents = worldEventService.countForCampaign(campaignId),
            relationships = relationshipService.countForCampaign(campaignId),
            sessions = sessionRepository.countByCampaignId(campaignId),
            characters = characterRepository.countByCampaignId(campaignId),
        )
    }

    /** Everything the application knows about a campaign, in structured form. */
    fun snapshot(campaignId: Long): CampaignMemorySnapshot {
        val campaign = campaignService.get(campaignId)
        return CampaignMemorySnapshot(
            campaign = campaign,
            stats = stats(campaignId),
            locations = locationService.listByCampaign(campaignId),
            factions = factionService.listByCampaign(campaignId),
            npcs = npcService.listByCampaign(campaignId),
            quests = questService.listByCampaign(campaignId),
            items = itemService.listByCampaign(campaignId),
            worldEvents = worldEventService.listByCampaign(campaignId),
            relationships = relationshipService.listByCampaign(campaignId),
            characters = characterService.listByCampaign(campaignId),
            sessions = sessionService.listForCampaign(campaignId),
        )
    }

    /**
     * Removes all memory for a campaign in an order that respects foreign keys.
     * Called before deleting the campaign itself.
     */
    @Transactional
    fun deleteAllForCampaign(campaignId: Long) {
        generationLogService.deleteAllForCampaign(campaignId)
        relationshipService.deleteAllForCampaign(campaignId)
        itemService.deleteAllForCampaign(campaignId)
        worldEventService.deleteAllForCampaign(campaignId)
        questService.deleteAllForCampaign(campaignId)
        npcService.deleteAllForCampaign(campaignId)
        factionService.deleteAllForCampaign(campaignId)
        locationService.deleteAllForCampaign(campaignId)
    }
}


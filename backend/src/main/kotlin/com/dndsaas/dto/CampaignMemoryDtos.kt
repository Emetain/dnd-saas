package com.dndsaas.dto

/**
 * Counts of everything stored for a campaign — the dashboard view of memory.
 */
data class CampaignMemoryStats(
    val locations: Long,
    val factions: Long,
    val npcs: Long,
    val quests: Long,
    val items: Long,
    val worldEvents: Long,
    val relationships: Long,
    val sessions: Long,
    val characters: Long,
    val encounters: Long = 0,
) {
    val total: Long
        get() = locations + factions + npcs + quests + items + worldEvents + relationships + encounters
}

/**
 * A complete, structured snapshot of everything the application remembers
 * about a campaign.
 *
 * This is the object the AI service will consume in Phase 3: it is collected
 * once, condensed into a prompt, and sent with every generation request so the
 * model always writes content that fits the existing world.
 */
data class CampaignMemorySnapshot(
    val campaign: CampaignResponse,
    val stats: CampaignMemoryStats,
    val locations: List<LocationResponse>,
    val factions: List<FactionResponse>,
    val npcs: List<NpcResponse>,
    val quests: List<QuestResponse>,
    val items: List<ItemResponse>,
    val worldEvents: List<WorldEventResponse>,
    val relationships: List<RelationshipResponse>,
    val characters: List<CharacterResponse>,
    val sessions: List<SessionResponse>,
    val encounters: List<EncounterResponse> = emptyList(),
)


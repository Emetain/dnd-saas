package com.dndsaas.domain

/**
 * Every kind of thing the platform can generate.
 *
 * Each type carries the metadata the AI pipeline needs: what to call it, which
 * slices of Campaign Memory it should receive, and roughly what it costs.
 * Phase 4 adds one generator module per type; Phase 5 uses [tokenCost] for billing.
 */
enum class GenerationType(
    val label: String,
    /** Whether the NPC social web is relevant to this generation. */
    val needsRelationships: Boolean = false,
    /** Whether known items are relevant to this generation. */
    val needsItems: Boolean = false,
    /** Indicative cost in platform tokens (used from Phase 5). */
    val tokenCost: Int = 1,
    /** Whether Free users may run it. Full campaigns and free-form questions are Pro and up. */
    val availableOnFree: Boolean = true,
) {
    CAMPAIGN("Campaign", needsRelationships = true, needsItems = true, tokenCost = 10, availableOnFree = false),
    ONE_SHOT("One-Shot", needsRelationships = true, tokenCost = 8),
    NPC("NPC", needsRelationships = true, tokenCost = 1),
    BACKSTORY("Character Backstory", needsRelationships = true, tokenCost = 2),
    QUEST("Quest", needsRelationships = true, tokenCost = 2),
    ENCOUNTER("Encounter", tokenCost = 2),
    RANDOM_ENCOUNTER("Random Encounter", tokenCost = 1),
    BOSS("Boss", needsRelationships = true, needsItems = true, tokenCost = 3),
    LOOT("Loot", needsItems = true, tokenCost = 1),
    SHOP("Shop", needsItems = true, tokenCost = 2),
    PUZZLE("Puzzle", tokenCost = 2),

    /** Free-form question answered with full campaign context. */
    FREEFORM("Free-form", needsRelationships = true, needsItems = true, tokenCost = 1, availableOnFree = false),
}


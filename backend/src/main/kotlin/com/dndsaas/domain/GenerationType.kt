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
    // Costs: whole worlds 25-50; single features 5-15, scaled to how much they produce.
    CAMPAIGN("Campaign", needsRelationships = true, needsItems = true, tokenCost = 50, availableOnFree = false),
    ONE_SHOT("One-Shot", needsRelationships = true, tokenCost = 25),
    /** A campaign or one-shot idea for the "Your idea" box; saved to the account for reuse. */
    IDEA("Idea", tokenCost = 5),
    /** The next session of a running campaign; cheaper than a campaign because the world exists. */
    NEXT_SESSION("Next Session", needsRelationships = true, needsItems = true, tokenCost = 25, availableOnFree = false),
    NPC("NPC", needsRelationships = true, tokenCost = 5),
    BACKSTORY("Character Backstory", needsRelationships = true, tokenCost = 10),
    QUEST("Quest", needsRelationships = true, tokenCost = 10),
    ENCOUNTER("Encounter", tokenCost = 10),
    RANDOM_ENCOUNTER("Random Encounter", tokenCost = 5),
    BOSS("Boss", needsRelationships = true, needsItems = true, tokenCost = 15),
    LOOT("Loot", needsItems = true, tokenCost = 8),
    SHOP("Shop", needsItems = true, tokenCost = 12),
    PUZZLE("Puzzle", tokenCost = 8),

    /** Free-form question answered with full campaign context. */
    FREEFORM("Free-form", needsRelationships = true, needsItems = true, tokenCost = 5, availableOnFree = false),
}


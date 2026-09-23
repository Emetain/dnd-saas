package com.dndsaas.domain

/** Kind of place a [Location] represents. */
enum class LocationType {
    CONTINENT,
    REGION,
    CITY,
    TOWN,
    VILLAGE,
    DISTRICT,
    BUILDING,
    /** A shop, with its shopkeeper and stock linked to it. */
    SHOP,
    DUNGEON,
    LANDMARK,
    PLANE,
    OTHER,
}

/** Where a [Faction] currently stands in the world. */
enum class FactionStatus {
    ACTIVE,
    RISING,
    DECLINING,
    DISBANDED,
    HIDDEN,
}

/** Whether an [Npc] is still available to the story. */
enum class NpcStatus {
    ALIVE,
    DEAD,
    MISSING,
    UNKNOWN,
}

/** How the party relates to an [Npc]. */
enum class Disposition {
    ALLY,
    FRIENDLY,
    NEUTRAL,
    UNFRIENDLY,
    HOSTILE,
    UNKNOWN,
}

/** Progress of a [Quest]. */
enum class QuestStatus {
    RUMOURED,
    AVAILABLE,
    ACTIVE,
    COMPLETED,
    FAILED,
    ABANDONED,
}

/** D&D 5e item rarity. */
enum class ItemRarity {
    COMMON,
    UNCOMMON,
    RARE,
    VERY_RARE,
    LEGENDARY,
    ARTIFACT,
}

/** The nature of a [Relationship] between two NPCs. */
enum class RelationshipType {
    ALLY,
    ENEMY,
    RIVAL,
    FAMILY,
    PARENT,
    CHILD,
    SIBLING,
    SPOUSE,
    MENTOR,
    STUDENT,
    EMPLOYER,
    EMPLOYEE,
    FRIEND,
    LOVER,
    ACQUAINTANCE,
    UNKNOWN,
}

/** How much a [WorldEvent] matters to the campaign narrative. */
enum class EventImportance {
    MINOR,
    NOTABLE,
    MAJOR,
    LEGENDARY,
}


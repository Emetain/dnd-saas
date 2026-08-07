package com.dndsaas.dto

/**
 * A lightweight pointer to another memory object.
 *
 * Responses embed these instead of whole entities so a single NPC does not drag
 * its location, faction, quests and their relations along with it.
 */
data class EntityRef(
    val id: Long,
    val name: String,
)


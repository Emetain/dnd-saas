package com.dndsaas.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * Unlinks one piece of Campaign Memory from everything that points at it, so it
 * can be deleted on its own: a quest loses its quest giver instead of blocking
 * the delete, a location's sub-locations move up to the top level, and so on.
 * Rows that make no sense without it (an NPC's relationships, an encounter's
 * creatures) go with it.
 *
 * Called by each service's `delete` inside its transaction, just before the row
 * itself is removed.
 */
@Component
class MemoryReferences(private val jdbc: JdbcTemplate) {

    fun detachNpc(id: Long) = run(
        id,
        "update items set owner_npc_id = null where owner_npc_id = ?",
        "update quests set quest_giver_id = null where quest_giver_id = ?",
        "delete from quest_npcs where npc_id = ?",
        "delete from npc_sessions where npc_id = ?",
        "delete from relationships where from_npc_id = ? or to_npc_id = ?",
    )

    fun detachLocation(id: Long) = run(
        id,
        "update locations set parent_id = null where parent_id = ?",
        "update npcs set location_id = null where location_id = ?",
        "update quests set location_id = null where location_id = ?",
        "update items set location_id = null where location_id = ?",
        "update encounters set location_id = null where location_id = ?",
        "update world_events set location_id = null where location_id = ?",
        "update factions set headquarters_id = null where headquarters_id = ?",
    )

    fun detachFaction(id: Long) = run(id, "update npcs set faction_id = null where faction_id = ?")

    fun detachQuest(id: Long) = run(id, "delete from quest_npcs where quest_id = ?")

    fun detachCharacter(id: Long) = run(id, "update items set owner_character_id = null where owner_character_id = ?")

    fun detachEncounter(id: Long) = run(id, "delete from encounter_creatures where encounter_id = ?")

    fun detachSession(id: Long) = run(
        id,
        "update encounters set session_id = null where session_id = ?",
        "update world_events set session_id = null where session_id = ?",
        "delete from npc_sessions where session_id = ?",
    )

    private fun run(id: Long, vararg statements: String) {
        for (sql in statements) {
            val placeholders = sql.count { it == '?' }
            jdbc.update(sql, *Array(placeholders) { id })
        }
    }
}

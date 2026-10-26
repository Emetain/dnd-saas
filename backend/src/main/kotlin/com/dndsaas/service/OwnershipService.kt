package com.dndsaas.service

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Checks that the ids in a URL belong to the signed-in user.
 *
 * Every campaign belongs to a user, and everything else (NPCs, sessions,
 * quests…) belongs to a campaign, so one query per id finds the owner. An id
 * owned by someone else answers "not found", so ids can't be probed.
 */
@Service
class OwnershipService(private val entityManager: EntityManager) {

    /** URL segment before `{id}` → entity, e.g. `/v1/npcs/{id}` → Npc. */
    private val entityBySegment = mapOf(
        "campaigns" to "Campaign",
        "npcs" to "Npc",
        "locations" to "Location",
        "factions" to "Faction",
        "quests" to "Quest",
        "items" to "Item",
        "encounters" to "Encounter",
        "events" to "WorldEvent",
        "relationships" to "Relationship",
        "sessions" to "Session",
        "characters" to "Character",
    )

    /** Named path variables → entity, e.g. `{campaignId}`. */
    private val entityByVariable = mapOf(
        "campaignId" to "Campaign",
        "npcId" to "Npc",
        "sessionId" to "Session",
        "characterId" to "Character",
    )

    private val idSegment = Regex("""/v1/([a-z-]+)/\{id}""")

    @Transactional(readOnly = true)
    fun check(userId: Long, pattern: String, variables: Map<String, String>) {
        variables["userId"]?.let { if (it.toLongOrNull() != userId) throw NotYourAccountException() }

        for ((name, raw) in variables) {
            val entity = when (name) {
                "id" -> idSegment.find(pattern)?.groupValues?.get(1)?.let(entityBySegment::get)
                else -> entityByVariable[name]
            } ?: continue
            val owner = raw.toLongOrNull()?.let { ownerOf(entity, it) }
            if (owner != userId) throw NoSuchElementException("${entity.replace(Regex("([a-z])([A-Z])"), "$1 $2")} $raw not found")
        }
    }

    private fun ownerOf(entity: String, id: Long): Long? {
        val query = if (entity == "Campaign") {
            "select c.user.id from Campaign c where c.id = :id"
        } else {
            "select e.campaign.user.id from $entity e where e.id = :id"
        }
        return entityManager.createQuery(query, java.lang.Long::class.java)
            .setParameter("id", id)
            .resultList
            .firstOrNull()
            ?.toLong()
    }
}

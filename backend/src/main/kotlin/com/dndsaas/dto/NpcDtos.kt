package com.dndsaas.dto

import com.dndsaas.domain.Disposition
import com.dndsaas.domain.NpcStatus
import java.time.Instant

/** Request to create or update an NPC. */
data class NpcRequest(
    val name: String,
    val race: String = "",
    val occupation: String = "",
    val role: String = "",
    val description: String = "",
    val appearance: String = "",
    val personality: String = "",
    val motivation: String = "",
    val secret: String = "",
    val voice: String = "",
    val status: NpcStatus = NpcStatus.ALIVE,
    val disposition: Disposition = Disposition.NEUTRAL,
    val locationId: Long? = null,
    val factionId: Long? = null,
    val notes: String = "",
)

data class NpcResponse(
    val id: Long,
    val campaignId: Long,
    val name: String,
    val race: String,
    val occupation: String,
    val role: String,
    val description: String,
    val appearance: String,
    val personality: String,
    val motivation: String,
    val secret: String,
    val voice: String,
    val status: NpcStatus,
    val disposition: Disposition,
    val location: EntityRef?,
    val faction: EntityRef?,
    /** Sessions this NPC appeared in. */
    val appearances: List<EntityRef>,
    val notes: String,
    val aiGenerated: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * The full picture of an NPC, including everything connected to it.
 * This is what makes Campaign Memory queryable: one call answers
 * "who is this, where do they live, who do they know, what are they involved in".
 */
data class NpcDetailResponse(
    val npc: NpcResponse,
    val relationships: List<RelationshipResponse>,
    val quests: List<EntityRef>,
    val items: List<EntityRef>,
)


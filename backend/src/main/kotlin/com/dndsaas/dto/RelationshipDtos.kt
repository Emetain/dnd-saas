package com.dndsaas.dto

import com.dndsaas.domain.RelationshipType
import java.time.Instant

/** Request to create or update a relationship between two NPCs. */
data class RelationshipRequest(
    val fromNpcId: Long,
    val toNpcId: Long,
    val type: RelationshipType = RelationshipType.ACQUAINTANCE,
    val description: String = "",
    val strength: Int = 0,
    val knownToPlayers: Boolean = false,
)

data class RelationshipResponse(
    val id: Long,
    val campaignId: Long,
    val fromNpc: EntityRef,
    val toNpc: EntityRef,
    val type: RelationshipType,
    val description: String,
    val strength: Int,
    val knownToPlayers: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)


package com.dndsaas.dto

import com.dndsaas.domain.EventImportance
import java.time.Instant

/** Request to create or update a world event. */
data class WorldEventRequest(
    val title: String,
    val description: String = "",
    val inGameDate: String = "",
    val importance: EventImportance = EventImportance.NOTABLE,
    val sessionId: Long? = null,
    val locationId: Long? = null,
    val consequences: String = "",
    val knownToPlayers: Boolean = true,
)

data class WorldEventResponse(
    val id: Long,
    val campaignId: Long,
    val title: String,
    val description: String,
    val inGameDate: String,
    val importance: EventImportance,
    val session: EntityRef?,
    val location: EntityRef?,
    val consequences: String,
    val knownToPlayers: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)


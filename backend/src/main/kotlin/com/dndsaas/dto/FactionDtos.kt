package com.dndsaas.dto

import com.dndsaas.domain.FactionStatus
import java.time.Instant

/** Request to create or update a faction. */
data class FactionRequest(
    val name: String,
    val type: String = "",
    val description: String = "",
    val goals: String = "",
    val ideology: String = "",
    val status: FactionStatus = FactionStatus.ACTIVE,
    val headquartersId: Long? = null,
    val reputationWithParty: Int = 0,
    val notes: String = "",
)

data class FactionResponse(
    val id: Long,
    val campaignId: Long,
    val name: String,
    val type: String,
    val description: String,
    val goals: String,
    val ideology: String,
    val status: FactionStatus,
    val headquarters: EntityRef?,
    val reputationWithParty: Int,
    val notes: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)


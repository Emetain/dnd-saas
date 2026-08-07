package com.dndsaas.dto

import java.time.Instant

/** Request to create or update a campaign. */
data class CampaignRequest(
    val name: String,
    val description: String = "",
    val system: String = "D&D 5e",
    val worldLore: String = "",
)

/** Campaign response (summary form, without full session list). */
data class CampaignResponse(
    val id: Long,
    val name: String,
    val description: String,
    val system: String,
    val worldLore: String,
    val sessionCount: Int,
    val characterCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)


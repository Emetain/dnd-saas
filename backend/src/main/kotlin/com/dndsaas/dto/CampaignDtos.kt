package com.dndsaas.dto

import com.dndsaas.domain.CampaignKind
import java.time.Instant

/** Request to create or update a campaign. */
data class CampaignRequest(
    val name: String,
    val description: String = "",
    val system: String = "D&D 5e",
    val worldLore: String = "",
    /** Free users can only create one-shots. */
    val kind: CampaignKind = CampaignKind.CAMPAIGN,
)

/** Campaign response (summary form, without full session list). */
data class CampaignResponse(
    val id: Long,
    /** The user who owns the campaign and pays for its generations. */
    val userId: Long,
    val kind: CampaignKind,
    val name: String,
    val description: String,
    val system: String,
    val worldLore: String,
    val sessionCount: Int,
    val characterCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)


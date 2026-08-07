package com.dndsaas.dto

import com.dndsaas.domain.ItemRarity
import java.time.Instant

/** Request to create or update an item. */
data class ItemRequest(
    val name: String,
    val type: String = "",
    val rarity: ItemRarity = ItemRarity.COMMON,
    val description: String = "",
    val properties: String = "",
    val history: String = "",
    val requiresAttunement: Boolean = false,
    val ownerCharacterId: Long? = null,
    val ownerNpcId: Long? = null,
    val locationId: Long? = null,
    val notes: String = "",
)

data class ItemResponse(
    val id: Long,
    val campaignId: Long,
    val name: String,
    val type: String,
    val rarity: ItemRarity,
    val description: String,
    val properties: String,
    val history: String,
    val requiresAttunement: Boolean,
    val ownerCharacter: EntityRef?,
    val ownerNpc: EntityRef?,
    val location: EntityRef?,
    val notes: String,
    val aiGenerated: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)


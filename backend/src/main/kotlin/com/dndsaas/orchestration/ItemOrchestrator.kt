package com.dndsaas.orchestration

import com.dndsaas.dto.ItemRequest
import com.dndsaas.dto.ItemResponse
import com.dndsaas.service.CampaignService
import com.dndsaas.service.CharacterService
import com.dndsaas.service.ItemService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for items.
 */
@Component
class ItemOrchestrator(
    private val itemService: ItemService,
    private val characterService: CharacterService,
    private val campaignService: CampaignService,
) {

    fun addItem(campaignId: Long, request: ItemRequest): ItemResponse {
        val campaign = campaignService.findEntity(campaignId)
        return itemService.create(campaign, request)
    }

    fun listItems(campaignId: Long): List<ItemResponse> {
        campaignService.findEntity(campaignId)
        return itemService.listByCampaign(campaignId)
    }

    /** Task: a player character's inventory. */
    fun listCharacterInventory(characterId: Long): List<ItemResponse> {
        characterService.findEntity(characterId)
        return itemService.listForCharacter(characterId)
    }

    fun getItem(id: Long): ItemResponse = itemService.get(id)

    fun updateItem(id: Long, request: ItemRequest): ItemResponse = itemService.update(id, request)

    fun deleteItem(id: Long) = itemService.delete(id)
}


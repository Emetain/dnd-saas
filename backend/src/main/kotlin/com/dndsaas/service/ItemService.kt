package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.Item
import com.dndsaas.dto.EntityRef
import com.dndsaas.dto.ItemRequest
import com.dndsaas.dto.ItemResponse
import com.dndsaas.repository.ItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Business logic for items — loot, artefacts and quest objects.
 */
@Service
@Transactional
class ItemService(
    private val itemRepository: ItemRepository,
    private val characterService: CharacterService,
    private val npcService: NpcService,
    private val locationService: LocationService,
) {

    fun create(campaign: Campaign, request: ItemRequest, aiGenerated: Boolean = false): ItemResponse {
        val item = Item(campaign = campaign)
        item.aiGenerated = aiGenerated
        apply(item, request)
        return toResponse(itemRepository.save(item))
    }

    fun update(id: Long, request: ItemRequest): ItemResponse {
        val item = findEntity(id)
        apply(item, request)
        return toResponse(itemRepository.save(item))
    }

    @Transactional(readOnly = true)
    fun get(id: Long): ItemResponse = toResponse(findEntity(id))

    @Transactional(readOnly = true)
    fun listByCampaign(campaignId: Long): List<ItemResponse> =
        itemRepository.findByCampaignIdOrderByNameAsc(campaignId).map(::toResponse)

    /** Everything a player character is carrying. */
    @Transactional(readOnly = true)
    fun listForCharacter(characterId: Long): List<ItemResponse> =
        itemRepository.findByOwnerCharacterIdOrderByNameAsc(characterId).map(::toResponse)

    @Transactional(readOnly = true)
    fun listForNpc(campaignId: Long, npcId: Long): List<ItemResponse> =
        itemRepository.findByCampaignIdOrderByNameAsc(campaignId)
            .filter { it.ownerNpc?.id == npcId }
            .map(::toResponse)

    fun delete(id: Long) = itemRepository.deleteById(id)

    fun findEntity(id: Long): Item =
        itemRepository.findById(id)
            .orElseThrow { NoSuchElementException("Item $id not found") }

    fun countForCampaign(campaignId: Long): Long = itemRepository.countByCampaignId(campaignId)

    fun deleteAllForCampaign(campaignId: Long) =
        itemRepository.deleteAll(itemRepository.findByCampaignIdOrderByNameAsc(campaignId))

    private fun apply(item: Item, request: ItemRequest) {
        item.name = request.name
        item.type = request.type
        item.rarity = request.rarity
        item.description = request.description
        item.properties = request.properties
        item.history = request.history
        item.requiresAttunement = request.requiresAttunement
        item.ownerCharacter = request.ownerCharacterId?.let { characterService.findEntity(it) }
        item.ownerNpc = npcService.findEntityOrNull(request.ownerNpcId)
        item.location = locationService.findEntityOrNull(request.locationId)
        item.notes = request.notes
    }

    fun toResponse(item: Item): ItemResponse =
        ItemResponse(
            id = item.id!!,
            campaignId = item.campaign!!.id!!,
            name = item.name,
            type = item.type,
            rarity = item.rarity,
            description = item.description,
            properties = item.properties,
            history = item.history,
            requiresAttunement = item.requiresAttunement,
            ownerCharacter = item.ownerCharacter?.let { EntityRef(it.id!!, it.name) },
            ownerNpc = item.ownerNpc?.let { EntityRef(it.id!!, it.name) },
            location = item.location?.let { EntityRef(it.id!!, it.name) },
            notes = item.notes,
            aiGenerated = item.aiGenerated,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
        )
}


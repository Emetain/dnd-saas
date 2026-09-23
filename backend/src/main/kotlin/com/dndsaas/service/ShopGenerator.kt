package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.Disposition
import com.dndsaas.domain.GenerationType
import com.dndsaas.domain.ItemRarity
import com.dndsaas.domain.LocationType
import com.dndsaas.domain.NpcStatus
import com.dndsaas.dto.CreatedEntity
import com.dndsaas.dto.ItemRequest
import com.dndsaas.dto.LocationRequest
import com.dndsaas.dto.NpcRequest
import com.dndsaas.dto.TypedGenerationRequest
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service

/**
 * Generates a shop as three connected objects: the premises (a location), the
 * shopkeeper (an NPC who lives there) and the stock (items held in the shop).
 *
 * A good example of why persistence matters — the party can come back, and the
 * shopkeeper is a real NPC who can be given quests later.
 */
@Service
class ShopGenerator(
    private val locationService: LocationService,
    private val npcService: NpcService,
    private val itemService: ItemService,
) : Generator {

    override val type = GenerationType.SHOP

    override val description =
        "A shop, its keeper and its stock — saved as a location, an NPC and real items " +
            "the party can come back to."

    override val requiredFields = listOf("name")

    override val jsonSchema = """
        {
          "name": "the shop's name",
          "description": "what it sells and who comes here",
          "atmosphere": "what it is like inside: smell, sound, light, clutter",
          "parentLocationName": "an EXISTING settlement or district containing it, or null",
          "shopkeeper": {
            "name": "",
            "race": "",
            "description": "",
            "appearance": "",
            "personality": "",
            "motivation": "",
            "secret": "something about them or the shop the party could discover",
            "voice": "how they speak",
            "hagglingStyle": "how they respond to bargaining"
          },
          "stock": [
            {
              "name": "",
              "type": "e.g. Weapon, Potion, Wondrous item",
              "rarity": "COMMON, UNCOMMON, RARE, VERY_RARE, LEGENDARY or ARTIFACT",
              "description": "",
              "properties": "mechanical effect, or empty for mundane goods",
              "price": "e.g. '50 gp'"
            }
          ],
          "rumour": "something the shopkeeper will mention if the party is friendly"
        }
    """.trimIndent()

    override val guidance = """
        Make the shop a place worth returning to.

        - Put it inside an existing settlement or district from the campaign context.
        - The shopkeeper needs a want of their own, so they can become a quest giver
          later rather than a vending machine.
        - Stock should reflect the campaign: a besieged town sells different goods to
          a wealthy port. Mostly mundane, with one or two things worth talking about.
        - The rumour should point at something that already exists in the campaign.
        - Prices in gold pieces, roughly in line with 5e norms.
    """.trimIndent()

    override fun persist(
        campaign: Campaign,
        content: JsonNode,
        request: TypedGenerationRequest,
    ): List<CreatedEntity> {
        val created = mutableListOf<CreatedEntity>()

        val parentId = GeneratorJson.textOrNull(content, "parentLocationName")?.let { wanted ->
            locationService.listByCampaign(campaign.id!!)
                .firstOrNull { it.name.equals(wanted, ignoreCase = true) }?.id
        }

        // 1. The premises.
        val shop = locationService.create(
            campaign,
            LocationRequest(
                name = GeneratorJson.text(content, "name"),
                type = LocationType.SHOP,
                description = GeneratorJson.text(content, "description"),
                atmosphere = GeneratorJson.text(content, "atmosphere"),
                parentId = parentId,
                notes = GeneratorJson.text(content, "rumour").takeIf { it.isNotBlank() }
                    ?.let { "Rumour the shopkeeper shares: $it" } ?: "",
                discovered = true,
            ),
        )
        created += CreatedEntity("location", shop.id, shop.name)

        // 2. The shopkeeper, who lives there.
        val keeperNode = content.get("shopkeeper")
        if (keeperNode != null && !keeperNode.isNull) {
            val keeperName = GeneratorJson.text(keeperNode, "name")
            if (keeperName.isNotBlank()) {
                val haggling = GeneratorJson.text(keeperNode, "hagglingStyle")
                val keeper = npcService.create(
                    campaign,
                    NpcRequest(
                        name = keeperName,
                        race = GeneratorJson.text(keeperNode, "race"),
                        occupation = "Shopkeeper",
                        role = "Merchant",
                        description = GeneratorJson.text(keeperNode, "description"),
                        appearance = GeneratorJson.text(keeperNode, "appearance"),
                        personality = GeneratorJson.text(keeperNode, "personality"),
                        motivation = GeneratorJson.text(keeperNode, "motivation"),
                        secret = GeneratorJson.text(keeperNode, "secret"),
                        voice = GeneratorJson.text(keeperNode, "voice"),
                        status = NpcStatus.ALIVE,
                        disposition = Disposition.NEUTRAL,
                        locationId = shop.id,
                        notes = if (haggling.isBlank()) "" else "Haggling: $haggling",
                    ),
                    aiGenerated = true,
                )
                created += CreatedEntity("npc", keeper.id, keeper.name)
            }
        }

        // 3. The stock, sitting in the shop.
        GeneratorJson.array(content, "stock").forEach { node ->
            val name = GeneratorJson.text(node, "name")
            if (name.isBlank()) return@forEach
            val price = GeneratorJson.text(node, "price")
            val item = itemService.create(
                campaign,
                ItemRequest(
                    name = name,
                    type = GeneratorJson.text(node, "type"),
                    rarity = GeneratorJson.enum(GeneratorJson.textOrNull(node, "rarity"), ItemRarity.COMMON),
                    description = GeneratorJson.text(node, "description"),
                    properties = GeneratorJson.text(node, "properties"),
                    locationId = shop.id,
                    notes = if (price.isBlank()) "For sale at ${shop.name}" else "For sale at ${shop.name}: $price",
                ),
                aiGenerated = true,
            )
            created += CreatedEntity("item", item.id, item.name)
        }

        return created
    }
}


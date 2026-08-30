package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.GenerationType
import com.dndsaas.domain.ItemRarity
import com.dndsaas.dto.CreatedEntity
import com.dndsaas.dto.ItemRequest
import com.dndsaas.dto.TypedGenerationRequest
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service

/**
 * Generates treasure that fits the campaign's tone and the party's level, and
 * saves each piece as a real item in Campaign Memory.
 */
@Service
class LootGenerator(
    private val itemService: ItemService,
    private val locationService: LocationService,
) : Generator {

    override val type = GenerationType.LOOT

    override val description =
        "Treasure suited to the campaign's tone and the party's level, with history " +
            "that ties it to the world."

    override val requiredFields = listOf("items")

    override val jsonSchema = """
        {
          "hoardDescription": "how the treasure is presented: where it sits, what it looks like",
          "currency": "coins and valuables, e.g. '180 gp, a pearl worth 100 gp'",
          "items": [
            {
              "name": "the item's name",
              "type": "e.g. Weapon, Armour, Wondrous item, Potion",
              "rarity": "COMMON, UNCOMMON, RARE, VERY_RARE, LEGENDARY or ARTIFACT",
              "description": "what it looks like",
              "properties": "its mechanical effect in 5e terms",
              "history": "where it came from, tied to this world",
              "requiresAttunement": false
            }
          ],
          "locationName": "an EXISTING location from the campaign context, or null"
        }
    """.trimIndent()

    override val guidance = """
        Treasure should tell the players something about the world.

        - Give each magic item a history that connects to an existing faction,
          location, NPC or event from the campaign context.
        - Never re-create an item that already exists in the campaign.
        - Match rarity to the party's level: mostly common and uncommon at low
          levels, with a rare item only when it is earned.
        - Mechanical effects must be written in real 5e terms.
        - At least one item should be interesting rather than powerful: something
          that creates a scene, not just a bigger number.
    """.trimIndent()

    override fun persist(
        campaign: Campaign,
        content: JsonNode,
        request: TypedGenerationRequest,
    ): List<CreatedEntity> {
        val locationId = GeneratorJson.textOrNull(content, "locationName")?.let { wanted ->
            locationService.listByCampaign(campaign.id!!)
                .firstOrNull { it.name.equals(wanted, ignoreCase = true) }?.id
        }
        val hoard = GeneratorJson.text(content, "hoardDescription")
        val currency = GeneratorJson.text(content, "currency")
        val notes = listOfNotNull(
            hoard.takeIf { it.isNotBlank() }?.let { "Found with: $it" },
            currency.takeIf { it.isNotBlank() }?.let { "Alongside: $it" },
        ).joinToString("\n")

        return GeneratorJson.array(content, "items").mapNotNull { node ->
            val name = GeneratorJson.text(node, "name")
            if (name.isBlank()) return@mapNotNull null
            val item = itemService.create(
                campaign,
                ItemRequest(
                    name = name,
                    type = GeneratorJson.text(node, "type"),
                    rarity = GeneratorJson.enum(GeneratorJson.textOrNull(node, "rarity"), ItemRarity.COMMON),
                    description = GeneratorJson.text(node, "description"),
                    properties = GeneratorJson.text(node, "properties"),
                    history = GeneratorJson.text(node, "history"),
                    requiresAttunement = GeneratorJson.bool(node, "requiresAttunement"),
                    locationId = locationId,
                    notes = notes,
                ),
                aiGenerated = true,
            )
            CreatedEntity("item", item.id, item.name)
        }
    }
}


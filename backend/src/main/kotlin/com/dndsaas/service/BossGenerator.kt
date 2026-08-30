package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.Disposition
import com.dndsaas.domain.GenerationType
import com.dndsaas.domain.ItemRarity
import com.dndsaas.domain.NpcStatus
import com.dndsaas.dto.CreatedEntity
import com.dndsaas.dto.ItemRequest
import com.dndsaas.dto.NpcRequest
import com.dndsaas.dto.TypedGenerationRequest
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service

/**
 * Generates a villain: an NPC, their lair, their tactics, and the treasure they
 * guard. Saved as a real NPC so the rest of the platform can reference them.
 */
@Service
class BossGenerator(
    private val npcService: NpcService,
    private val itemService: ItemService,
    private val locationService: LocationService,
    private val factionService: FactionService,
) : Generator {

    override val type = GenerationType.BOSS

    override val description =
        "A villain with a plan already in motion: their lair, phases, tactics, weakness " +
            "and the treasure they hoard."

    override val requiredFields = listOf("name")

    override val jsonSchema = """
        {
          "name": "the villain's name",
          "race": "",
          "occupation": "their role in the world",
          "description": "who they are and why they are dangerous",
          "appearance": "",
          "personality": "",
          "motivation": "the concrete goal they are already pursuing",
          "secret": "what the party does not know",
          "voice": "how they speak",
          "plan": "the steps of their scheme, and how far along it already is",
          "lairDescription": "where they hold power, and what that place is like",
          "locationName": "an EXISTING location from the campaign context, or null",
          "factionName": "an EXISTING faction from the campaign context, or null",
          "challengeRating": "a CR appropriate to the party",
          "tactics": "how they fight: opening move, phase change, what they do when losing",
          "lairActions": ["things the lair itself does on initiative count 20"],
          "weakness": "the lever the party can discover and use",
          "minions": ["SRD 5.1 creatures that fight alongside them"],
          "treasure": [
            {
              "name": "an item they guard",
              "type": "e.g. Weapon, Wondrous item",
              "rarity": "COMMON, UNCOMMON, RARE, VERY_RARE, LEGENDARY or ARTIFACT",
              "description": "",
              "properties": "its mechanical effect",
              "requiresAttunement": true
            }
          ]
        }
    """.trimIndent()

    override val guidance = """
        Build a villain the campaign has been waiting for.

        - Their plan must already be in motion, with visible consequences the party
          could have noticed. A villain who is merely waiting is not a villain.
        - Connect them to an existing faction, location or NPC wherever possible.
        - The weakness must be discoverable through play, not handed over.
        - Give the fight a shape: an opening, a moment where the fight changes, and
          what they do when they are losing. Villains should flee if it serves them.
        - Any minions must be creatures from the D&D 5e SRD 5.1.
    """.trimIndent()

    override fun persist(
        campaign: Campaign,
        content: JsonNode,
        request: TypedGenerationRequest,
    ): List<CreatedEntity> {
        val created = mutableListOf<CreatedEntity>()

        val locationId = GeneratorJson.textOrNull(content, "locationName")?.let { wanted ->
            locationService.listByCampaign(campaign.id!!)
                .firstOrNull { it.name.equals(wanted, ignoreCase = true) }?.id
        }
        val factionId = GeneratorJson.textOrNull(content, "factionName")?.let { wanted ->
            factionService.listByCampaign(campaign.id!!)
                .firstOrNull { it.name.equals(wanted, ignoreCase = true) }?.id
        }

        // Everything a DM needs mid-fight goes into the notes, so it is one glance away.
        val notes = buildString {
            GeneratorJson.text(content, "plan").takeIf { it.isNotBlank() }?.let {
                appendLine("## Plan"); appendLine(it); appendLine()
            }
            GeneratorJson.text(content, "lairDescription").takeIf { it.isNotBlank() }?.let {
                appendLine("## Lair"); appendLine(it); appendLine()
            }
            GeneratorJson.text(content, "challengeRating").takeIf { it.isNotBlank() }?.let {
                appendLine("Challenge Rating: $it"); appendLine()
            }
            GeneratorJson.text(content, "tactics").takeIf { it.isNotBlank() }?.let {
                appendLine("## Tactics"); appendLine(it); appendLine()
            }
            GeneratorJson.strings(content, "lairActions").takeIf { it.isNotEmpty() }?.let { actions ->
                appendLine("## Lair actions")
                actions.forEach { appendLine("- $it") }
                appendLine()
            }
            GeneratorJson.strings(content, "minions").takeIf { it.isNotEmpty() }?.let { minions ->
                appendLine("## Minions")
                minions.forEach { appendLine("- $it") }
                appendLine()
            }
            GeneratorJson.text(content, "weakness").takeIf { it.isNotBlank() }?.let {
                appendLine("## Weakness"); appendLine(it)
            }
        }

        val boss = npcService.create(
            campaign,
            NpcRequest(
                name = GeneratorJson.text(content, "name"),
                race = GeneratorJson.text(content, "race"),
                occupation = GeneratorJson.text(content, "occupation"),
                role = "Villain",
                description = GeneratorJson.text(content, "description"),
                appearance = GeneratorJson.text(content, "appearance"),
                personality = GeneratorJson.text(content, "personality"),
                motivation = GeneratorJson.text(content, "motivation"),
                secret = GeneratorJson.text(content, "secret"),
                voice = GeneratorJson.text(content, "voice"),
                status = NpcStatus.ALIVE,
                disposition = Disposition.HOSTILE,
                locationId = locationId,
                factionId = factionId,
                notes = notes.trim(),
            ),
            aiGenerated = true,
        )
        created += CreatedEntity("npc", boss.id, boss.name)

        // The hoard becomes real items, held by the villain.
        GeneratorJson.array(content, "treasure").forEach { node ->
            val name = GeneratorJson.text(node, "name")
            if (name.isBlank()) return@forEach
            val item = itemService.create(
                campaign,
                ItemRequest(
                    name = name,
                    type = GeneratorJson.text(node, "type"),
                    rarity = GeneratorJson.enum(
                        GeneratorJson.textOrNull(node, "rarity"),
                        ItemRarity.RARE,
                    ),
                    description = GeneratorJson.text(node, "description"),
                    properties = GeneratorJson.text(node, "properties"),
                    requiresAttunement = GeneratorJson.bool(node, "requiresAttunement"),
                    ownerNpcId = boss.id,
                ),
                aiGenerated = true,
            )
            created += CreatedEntity("item", item.id, item.name)
        }

        return created
    }
}


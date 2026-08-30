package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.CreatedEntity
import com.dndsaas.dto.TypedGenerationRequest
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service

/**
 * Generates a player character's backstory, woven into the existing world.
 *
 * Requires `targetId` — the character the backstory belongs to. Writes the
 * prose onto the character itself and, where the story calls for it, creates
 * the NPCs it introduces so they exist for later sessions rather than only
 * living in a paragraph of text.
 */
@Service
class BackstoryGenerator(
    private val characterService: CharacterService,
    private val npcService: NpcService,
    private val locationService: LocationService,
    private val factionService: FactionService,
) : Generator {

    override val type = GenerationType.BACKSTORY

    override val description =
        "A player character backstory that ties into existing locations, factions and " +
            "NPCs, creating any new NPCs it introduces."

    override val requiredFields = listOf("backstory")

    override val jsonSchema = """
        {
          "backstory": "4-6 paragraphs of prose, written in third person",
          "hook": "one sentence a Dungeon Master can use to pull this character into the plot",
          "connectionsToWorld": "how this backstory ties to existing locations, factions or NPCs",
          "newNpcs": [
            {
              "name": "someone the backstory introduces, e.g. a mentor or rival",
              "relationshipToCharacter": "e.g. mentor, sibling, rival, old flame",
              "description": "",
              "motivation": "",
              "locationName": "an EXISTING location from the campaign context, or null"
            }
          ]
        }
    """.trimIndent()

    override val guidance = """
        Write a backstory for an existing player character, using the ability
        scores, class and race already on file for them.

        - Reference at least one existing location, faction or NPC from the
          campaign context, so the character's history is part of this world.
        - The hook must be something a Dungeon Master can act on in a session,
          not a vague theme.
        - Any new NPCs the backstory needs (a mentor, a rival, a lost sibling)
          should be listed in newNpcs so they can be created properly, with a
          clear relationship to the character.
        - Avoid tragic-backstory clichés unless the Dungeon Master's instruction
          asks for one specifically.
    """.trimIndent()

    override fun extraInstruction(campaign: Campaign, request: TypedGenerationRequest): String {
        val characterId = request.targetId
            ?: return "\nNo character was specified — decline gracefully by returning an empty backstory."
        val character = runCatching { characterService.get(characterId) }.getOrNull()
            ?: return "\nCharacter $characterId could not be found."
        return buildString {
            appendLine()
            appendLine("## The character this backstory is for")
            appendLine(
                "${character.name}, a level ${character.level} ${character.race} " +
                    "${character.characterClass} with background '${character.background}'.",
            )
            if (character.backstory.isNotBlank()) {
                appendLine("They already have a short backstory to build on: ${character.backstory}")
            }
        }
    }

    override fun persist(
        campaign: Campaign,
        content: JsonNode,
        request: TypedGenerationRequest,
    ): List<CreatedEntity> {
        val characterId = request.targetId
            ?: throw IllegalArgumentException("targetId (the character's id) is required for BACKSTORY generation")
        val created = mutableListOf<CreatedEntity>()

        val backstory = GeneratorJson.text(content, "backstory")
        val hook = GeneratorJson.text(content, "hook")
        val fullText = if (hook.isBlank()) backstory else "$backstory\n\nHook: $hook"

        val character = characterService.updateBackstory(characterId, fullText)
        created += CreatedEntity("character", character.id, character.name)

        GeneratorJson.array(content, "newNpcs").forEach { node ->
            val name = GeneratorJson.text(node, "name")
            if (name.isBlank()) return@forEach
            val locationId = GeneratorJson.textOrNull(node, "locationName")?.let { wanted ->
                locationService.listByCampaign(campaign.id!!)
                    .firstOrNull { it.name.equals(wanted, ignoreCase = true) }?.id
            }
            val relationship = GeneratorJson.text(node, "relationshipToCharacter")
            val npc = npcService.create(
                campaign,
                com.dndsaas.dto.NpcRequest(
                    name = name,
                    description = GeneratorJson.text(node, "description"),
                    motivation = GeneratorJson.text(node, "motivation"),
                    role = "Tied to ${character.name}'s backstory",
                    locationId = locationId,
                    notes = if (relationship.isBlank()) {
                        ""
                    } else {
                        "Relationship to ${character.name}: $relationship"
                    },
                ),
                aiGenerated = true,
            )
            created += CreatedEntity("npc", npc.id, npc.name)
        }

        return created
    }
}


package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.GenerationType
import com.dndsaas.domain.QuestStatus
import com.dndsaas.dto.CreatedEntity
import com.dndsaas.dto.QuestRequest
import com.dndsaas.dto.TypedGenerationRequest
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service

/**
 * Generates a quest wired into the campaign's existing NPCs and locations.
 */
@Service
class QuestGenerator(
    private val questService: QuestService,
    private val npcService: NpcService,
    private val locationService: LocationService,
) : Generator {

    override val type = GenerationType.QUEST

    override val description =
        "A quest with a hook, an objective and a reward, given by an existing NPC " +
            "and set in an existing location."

    override val requiredFields = listOf("title")

    override val jsonSchema = """
        {
          "title": "a short, evocative quest name",
          "description": "what is really going on, for the Dungeon Master",
          "hook": "how the party hears about it, in one or two sentences",
          "objective": "what the party must actually do",
          "reward": "what they get, which need not be only money",
          "questGiverName": "an EXISTING NPC from the campaign context, or null",
          "locationName": "an EXISTING location from the campaign context, or null",
          "involvedNpcNames": ["EXISTING NPCs entangled in this quest"],
          "complication": "what goes wrong, or what the party is not told",
          "status": "RUMOURED, AVAILABLE or ACTIVE"
        }
    """.trimIndent()

    override val guidance = """
        Build the quest out of what the campaign already contains.

        - Prefer an existing NPC as the quest giver, and existing NPCs as
          participants. Only use names that appear in the campaign context.
        - Connect the quest to an active thread, a faction's goal, or a player
          character's backstory where one fits.
        - The complication is the important part: a quest that goes exactly as
          described is not worth playing.
        - Do not resolve the quest. Leave the outcome to the table.
    """.trimIndent()

    override fun persist(
        campaign: Campaign,
        content: JsonNode,
        request: TypedGenerationRequest,
    ): List<CreatedEntity> {
        val npcs = npcService.listByCampaign(campaign.id!!)
        fun npcId(name: String?): Long? =
            name?.let { wanted -> npcs.firstOrNull { it.name.equals(wanted, ignoreCase = true) }?.id }

        val locationId = GeneratorJson.textOrNull(content, "locationName")?.let { wanted ->
            locationService.listByCampaign(campaign.id!!)
                .firstOrNull { it.name.equals(wanted, ignoreCase = true) }?.id
        }

        // The complication belongs in the DM-only notes, not the player-facing text.
        val complication = GeneratorJson.text(content, "complication")

        val quest = questService.create(
            campaign,
            QuestRequest(
                title = GeneratorJson.text(content, "title"),
                description = GeneratorJson.text(content, "description"),
                hook = GeneratorJson.text(content, "hook"),
                objective = GeneratorJson.text(content, "objective"),
                status = GeneratorJson.enum(
                    GeneratorJson.textOrNull(content, "status"),
                    QuestStatus.AVAILABLE,
                ),
                questGiverId = npcId(GeneratorJson.textOrNull(content, "questGiverName")),
                locationId = locationId,
                involvedNpcIds = GeneratorJson.strings(content, "involvedNpcNames").mapNotNull { npcId(it) },
                reward = GeneratorJson.text(content, "reward"),
                notes = if (complication.isBlank()) "" else "Complication: $complication",
            ),
            aiGenerated = true,
        )

        return listOf(CreatedEntity("quest", quest.id, quest.title))
    }
}


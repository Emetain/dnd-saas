package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.Disposition
import com.dndsaas.domain.GenerationType
import com.dndsaas.domain.NpcStatus
import com.dndsaas.domain.RelationshipType
import com.dndsaas.dto.CreatedEntity
import com.dndsaas.dto.NpcRequest
import com.dndsaas.dto.RelationshipRequest
import com.dndsaas.dto.TypedGenerationRequest
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service

/**
 * Generates a non-player character who fits the existing world.
 *
 * Also creates any relationships the model draws to NPCs that already exist,
 * so the social graph thickens with every generation instead of accumulating
 * disconnected strangers.
 */
@Service
class NpcGenerator(
    private val npcService: NpcService,
    private val locationService: LocationService,
    private val factionService: FactionService,
    private val relationshipService: RelationshipService,
) : Generator {

    override val type = GenerationType.NPC

    override val description =
        "A non-player character rooted in the campaign: their motivation, secret, voice, " +
            "where they live, who they serve and how they relate to existing NPCs."

    override val requiredFields = listOf("name")

    override val jsonSchema = """
        {
          "name": "a name that does not already exist in this campaign",
          "race": "",
          "occupation": "",
          "role": "narrative role, e.g. Quest giver, Rival, Informant",
          "description": "who they are, in two or three sentences",
          "appearance": "what the players notice first",
          "personality": "how they behave",
          "motivation": "what they actually want",
          "secret": "something the players do not know yet",
          "voice": "how they speak: accent, cadence, a phrase they repeat",
          "locationName": "an EXISTING location from the campaign context, or null",
          "factionName": "an EXISTING faction from the campaign context, or null",
          "status": "ALIVE, DEAD, MISSING or UNKNOWN",
          "disposition": "how they feel about the party: ALLY, FRIENDLY, NEUTRAL, UNFRIENDLY, HOSTILE or UNKNOWN",
          "relationships": [
            {
              "npcName": "an EXISTING NPC from the campaign context",
              "type": "ALLY, ENEMY, RIVAL, FAMILY, PARENT, CHILD, SIBLING, SPOUSE, MENTOR, STUDENT, EMPLOYER, EMPLOYEE, FRIEND, LOVER or ACQUAINTANCE",
              "description": "the nature of the connection",
              "strength": "-100 to 100"
            }
          ]
        }
    """.trimIndent()

    override val guidance = """
        Ground this NPC in the world that already exists.

        - Place them in an existing location and, where it fits, an existing faction.
        - Give them at least one relationship to an existing NPC. This is what makes
          the world feel connected rather than a list of strangers.
        - Only use names from the campaign context in locationName, factionName and
          relationships.npcName. If nothing suitable exists, use null and add no
          relationships — never invent a reference.
        - The secret should be usable: something that changes a scene if revealed.
        - The motivation should explain what they do when the party is not watching.
    """.trimIndent()

    override fun persist(
        campaign: Campaign,
        content: JsonNode,
        request: TypedGenerationRequest,
    ): List<CreatedEntity> {
        val created = mutableListOf<CreatedEntity>()

        val locationId = GeneratorJson.textOrNull(content, "locationName")
            ?.let { name -> findLocationId(campaign, name) }
        val factionId = GeneratorJson.textOrNull(content, "factionName")
            ?.let { name -> findFactionId(campaign, name) }

        val npc = npcService.create(
            campaign,
            NpcRequest(
                name = GeneratorJson.text(content, "name"),
                race = GeneratorJson.text(content, "race"),
                occupation = GeneratorJson.text(content, "occupation"),
                role = GeneratorJson.text(content, "role"),
                description = GeneratorJson.text(content, "description"),
                appearance = GeneratorJson.text(content, "appearance"),
                personality = GeneratorJson.text(content, "personality"),
                motivation = GeneratorJson.text(content, "motivation"),
                secret = GeneratorJson.text(content, "secret"),
                voice = GeneratorJson.text(content, "voice"),
                status = GeneratorJson.enum(GeneratorJson.textOrNull(content, "status"), NpcStatus.ALIVE),
                disposition = GeneratorJson.enum(
                    GeneratorJson.textOrNull(content, "disposition"),
                    Disposition.NEUTRAL,
                ),
                locationId = locationId,
                factionId = factionId,
            ),
            aiGenerated = true,
        )
        created += CreatedEntity("npc", npc.id, npc.name)

        // Link the new NPC into the existing social web.
        GeneratorJson.array(content, "relationships").forEach { link ->
            val otherName = GeneratorJson.textOrNull(link, "npcName") ?: return@forEach
            val otherId = findNpcId(campaign, otherName) ?: return@forEach
            if (otherId == npc.id) return@forEach
            val relationship = relationshipService.create(
                campaign,
                RelationshipRequest(
                    fromNpcId = npc.id,
                    toNpcId = otherId,
                    type = GeneratorJson.enum(
                        GeneratorJson.textOrNull(link, "type"),
                        RelationshipType.ACQUAINTANCE,
                    ),
                    description = GeneratorJson.text(link, "description"),
                    strength = GeneratorJson.int(link, "strength"),
                ),
            )
            created += CreatedEntity(
                "relationship",
                relationship.id,
                "${relationship.fromNpc.name} → ${relationship.toNpc.name}",
            )
        }

        return created
    }

    private fun findLocationId(campaign: Campaign, name: String): Long? =
        locationService.listByCampaign(campaign.id!!)
            .firstOrNull { it.name.equals(name, ignoreCase = true) }?.id

    private fun findFactionId(campaign: Campaign, name: String): Long? =
        factionService.listByCampaign(campaign.id!!)
            .firstOrNull { it.name.equals(name, ignoreCase = true) }?.id

    private fun findNpcId(campaign: Campaign, name: String): Long? =
        npcService.listByCampaign(campaign.id!!)
            .firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
}


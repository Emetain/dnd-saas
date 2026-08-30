package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.CreatureRole
import com.dndsaas.domain.EncounterDifficulty
import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.CreatedEntity
import com.dndsaas.dto.EncounterCreatureDto
import com.dndsaas.dto.EncounterRequest
import com.dndsaas.dto.TypedGenerationRequest
import com.dndsaas.repository.CharacterRepository
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service

/**
 * Generates a prepared encounter with a real creature list.
 *
 * The model proposes the creatures; the service layer then costs them against
 * the party's actual levels using the 5e rules, so the Dungeon Master is told
 * what the fight is genuinely worth rather than what the model guessed.
 */
@Service
class EncounterGenerator(
    private val encounterService: EncounterService,
    private val locationService: LocationService,
    private val characterRepository: CharacterRepository,
    private val calculationService: EncounterCalculationService,
) : Generator {

    override val type = GenerationType.ENCOUNTER

    override val description =
        "A prepared encounter with a structured creature list, tactics, terrain and an " +
            "objective — automatically rated against your party's real levels."

    override val requiredFields = listOf("title")

    override val jsonSchema = """
        {
          "title": "a short name for the encounter",
          "description": "what the party runs into and why it is here",
          "objective": "what success means — not every fight is 'kill them all'",
          "terrain": "the battlefield: cover, elevation, hazards, light",
          "setup": "read-aloud text for the moment the encounter begins",
          "resolution": "how it can end without a fight, and when enemies flee or surrender",
          "creatures": [
            {
              "name": "an SRD 5.1 creature name",
              "count": 3,
              "challengeRating": "1/4",
              "role": "BRUTE, SKIRMISHER, CONTROLLER, ARTILLERY, LEADER, MINION or SOLO",
              "tactics": "what this creature does on its turns"
            }
          ],
          "locationName": "an EXISTING location from the campaign context, or null"
        }
    """.trimIndent()

    override val guidance = """
        Design a fight that belongs to this campaign and to this party.

        Creature rules — these matter:
        - Use ONLY creatures from the D&D 5e SRD 5.1 (goblins, bandits, wolves,
          cultists, zombies, ogres, wights and so on). Never use creatures that are
          not in the SRD.
        - You may reskin an SRD creature to fit the story: keep the SRD name in
          "name" and explain the reskin in "tactics" or "description".
        - Respect the XP budget you are given. Prefer a mix of roles over a single
          slab of hit points: a LEADER plus SKIRMISHERS plays far better than five
          identical brutes.
        - Give every creature entry concrete tactics — an opening move and what it
          does when the fight turns against it.

        The encounter itself:
        - Tie the enemies to a faction, quest or NPC from the campaign context.
        - Terrain should give the players something to use, not just decoration.
        - Give the fight an objective beyond killing, where the story allows.
    """.trimIndent()

    /**
     * Tells the model the party's real XP budget.
     *
     * This is the crucial difference from asking a chat model for an encounter:
     * the budget is computed from the party's actual levels and size, so the
     * result is balanced rather than plausible-sounding.
     */
    override fun extraInstruction(campaign: Campaign, request: TypedGenerationRequest): String {
        val party = characterRepository.findByCampaignIdOrderByNameAsc(campaign.id!!).filter { it.active }
        if (party.isEmpty()) {
            return "The party roster is unknown. Assume four characters of level 3."
        }
        val target = GeneratorJson.enum(request.difficulty.ifBlank { null }, EncounterDifficulty.MEDIUM)
        val budget = calculationService.budgetFor(party, target)
        val thresholds = calculationService.partyThresholds(party)
        return buildString {
            appendLine()
            appendLine("## Encounter budget (calculated from the real party)")
            appendLine("Party: ${party.size} characters, levels ${party.joinToString { it.level.toString() }}.")
            appendLine(
                "XP thresholds — easy ${thresholds.easy}, medium ${thresholds.medium}, " +
                    "hard ${thresholds.hard}, deadly ${thresholds.deadly}.",
            )
            appendLine("Target difficulty: $target. Aim for roughly $budget adjusted XP.")
            appendLine(
                "Remember the encounter multiplier: more creatures raise the effective " +
                    "difficulty above their raw XP total.",
            )
        }
    }

    override fun persist(
        campaign: Campaign,
        content: JsonNode,
        request: TypedGenerationRequest,
    ): List<CreatedEntity> {
        val creatures = GeneratorJson.array(content, "creatures").mapNotNull { node ->
            val name = GeneratorJson.text(node, "name")
            if (name.isBlank()) {
                null
            } else {
                EncounterCreatureDto(
                    name = name,
                    count = GeneratorJson.int(node, "count", 1).coerceIn(1, 100),
                    challengeRating = GeneratorJson.text(node, "challengeRating").ifBlank { "0" },
                    role = GeneratorJson.enum(GeneratorJson.textOrNull(node, "role"), CreatureRole.BRUTE),
                    tactics = GeneratorJson.text(node, "tactics"),
                )
            }
        }

        val locationId = GeneratorJson.textOrNull(content, "locationName")?.let { wanted ->
            locationService.listByCampaign(campaign.id!!)
                .firstOrNull { it.name.equals(wanted, ignoreCase = true) }?.id
        }

        val encounter = encounterService.create(
            campaign,
            EncounterRequest(
                title = GeneratorJson.text(content, "title"),
                description = GeneratorJson.text(content, "description"),
                objective = GeneratorJson.text(content, "objective"),
                terrain = GeneratorJson.text(content, "terrain"),
                setup = GeneratorJson.text(content, "setup"),
                resolution = GeneratorJson.text(content, "resolution"),
                creatures = creatures,
                intendedDifficulty = GeneratorJson.enum(
                    request.difficulty.ifBlank { null },
                    EncounterDifficulty.MEDIUM,
                ),
                locationId = locationId,
            ),
            aiGenerated = true,
        )

        return listOf(CreatedEntity("encounter", encounter.id, encounter.title))
    }
}






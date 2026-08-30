package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.CreatedEntity
import com.dndsaas.dto.TypedGenerationRequest
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service

/**
 * Generates a rollable table of quick encounters for travel and downtime.
 *
 * Not persisted — this is a table the Dungeon Master keeps to hand, not a set
 * of prepared encounters. Anything worth keeping can be regenerated properly
 * with the encounter generator.
 */
@Service
class RandomEncounterGenerator : Generator {

    override val type = GenerationType.RANDOM_ENCOUNTER

    override val persists = false

    override val description =
        "A d8 table of quick encounters for travel or downtime, fitted to a region. " +
            "Returned only — not saved."

    override val requiredFields = listOf("encounters")

    override val jsonSchema = """
        {
          "region": "where this table applies",
          "howToUse": "when to roll, e.g. once per watch while travelling",
          "encounters": [
            {
              "roll": "1",
              "title": "a short name",
              "description": "what happens, in two or three sentences",
              "type": "COMBAT, SOCIAL, EXPLORATION, HAZARD or DISCOVERY",
              "creatures": "SRD 5.1 creatures involved, or empty for non-combat",
              "twist": "the detail that makes it memorable"
            }
          ]
        }
    """.trimIndent()

    override val guidance = """
        Produce exactly 8 encounters, numbered 1 to 8.

        - Vary the type. At most three should be combat; the rest social,
          exploration, hazard or discovery.
        - Every entry must belong to this specific campaign — reference its
          factions, its weather, its politics, its history. A generic wandering
          monster table is a failure.
        - Any creatures must come from the D&D 5e SRD 5.1.
        - At least one entry should quietly foreshadow an active quest or a
          villain's plan.
        - Keep each one short enough to run in ten minutes.
    """.trimIndent()

    override fun persist(
        campaign: Campaign,
        content: JsonNode,
        request: TypedGenerationRequest,
    ): List<CreatedEntity> = emptyList()
}


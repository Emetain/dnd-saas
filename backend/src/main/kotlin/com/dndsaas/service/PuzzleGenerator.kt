package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.CreatedEntity
import com.dndsaas.dto.TypedGenerationRequest
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service

/**
 * Generates a puzzle with a solution, graded hints and a fallback.
 *
 * Deliberately does not persist: a puzzle belongs to the scene it is used in,
 * so the Dungeon Master pastes it into their session notes rather than having
 * it clutter Campaign Memory.
 */
@Service
class PuzzleGenerator : Generator {

    override val type = GenerationType.PUZZLE

    override val persists = false

    override val description =
        "A puzzle with a clear solution, graded hints and a way to move on if the " +
            "party gets stuck. Returned only — not saved."

    override val requiredFields = listOf("name", "solution")

    override val jsonSchema = """
        {
          "name": "a short name for the puzzle",
          "presentation": "read-aloud text describing exactly what the players see",
          "mechanism": "how the puzzle works, for the Dungeon Master only",
          "solution": "the intended answer, stated plainly",
          "hints": [
            "hint 1: a nudge",
            "hint 2: a stronger nudge",
            "hint 3: nearly the answer"
          ],
          "alternativeSolutions": ["other approaches you should reward"],
          "failureConsequence": "what happens on a wrong answer — ideally not damage",
          "bypass": "how the party moves on if they are stuck, so the session never stalls",
          "skillChecks": ["e.g. Intelligence (Investigation) DC 14 to notice the worn tiles"]
        }
    """.trimIndent()

    override val guidance = """
        Design a puzzle that can actually be solved at a table.

        - The players solve it, not their characters' modifiers. Skill checks may
          reveal information, never the answer.
        - Everything needed must be in the presentation text. No outside knowledge,
          no wordplay that only works in English if the table is playing otherwise.
        - Reward lateral thinking: list alternative solutions you would accept.
        - Always provide a bypass. A puzzle that stops the session is a bad puzzle.
        - Tie the imagery to the campaign's world and its factions or history.
    """.trimIndent()

    override fun persist(
        campaign: Campaign,
        content: JsonNode,
        request: TypedGenerationRequest,
    ): List<CreatedEntity> = emptyList()
}


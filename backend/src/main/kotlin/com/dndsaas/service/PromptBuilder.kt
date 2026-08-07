package com.dndsaas.service

import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.LlmMessage
import com.dndsaas.dto.LlmRequest
import com.dndsaas.dto.LlmRole
import org.springframework.stereotype.Service

/**
 * Step 2 of the AI pipeline: build an optimised prompt.
 *
 * Combines three things into a chat request:
 *  1. a system message defining the assistant's role and hard rules,
 *  2. the campaign context collected in step 1,
 *  3. the Dungeon Master's own instruction.
 *
 * Generators supply a JSON schema so the model returns structured data that can
 * be stored as real entities rather than a wall of prose.
 */
@Service
class PromptBuilder {

    private val baseSystemPrompt = """
        You are an expert Dungeon Master's assistant for tabletop roleplaying games.

        You are given the current state of a live campaign. Your single most
        important rule is CONSISTENCY: everything you produce must fit the world
        described in the context.

        Rules:
        - Reuse existing locations, factions and NPCs instead of inventing parallel ones.
        - Never contradict established lore, history or the current political situation.
        - Never reuse the name of an existing NPC, location or item for something new.
        - Respect the tone and genre implied by the campaign premise.
        - Where it strengthens the story, tie new content to existing quests,
          NPCs or the player characters' backstories.
        - Prefer specific, evocative detail over generic fantasy filler.
        - Content is for the Dungeon Master, so secrets and villain plans are welcome.

        Reply with a single valid JSON object and nothing else. No markdown, no
        code fences, no commentary before or after the JSON.
    """.trimIndent()

    /**
     * Assembles the final request sent to the model.
     *
     * @param context campaign context rendered by [AiContextCollector]
     * @param instruction what the Dungeon Master asked for
     * @param jsonSchema the shape the reply must take, supplied by a generator
     */
    fun build(
        type: GenerationType,
        context: String,
        instruction: String,
        jsonSchema: String? = null,
        temperature: Double = 0.8,
        extraGuidance: String? = null,
    ): LlmRequest {
        val system = buildString {
            append(baseSystemPrompt)
            appendLine()
            appendLine()
            appendLine("You are currently generating: ${type.label}.")
            if (extraGuidance != null) {
                appendLine()
                appendLine(extraGuidance)
            }
            if (jsonSchema != null) {
                appendLine()
                appendLine("Return JSON in exactly this shape:")
                appendLine(jsonSchema)
            }
        }

        val user = buildString {
            if (context.isBlank()) {
                appendLine("# NOTE")
                appendLine("This is a brand new campaign — there is no stored world yet.")
            } else {
                appendLine("# CURRENT CAMPAIGN STATE")
                appendLine(context)
            }
            appendLine()
            appendLine("# REQUEST")
            appendLine(
                instruction.ifBlank {
                    "Generate a ${type.label} that fits naturally into this campaign."
                },
            )
        }

        return LlmRequest(
            messages = listOf(
                LlmMessage(LlmRole.SYSTEM, system),
                LlmMessage(LlmRole.USER, user),
            ),
            temperature = temperature,
            jsonMode = true,
        )
    }
}


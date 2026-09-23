package com.dndsaas.dto

import com.dndsaas.domain.GenerationType
import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant

/**
 * A request to one of the generator modules.
 *
 * Campaign context is never sent by the client — it is collected automatically
 * by the AI pipeline from Campaign Memory.
 */
data class TypedGenerationRequest(
    /** What the Dungeon Master asked for, e.g. "a smuggler who owes the party money". */
    val instruction: String = "",
    /** Higher values produce more surprising results. */
    val temperature: Double = 0.8,
    /** Run against the mock client to test without spending tokens. */
    val mock: Boolean = false,
    /**
     * Generate and return the result WITHOUT writing it into Campaign Memory.
     *
     * The result is still logged, so it can be saved later with
     * `POST /generate/commit/{logId}` — letting a Dungeon Master regenerate
     * freely without polluting their world.
     */
    val preview: Boolean = false,
    /**
     * The existing object a generator should act on, when it needs one.
     * Used by the backstory generator to identify the player character.
     */
    val targetId: Long? = null,
    /** Optional steer for encounter difficulty: EASY, MEDIUM, HARD or DEADLY. */
    val difficulty: String = "",
)

/** Something a generator wrote into Campaign Memory. */
data class CreatedEntity(
    /** The kind of object, e.g. "npc", "quest", "item". */
    val kind: String,
    val id: Long,
    val name: String,
)

/** The result of running a generator. */
data class TypedGenerationResponse(
    val logId: Long,
    val type: GenerationType,
    val campaignId: Long,
    /** The validated, structured content the model produced. */
    val content: JsonNode,
    /** False when this was a preview, or when the generator does not persist. */
    val saved: Boolean,
    /** What was written into Campaign Memory. Empty for previews. */
    val created: List<CreatedEntity>,
    val usage: TokenUsage,
    val model: String,
    val mocked: Boolean,
    val durationMillis: Long,
    val createdAt: Instant,
)

/** A generator module that is available to this campaign. */
data class GeneratorInfo(
    val type: GenerationType,
    val label: String,
    /** Platform tokens this generation costs (enforced from Phase 5). */
    val tokenCost: Int,
    /** Whether the result is written into Campaign Memory. */
    val persists: Boolean,
    val description: String,
    val availableOnFree: Boolean,
)


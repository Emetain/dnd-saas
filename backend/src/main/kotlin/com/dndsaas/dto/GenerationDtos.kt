package com.dndsaas.dto

import com.dndsaas.domain.GenerationType
import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant

/**
 * What the Dungeon Master asks the AI for.
 * Campaign context is added automatically — it is never sent by the client.
 */
data class GenerationRequest(
    /** What to generate. Defaults to a free-form request. */
    val type: GenerationType = GenerationType.FREEFORM,
    /** The Dungeon Master's instruction, e.g. "an innkeeper who secretly spies for the cult". */
    val instruction: String = "",
    /** Higher values produce more surprising results. */
    val temperature: Double = 0.8,
    /** Run against the mock client to test without spending tokens. */
    val mock: Boolean = false,
)

/** The result of one trip through the AI pipeline. */
data class GenerationResponse(
    val logId: Long,
    val type: GenerationType,
    /** Null for generations made before a campaign exists (interview, blueprint). */
    val campaignId: Long?,
    /** The validated, structured content produced by the model. */
    val content: JsonNode,
    val usage: TokenUsage,
    val model: String,
    val mocked: Boolean,
    val durationMillis: Long,
    /** How much campaign context was injected — useful while tuning prompts. */
    val contextCharacters: Int,
    val createdAt: Instant,
)

/** A past generation, for the campaign's AI history. */
data class GenerationLogResponse(
    val id: Long,
    val campaignId: Long?,
    val type: GenerationType,
    val instruction: String,
    val result: String,
    val successful: Boolean,
    val errorMessage: String,
    val model: String,
    val usage: TokenUsage,
    val tokenCost: Int,
    val mocked: Boolean,
    val durationMillis: Long,
    val createdAt: Instant,
)

/** Aggregate AI usage for a campaign. */
data class GenerationUsageStats(
    val campaignId: Long,
    val generations: Long,
    val providerTokensUsed: Long,
    val platformTokensCharged: Long,
)

/**
 * The exact prompt that would be sent for a request, without calling the model.
 * Invaluable for verifying that Campaign Memory really reaches the AI.
 */
data class PromptPreview(
    val type: GenerationType,
    val campaignId: Long,
    val systemPrompt: String,
    val userPrompt: String,
    val contextCharacters: Int,
    val estimatedPromptTokens: Int,
)


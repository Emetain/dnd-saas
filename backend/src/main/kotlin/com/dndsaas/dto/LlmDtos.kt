package com.dndsaas.dto

/** Who is speaking in a chat completion. */
enum class LlmRole {
    SYSTEM,
    USER,
    ASSISTANT,
    ;

    /** The wire value expected by the OpenAI API. */
    fun wireValue(): String = name.lowercase()
}

/** A single message in a chat completion. */
data class LlmMessage(
    val role: LlmRole,
    val content: String,
)

/**
 * A provider-agnostic request to a language model.
 * Built by the prompt builder, consumed by an [com.dndsaas.service.LlmClient].
 */
data class LlmRequest(
    val messages: List<LlmMessage>,
    /** Higher = more creative. 0.8 suits creative worldbuilding. */
    val temperature: Double = 0.8,
    /** Ask the model to reply with strict JSON, so it can be parsed into entities. */
    val jsonMode: Boolean = true,
    val maxTokens: Int? = null,
    /** Overrides the configured default model when set. */
    val model: String? = null,
)

/** How many tokens a call consumed — the basis for Phase 5 billing. */
data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
)

/** A provider-agnostic response from a language model. */
data class LlmResult(
    val content: String,
    val usage: TokenUsage = TokenUsage(),
    val model: String = "",
    /** True when produced by the mock client rather than a real provider. */
    val mocked: Boolean = false,
)


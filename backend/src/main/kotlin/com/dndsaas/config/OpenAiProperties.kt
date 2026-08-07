package com.dndsaas.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * OpenAI settings, bound from the `app.openai.*` section of application.yml.
 *
 * The API key itself is never written in a config file — it comes from the
 * OPENAI_API_KEY environment variable, so it can never be committed.
 */
@ConfigurationProperties(prefix = "app.openai")
data class OpenAiProperties(
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-4o-mini",
    val timeoutSeconds: Long = 60,
) {
    /** True when a key has actually been supplied. */
    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    /**
     * A safe-to-log version of the key: only the prefix and last four characters.
     * Never log [apiKey] itself.
     */
    fun maskedKey(): String = when {
        apiKey.isBlank() -> "<not set>"
        apiKey.length < 12 -> "<set, but suspiciously short (${apiKey.length} chars)>"
        else -> "${apiKey.take(7)}…${apiKey.takeLast(4)} (${apiKey.length} chars)"
    }
}


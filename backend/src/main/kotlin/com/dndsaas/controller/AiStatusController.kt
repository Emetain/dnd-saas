package com.dndsaas.controller

import com.dndsaas.config.OpenAiProperties
import com.dndsaas.service.AiService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Diagnostics: confirms whether the AI provider is configured.
 *
 * Reads configuration only — it never calls OpenAI and never exposes the real
 * key, only a masked form.
 */
@RestController
@RequestMapping("/v1/ai")
@Tag(name = "AI", description = "AI provider status")
class AiStatusController(
    private val properties: OpenAiProperties,
    private val aiService: AiService,
) {

    @GetMapping("/status")
    @Operation(summary = "Check whether the OpenAI API key was picked up from the environment")
    fun status(): Map<String, Any> = mapOf(
        "configured" to properties.isConfigured,
        "activeProvider" to aiService.activeProvider(),
        "apiKey" to properties.maskedKey(),
        "model" to properties.model,
        "baseUrl" to properties.baseUrl,
        "timeoutSeconds" to properties.timeoutSeconds,
    )
}

package com.dndsaas.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Logs, at startup, whether the OpenAI key was picked up from the environment.
 *
 * Only a masked form of the key is ever printed, so the log can be shared or
 * screenshotted safely.
 */
@Configuration
@EnableConfigurationProperties(OpenAiProperties::class)
class OpenAiStartupCheck {

    private val log = LoggerFactory.getLogger(OpenAiStartupCheck::class.java)

    @Bean
    fun openAiKeyCheck(properties: OpenAiProperties) = ApplicationRunner {
        if (properties.isConfigured) {
            log.info("✅ OpenAI key detected: {}", properties.maskedKey())
            log.info("   model={} baseUrl={}", properties.model, properties.baseUrl)
        } else {
            log.warn("⚠️  OPENAI_API_KEY is NOT set — AI generation will be disabled.")
            log.warn("   Set it in the IntelliJ run configuration (Environment variables)")
            log.warn("   or export it in your shell, then restart the application.")
        }
    }
}


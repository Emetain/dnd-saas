package com.dndsaas.config

import com.dndsaas.service.LlmClient
import com.dndsaas.service.MockLlmClient
import com.dndsaas.service.OpenAiClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * Chooses which [LlmClient] the application runs with.
 *
 * A real OpenAI client when a key is configured, otherwise the mock — so the
 * application always starts and the pipeline can be tested without a key.
 */
@Configuration
class LlmClientConfig {

    private val log = LoggerFactory.getLogger(LlmClientConfig::class.java)

    /** Always available, so callers can explicitly ask for a token-free run. */
    @Bean
    fun mockLlmClient(): MockLlmClient = MockLlmClient()

    /**
     * The client used by default.
     *
     * Marked [Primary] because [MockLlmClient] also implements [LlmClient]:
     * without it, injecting an `LlmClient` would be ambiguous and startup would
     * fail as soon as a parameter were renamed.
     */
    @Bean
    @Primary
    fun llmClient(properties: OpenAiProperties, mockLlmClient: MockLlmClient): LlmClient =
        if (properties.isConfigured) {
            log.info("LLM provider: OpenAI (model={})", properties.model)
            OpenAiClient(properties, openAiRestClient(properties))
        } else {
            log.warn("LLM provider: Mock — set OPENAI_API_KEY to enable real generation")
            mockLlmClient
        }

    private fun openAiRestClient(properties: OpenAiProperties): RestClient {
        val timeout = Duration.ofSeconds(properties.timeoutSeconds)
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(timeout)
            setReadTimeout(timeout)
        }
        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .defaultHeader("Authorization", "Bearer ${properties.apiKey}")
            .requestFactory(factory)
            .build()
    }
}


package com.dndsaas.service

import com.dndsaas.config.OpenAiProperties
import com.dndsaas.dto.LlmRequest
import com.dndsaas.dto.LlmResult
import com.dndsaas.dto.TokenUsage
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

/**
 * Step 3 of the AI pipeline, backed by the OpenAI Chat Completions API.
 *
 * Instantiated by [com.dndsaas.config.LlmClientConfig] only when an API key is
 * present; otherwise the mock client is used instead.
 */
class OpenAiClient(
    private val properties: OpenAiProperties,
    private val restClient: RestClient,
) : LlmClient {

    private val log = LoggerFactory.getLogger(OpenAiClient::class.java)

    override val providerName: String = "OpenAI"

    override fun complete(request: LlmRequest): LlmResult {
        val model = request.model ?: properties.model
        val body = ChatCompletionRequest(
            model = model,
            messages = request.messages.map { ChatMessage(it.role.wireValue(), it.content) },
            temperature = request.temperature,
            maxTokens = request.maxTokens,
            responseFormat = if (request.jsonMode) ResponseFormat("json_object") else null,
        )

        log.debug("Calling OpenAI model={} messages={}", model, request.messages.size)

        val response = try {
            restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ChatCompletionResponse::class.java)
        } catch (ex: RestClientException) {
            throw LlmException("OpenAI request failed: ${ex.message}", ex)
        } ?: throw LlmException("OpenAI returned an empty response")

        val content = response.choices.firstOrNull()?.message?.content
            ?: throw LlmException("OpenAI response contained no choices")

        val usage = response.usage?.let {
            TokenUsage(it.promptTokens, it.completionTokens, it.totalTokens)
        } ?: TokenUsage()

        log.info(
            "OpenAI call complete: model={} promptTokens={} completionTokens={} totalTokens={}",
            response.model, usage.promptTokens, usage.completionTokens, usage.totalTokens,
        )

        return LlmResult(
            content = content,
            usage = usage,
            model = response.model.ifBlank { model },
            mocked = false,
        )
    }
}

// --- Wire format for the OpenAI Chat Completions API ---

@JsonInclude(JsonInclude.Include.NON_NULL)
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double,
    @JsonProperty("max_tokens") val maxTokens: Int?,
    @JsonProperty("response_format") val responseFormat: ResponseFormat?,
)

private data class ResponseFormat(val type: String)

private data class ChatMessage(val role: String, val content: String)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class ChatCompletionResponse(
    val model: String = "",
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class Choice(val message: ChatMessage? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class Usage(
    @JsonProperty("prompt_tokens") val promptTokens: Int = 0,
    @JsonProperty("completion_tokens") val completionTokens: Int = 0,
    @JsonProperty("total_tokens") val totalTokens: Int = 0,
)


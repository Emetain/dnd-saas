package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.GenerationType
import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.domain.User
import com.dndsaas.dto.GenerationRequest
import com.dndsaas.dto.GenerationResponse
import com.dndsaas.dto.LlmRole
import com.dndsaas.dto.PromptPreview
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * THE CENTRAL AI SERVICE.
 *
 * Every AI request in the platform goes through here, and it always performs
 * the same five steps:
 *
 *   1. collect campaign context      -> [AiContextCollector]
 *   2. build an optimised prompt     -> [PromptBuilder]
 *   3. call the language model       -> [LlmClient]
 *   4. validate the response         -> [AiResponseValidator]
 *   5. store the structured result   -> [GenerationLogService]
 *   6. charge the user's tokens      -> [TokenService]
 *
 * Generators added in Phase 4 do not talk to OpenAI themselves; they describe
 * what they want and call [generate], so context injection, validation, logging
 * and billing stay in one place.
 */
@Service
class AiService(
    private val contextCollector: AiContextCollector,
    private val promptBuilder: PromptBuilder,
    private val llmClient: LlmClient,
    private val mockLlmClient: MockLlmClient,
    private val validator: AiResponseValidator,
    private val generationLogService: GenerationLogService,
    private val tokenService: TokenService,
) {

    private val log = LoggerFactory.getLogger(AiService::class.java)

    /**
     * Runs the full pipeline.
     *
     * @param campaign the campaign to generate for; its memory becomes the
     *   context. May be null for generations that happen *before* a campaign
     *   exists — the campaign interview and the initial blueprint — in which
     *   case step 1 is skipped and only the instruction is sent.
     * @param request what the Dungeon Master asked for
     * @param jsonSchema optional shape the reply must take, supplied by a generator
     * @param requiredFields fields the reply must contain to be accepted
     * @param systemGuidance extra rules a generator wants to impose
     * @param user who pays for the generation; defaults to the campaign's owner
     * @param tokenCost platform tokens charged on success; defaults to the type's cost
     */
    fun generate(
        campaign: Campaign?,
        request: GenerationRequest,
        jsonSchema: String? = null,
        requiredFields: List<String> = emptyList(),
        systemGuidance: String? = null,
        user: User? = campaign?.user,
        tokenCost: Int = request.type.tokenCost,
    ): GenerationResponse {
        val campaignId = campaign?.id
        val startedAt = System.currentTimeMillis()
        var contextCharacters = 0

        // --- 0. Refuse up front if the tier or balance does not allow it, before spending an LLM call ---
        val payerId = checkNotNull(user?.id) { "A generation must be paid for by a user" }
        if (user!!.subscriptionTier == SubscriptionTier.FREE && !request.type.availableOnFree) {
            throw TierRestrictionException("The ${request.type.label} generator requires Pro or higher")
        }
        tokenService.requireBalance(payerId, tokenCost.toLong())

        try {
            // --- 1. Collect campaign context (skipped when there is no campaign yet) ---
            val context = if (campaignId != null) {
                contextCollector.collect(campaignId, request.type)
            } else {
                ""
            }
            contextCharacters = context.length
            log.debug("Collected {} characters of context for campaign {}", contextCharacters, campaignId)

            // --- 2. Build an optimised prompt ---
            val llmRequest = promptBuilder.build(
                type = request.type,
                context = context,
                instruction = request.instruction,
                jsonSchema = jsonSchema,
                temperature = request.temperature,
                extraGuidance = systemGuidance,
            )

            // --- 3. Call the model ---
            val client = clientFor(request)
            log.info(
                "Generating {} for campaign {} via {}",
                request.type.label, campaignId, client.providerName,
            )
            val result = client.complete(llmRequest)

            // --- 4. Validate the response ---
            val content: JsonNode = if (requiredFields.isEmpty()) {
                validator.parse(result.content)
            } else {
                validator.parseAndRequire(result.content, requiredFields)
            }

            // --- 5. Store the result ---
            val duration = System.currentTimeMillis() - startedAt
            val logEntry = generationLogService.recordSuccess(
                campaign = campaign,
                type = request.type,
                instruction = request.instruction,
                result = result.content,
                usage = result.usage,
                model = result.model,
                mocked = result.mocked,
                durationMillis = duration,
                contextCharacters = contextCharacters,
            )

            // --- 6. Charge only for generations that succeeded ---
            if (tokenCost > 0) {
                val charged = tokenService.chargeTokens(
                    userId = payerId,
                    amount = tokenCost.toLong(),
                    generationLogId = logEntry.id,
                    description = "${request.type.label} generation",
                    campaignId = campaignId,
                )
                if (!charged) {
                    throw InsufficientTokensException(tokenCost.toLong(), tokenService.getBalance(payerId))
                }
            }

            return GenerationResponse(
                logId = logEntry.id!!,
                type = request.type,
                campaignId = campaignId,
                content = content,
                usage = result.usage,
                model = result.model,
                mocked = result.mocked,
                durationMillis = duration,
                contextCharacters = contextCharacters,
                createdAt = logEntry.createdAt,
            )
        } catch (ex: Exception) {
            val duration = System.currentTimeMillis() - startedAt
            log.error("Generation failed for campaign {}: {}", campaignId, ex.message, ex)
            // Recording the failure must never replace the original exception —
            // otherwise the real cause is lost (e.g. when the log table itself
            // is what rejected the write).
            runCatching {
                generationLogService.recordFailure(
                    campaign = campaign,
                    type = request.type,
                    instruction = request.instruction,
                    errorMessage = ex.message ?: ex::class.simpleName.orEmpty(),
                    durationMillis = duration,
                    contextCharacters = contextCharacters,
                )
            }.onFailure { logFailure ->
                log.error("Could not even record the failed generation: {}", logFailure.message)
            }
            throw ex
        }
    }

    /**
     * Builds the prompt without calling the model.
     *
     * Lets you see exactly which parts of Campaign Memory reach the AI — the
     * cheapest way to verify context injection and to tune prompts.
     */
    fun preview(
        campaignId: Long,
        type: GenerationType,
        instruction: String,
        jsonSchema: String? = null,
    ): PromptPreview {
        val context = contextCollector.collect(campaignId, type)
        val llmRequest = promptBuilder.build(type, context, instruction, jsonSchema)
        val system = llmRequest.messages.first { it.role == LlmRole.SYSTEM }.content
        val user = llmRequest.messages.first { it.role == LlmRole.USER }.content
        return PromptPreview(
            type = type,
            campaignId = campaignId,
            systemPrompt = system,
            userPrompt = user,
            contextCharacters = context.length,
            // Rough industry estimate of ~4 characters per token.
            estimatedPromptTokens = (system.length + user.length) / 4,
        )
    }

    /** The mock client can be requested per call to avoid spending tokens. */
    private fun clientFor(request: GenerationRequest): LlmClient =
        if (request.mock) mockLlmClient else llmClient

    /** Which provider is currently active. */
    fun activeProvider(): String = llmClient.providerName
}


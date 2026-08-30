package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.GenerationLog
import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.CreatedEntity
import com.dndsaas.dto.GenerationRequest
import com.dndsaas.dto.GeneratorInfo
import com.dndsaas.dto.TypedGenerationRequest
import com.dndsaas.dto.TypedGenerationResponse
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Runs any registered [Generator] through the central AI pipeline.
 *
 * This is the glue between the generic [AiService] and the individual
 * generator modules: it looks up the right module for a [GenerationType],
 * merges its schema and guidance into the request, and — unless the Dungeon
 * Master only asked for a preview — persists the result into Campaign Memory.
 *
 * Preview/commit needs no extra storage: [GenerationLog.result] already holds
 * the model's raw JSON, so committing later simply re-parses that same log.
 */
@Service
class GeneratorService(
    private val registry: GeneratorRegistry,
    private val aiService: AiService,
    private val campaignService: CampaignService,
    private val generationLogService: GenerationLogService,
    private val validator: AiResponseValidator,
) {

    private val log = LoggerFactory.getLogger(GeneratorService::class.java)

    fun catalogue(): List<GeneratorInfo> = registry.catalogue()

    @Transactional
    fun generate(campaignId: Long, type: GenerationType, request: TypedGenerationRequest): TypedGenerationResponse {
        val campaign = campaignService.findEntity(campaignId)
        val generator = registry.forType(type)

        val instruction = buildString {
            append(request.instruction.ifBlank { "Generate a ${type.label} that fits this campaign." })
            append(generator.extraInstruction(campaign, request))
        }

        val response = aiService.generate(
            campaign = campaign,
            request = GenerationRequest(
                type = type,
                instruction = instruction,
                temperature = request.temperature,
                mock = request.mock,
            ),
            jsonSchema = generator.jsonSchema,
            requiredFields = generator.requiredFields,
            systemGuidance = generator.guidance,
        )

        val shouldPersist = generator.persists && !request.preview
        val created = if (shouldPersist) {
            runCatching { generator.persist(campaign, response.content, request) }
                .onFailure { log.error("Failed to persist {} generation: {}", type, it.message, it) }
                .getOrElse { emptyList() }
        } else {
            emptyList()
        }

        return TypedGenerationResponse(
            logId = response.logId,
            type = type,
            campaignId = campaignId,
            content = response.content,
            saved = created.isNotEmpty(),
            created = created,
            usage = response.usage,
            model = response.model,
            mocked = response.mocked,
            durationMillis = response.durationMillis,
            createdAt = response.createdAt,
        )
    }

    /**
     * Commits a previously previewed generation: re-reads the logged JSON and
     * runs the same generator's [Generator.persist], without calling the model
     * again.
     */
    @Transactional
    fun commit(campaignId: Long, logId: Long, request: TypedGenerationRequest): TypedGenerationResponse {
        val campaign = campaignService.findEntity(campaignId)
        val logEntry = generationLogService.findEntity(logId)
        require(logEntry.campaign?.id == campaignId) {
            "Generation log $logId does not belong to campaign $campaignId"
        }
        require(logEntry.successful) { "Cannot commit a failed generation" }

        val generator = registry.forType(logEntry.type)
        val content: JsonNode = validator.parse(logEntry.result)
        val created = generator.persist(campaign, content, request)

        return TypedGenerationResponse(
            logId = logEntry.id!!,
            type = logEntry.type,
            campaignId = campaignId,
            content = content,
            saved = created.isNotEmpty(),
            created = created,
            usage = com.dndsaas.dto.TokenUsage(logEntry.promptTokens, logEntry.completionTokens, logEntry.totalTokens),
            model = logEntry.model,
            mocked = logEntry.mocked,
            durationMillis = logEntry.durationMillis,
            createdAt = logEntry.createdAt,
        )
    }
}


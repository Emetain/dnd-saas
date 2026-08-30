package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.GenerationLog
import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.GenerationLogResponse
import com.dndsaas.dto.GenerationUsageStats
import com.dndsaas.dto.TokenUsage
import com.dndsaas.repository.GenerationLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Step 5 of the AI pipeline: store the result.
 *
 * Persists a record of every generation — successful or not — giving Dungeon
 * Masters a history of what they created and providing the usage data Phase 5
 * will bill against.
 */
@Service
@Transactional
class GenerationLogService(
    private val generationLogRepository: GenerationLogRepository,
) {

    fun findEntity(id: Long): GenerationLog =
        generationLogRepository.findById(id)
            .orElseThrow { NoSuchElementException("Generation log $id not found") }

    fun recordSuccess(
        campaign: Campaign?,
        type: GenerationType,
        instruction: String,
        result: String,
        usage: TokenUsage,
        model: String,
        mocked: Boolean,
        durationMillis: Long,
        contextCharacters: Int,
    ): GenerationLog = generationLogRepository.save(
        GenerationLog(
            campaign = campaign,
            type = type,
            instruction = instruction,
            result = result,
            successful = true,
            model = model,
            promptTokens = usage.promptTokens,
            completionTokens = usage.completionTokens,
            totalTokens = usage.totalTokens,
            // Mock runs are free; real generations are charged from Phase 5.
            tokenCost = if (mocked) 0 else type.tokenCost,
            mocked = mocked,
            durationMillis = durationMillis,
            contextCharacters = contextCharacters,
        ),
    )

    /**
     * Runs in its own transaction so the failure is still recorded even when the
     * caller's transaction rolls back — which is exactly what happens when a
     * generator fails part-way through persisting its result.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordFailure(
        campaign: Campaign?,
        type: GenerationType,
        instruction: String,
        errorMessage: String,
        durationMillis: Long,
        contextCharacters: Int,
    ): GenerationLog = generationLogRepository.save(
        GenerationLog(
            campaign = campaign,
            type = type,
            instruction = instruction,
            successful = false,
            errorMessage = errorMessage,
            // Failed generations are never charged.
            tokenCost = 0,
            durationMillis = durationMillis,
            contextCharacters = contextCharacters,
        ),
    )

    @Transactional(readOnly = true)
    fun listForCampaign(campaignId: Long): List<GenerationLogResponse> =
        generationLogRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId).map(::toResponse)

    @Transactional(readOnly = true)
    fun usageStats(campaignId: Long): GenerationUsageStats = GenerationUsageStats(
        campaignId = campaignId,
        generations = generationLogRepository.countByCampaignId(campaignId),
        providerTokensUsed = generationLogRepository.sumTotalTokensByCampaignId(campaignId) ?: 0,
        platformTokensCharged = generationLogRepository.sumTokenCostByCampaignId(campaignId) ?: 0,
    )

    fun deleteAllForCampaign(campaignId: Long) = generationLogRepository.deleteAll(
        generationLogRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId),
    )

    fun toResponse(log: GenerationLog): GenerationLogResponse = GenerationLogResponse(
        id = log.id!!,
        campaignId = log.campaign?.id,
        type = log.type,
        instruction = log.instruction,
        result = log.result,
        successful = log.successful,
        errorMessage = log.errorMessage,
        model = log.model,
        usage = TokenUsage(log.promptTokens, log.completionTokens, log.totalTokens),
        tokenCost = log.tokenCost,
        mocked = log.mocked,
        durationMillis = log.durationMillis,
        createdAt = log.createdAt,
    )
}


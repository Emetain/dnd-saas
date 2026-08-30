package com.dndsaas.orchestration

import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.GeneratorInfo
import com.dndsaas.dto.TypedGenerationRequest
import com.dndsaas.dto.TypedGenerationResponse
import com.dndsaas.service.GeneratorService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for the generator modules.
 *
 * Routes a task to [GeneratorService], which finds the right module and runs
 * it through the central AI pipeline. From Phase 5 this is also where the
 * user's token balance will be checked before generating.
 */
@Component
class GeneratorOrchestrator(
    private val generatorService: GeneratorService,
) {

    /** Task: which generators are available, for the frontend's generator panels. */
    fun catalogue(): List<GeneratorInfo> = generatorService.catalogue()

    /** Task: run a generator for a campaign. */
    fun generate(campaignId: Long, type: GenerationType, request: TypedGenerationRequest): TypedGenerationResponse =
        generatorService.generate(campaignId, type, request)

    /** Task: save a previously previewed generation without calling the model again. */
    fun commit(campaignId: Long, logId: Long, request: TypedGenerationRequest): TypedGenerationResponse =
        generatorService.commit(campaignId, logId, request)
}


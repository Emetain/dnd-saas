package com.dndsaas.service

import com.dndsaas.dto.LlmRequest
import com.dndsaas.dto.LlmResult

/**
 * Step 3 of the AI pipeline: call the language model.
 *
 * Deliberately provider-agnostic. The rest of the application depends on this
 * interface only, so swapping OpenAI for another provider — or for the mock
 * used in tests — never touches the generators or the pipeline.
 */
interface LlmClient {

    /** Sends a prompt to the model and returns its raw reply. */
    fun complete(request: LlmRequest): LlmResult

    /** Human-readable name of the backing provider, for logging. */
    val providerName: String
}

/** Raised when the model could not be reached or returned an error. */
class LlmException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)


package com.dndsaas.controller

import com.dndsaas.service.AiValidationException
import com.dndsaas.service.InsufficientTokensException
import com.dndsaas.service.TierRestrictionException
import com.dndsaas.service.LlmException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Translates exceptions into clean HTTP responses.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to ex.message))

    /** No endpoint matches the URL — without this the catch-all below turned it into a 500. */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleUnknownUrl(ex: NoResourceFoundException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "No endpoint for /${ex.resourcePath}"))

    /**
     * Validation failures raised by `require(...)` in the service layer,
     * e.g. linking objects that belong to different campaigns.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to ex.message))

    /** The request body is not valid JSON or does not match the DTO, e.g. an unknown enum value. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(ex: HttpMessageNotReadableException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to ex.mostSpecificCause.message))

    /** A required header is missing or malformed, e.g. no `X-User-Id`. */
    @ExceptionHandler(ServletRequestBindingException::class)
    fun handleMissingHeader(ex: ServletRequestBindingException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to ex.message))

    /** The user cannot afford the generation; nothing was sent to the AI. */
    @ExceptionHandler(InsufficientTokensException::class)
    fun handleInsufficientTokens(ex: InsufficientTokensException): ResponseEntity<Map<String, Any?>> =
        ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(
            mapOf("error" to ex.message, "required" to ex.required, "available" to ex.available),
        )

    /** The user's subscription tier does not include this feature. */
    @ExceptionHandler(TierRestrictionException::class)
    fun handleTierRestriction(ex: TierRestrictionException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to ex.message))

    /** The AI provider could not be reached or rejected the request. */
    @ExceptionHandler(LlmException::class)
    fun handleLlmFailure(ex: LlmException): ResponseEntity<Map<String, String?>> {
        log.error("LLM call failed", ex)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(mapOf("error" to ex.message, "hint" to "Check OPENAI_API_KEY, billing and connectivity."))
    }

    /** The model replied, but the reply could not be trusted. */
    @ExceptionHandler(AiValidationException::class)
    fun handleAiValidation(ex: AiValidationException): ResponseEntity<Map<String, String?>> {
        log.warn("AI response validation failed: {}", ex.message)
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(mapOf("error" to ex.message, "hint" to "Try again, or lower the temperature."))
    }

    /**
     * A database constraint rejected the write.
     *
     * The usual cause is an existing PostgreSQL database whose schema predates a
     * change, because `ddl-auto: update` never relaxes an existing constraint.
     */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException): ResponseEntity<Map<String, String?>> {
        val cause = ex.mostSpecificCause.message
        log.error("Database rejected the write: {}", cause, ex)
        val hint = if (cause?.contains("campaign_id", ignoreCase = true) == true) {
            "generation_logs.campaign_id must allow NULL. Run backend/migrations.sql: " +
                "ALTER TABLE generation_logs ALTER COLUMN campaign_id DROP NOT NULL;"
        } else {
            "The database schema may be out of date — see backend/migrations.sql."
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(mapOf("error" to cause, "hint" to hint))
    }

    /**
     * Last resort. Without this an unexpected failure returns a bare 500 with no
     * clue what happened; here the message and exception type are surfaced and
     * the full stack trace is logged.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<Map<String, String?>> {
        log.error("Unhandled exception", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf(
                "error" to (ex.message ?: "Unexpected error"),
                "exception" to ex::class.java.name,
                "cause" to ex.cause?.message,
            ),
        )
    }
}

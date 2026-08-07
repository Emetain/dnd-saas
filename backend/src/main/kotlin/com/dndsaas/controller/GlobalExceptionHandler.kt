package com.dndsaas.controller

import com.dndsaas.service.AiValidationException
import com.dndsaas.service.LlmException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Translates exceptions into clean HTTP responses.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to ex.message))

    /**
     * Validation failures raised by `require(...)` in the service layer,
     * e.g. linking objects that belong to different campaigns.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to ex.message))

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

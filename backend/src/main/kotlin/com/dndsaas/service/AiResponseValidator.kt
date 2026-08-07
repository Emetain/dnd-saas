package com.dndsaas.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Step 4 of the AI pipeline: validate the model's reply.
 *
 * Language models occasionally wrap JSON in prose or code fences even when told
 * not to. This service repairs the common cases, parses the result and checks
 * that the fields a generator depends on are actually present — so bad output
 * never reaches the database.
 */
@Service
class AiResponseValidator(
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(AiResponseValidator::class.java)

    /** Parses the reply into a JSON tree, repairing common formatting mistakes. */
    fun parse(raw: String): JsonNode {
        val cleaned = clean(raw)
        if (cleaned.isBlank()) {
            throw AiValidationException("The model returned an empty response")
        }
        return try {
            objectMapper.readTree(cleaned)
        } catch (ex: Exception) {
            log.warn("Model returned unparseable JSON: {}", cleaned.take(500))
            throw AiValidationException("The model did not return valid JSON: ${ex.message}", ex)
        }
    }

    /** Parses and checks that every required field is present and non-empty. */
    fun parseAndRequire(raw: String, requiredFields: List<String>): JsonNode {
        val node = parse(raw)
        val missing = requiredFields.filter { field ->
            val value = node.get(field)
            value == null || value.isNull || (value.isTextual && value.asText().isBlank())
        }
        if (missing.isNotEmpty()) {
            throw AiValidationException("The model's reply is missing required fields: $missing")
        }
        return node
    }

    /** Maps the reply straight onto a generator's result type. */
    fun <T> parseAs(raw: String, target: Class<T>): T {
        val node = parse(raw)
        return try {
            objectMapper.treeToValue(node, target)
        } catch (ex: Exception) {
            throw AiValidationException(
                "The model's reply did not match the expected ${target.simpleName} structure: ${ex.message}",
                ex,
            )
        }
    }

    /**
     * Strips markdown code fences and any prose surrounding the JSON object.
     */
    private fun clean(raw: String): String {
        var text = raw.trim()

        if (text.startsWith("```")) {
            text = text.removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        }

        // Keep only the outermost JSON object or array.
        val startObject = text.indexOf('{')
        val startArray = text.indexOf('[')
        val start = when {
            startObject == -1 -> startArray
            startArray == -1 -> startObject
            else -> minOf(startObject, startArray)
        }
        if (start > 0) {
            val end = maxOf(text.lastIndexOf('}'), text.lastIndexOf(']'))
            if (end > start) {
                text = text.substring(start, end + 1)
            }
        }

        return text
    }
}

/** Raised when the model's reply cannot be trusted. */
class AiValidationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)


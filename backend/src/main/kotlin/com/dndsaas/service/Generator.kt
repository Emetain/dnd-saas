package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.CreatedEntity
import com.dndsaas.dto.TypedGenerationRequest
import com.fasterxml.jackson.databind.JsonNode

/**
 * An independent generator module.
 *
 * Each implementation describes *what* it wants from the model — a JSON shape,
 * the rules to follow, the fields that must come back — and how to turn the
 * reply into structured Campaign Memory. None of them talk to OpenAI directly;
 * [AiService] runs the shared pipeline for all of them, so context injection,
 * validation, logging and billing stay in one place.
 *
 * Adding a new generator is a single class: implement this interface, annotate
 * it `@Service`, and [GeneratorRegistry] picks it up automatically.
 */
interface Generator {

    /** Which generation this module handles. Must be unique across generators. */
    val type: GenerationType

    /** The exact JSON shape the model must reply with. */
    val jsonSchema: String

    /** Extra rules appended to the system prompt for this generator. */
    val guidance: String

    /** Fields that must be present, or the reply is rejected. */
    val requiredFields: List<String>
        get() = emptyList()

    /** Whether results are written into Campaign Memory. */
    val persists: Boolean
        get() = true

    /** One line describing the module, surfaced by the catalogue endpoint. */
    val description: String

    /**
     * Extra, computed detail appended to the Dungeon Master's instruction.
     *
     * Lets a module inject facts the model cannot work out for itself — the
     * encounter generator uses it to state the party's exact XP budget.
     */
    fun extraInstruction(campaign: Campaign, request: TypedGenerationRequest): String = ""

    /**
     * Writes the model's reply into Campaign Memory.
     *
     * Only called for non-preview requests. Implementations should resolve
     * references to existing objects (an NPC named as a quest giver, a location
     * named as a shop's home) into real foreign keys, so the memory graph stays
     * connected rather than accumulating orphans.
     */
    fun persist(campaign: Campaign, content: JsonNode, request: TypedGenerationRequest): List<CreatedEntity>
}

/**
 * Helpers shared by generator implementations for reading loose model output.
 *
 * Models are not perfectly obedient: a field may be missing, null, or a string
 * where a number was asked for. These read defensively so a small deviation
 * never fails a whole generation.
 */
object GeneratorJson {

    fun text(node: JsonNode, vararg names: String): String {
        names.forEach { name ->
            val value = node.get(name)
            if (value != null && !value.isNull) {
                val text = if (value.isTextual) value.asText() else value.toString()
                if (text.isNotBlank()) return text.trim()
            }
        }
        return ""
    }

    fun int(node: JsonNode, name: String, default: Int = 0): Int {
        val value = node.get(name) ?: return default
        return when {
            value.isNumber -> value.asInt()
            value.isTextual -> value.asText().filter { it.isDigit() || it == '-' }.toIntOrNull() ?: default
            else -> default
        }
    }

    fun bool(node: JsonNode, name: String, default: Boolean = false): Boolean {
        val value = node.get(name) ?: return default
        return when {
            value.isBoolean -> value.asBoolean()
            value.isTextual -> value.asText().equals("true", ignoreCase = true)
            else -> default
        }
    }

    /** Returns the named array, or an empty list when absent or the wrong type. */
    fun array(node: JsonNode, name: String): List<JsonNode> {
        val value = node.get(name) ?: return emptyList()
        return if (value.isArray) value.toList() else listOf(value)
    }

    fun strings(node: JsonNode, name: String): List<String> =
        array(node, name).map { if (it.isTextual) it.asText() else it.toString() }
            .filter { it.isNotBlank() }

    /** Text field, or null when blank — for optional references. */
    fun textOrNull(node: JsonNode, vararg names: String): String? =
        text(node, *names).ifBlank { null }

    /** Parses an enum leniently, falling back when the model invents a value. */
    inline fun <reified E : Enum<E>> enum(raw: String?, fallback: E): E {
        if (raw.isNullOrBlank()) return fallback
        val normalised = raw.trim().uppercase().replace(' ', '_').replace('-', '_')
        return runCatching { enumValueOf<E>(normalised) }.getOrDefault(fallback)
    }
}



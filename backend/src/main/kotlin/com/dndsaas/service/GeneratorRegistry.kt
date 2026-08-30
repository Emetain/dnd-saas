package com.dndsaas.service

import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.GeneratorInfo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Finds the generator module responsible for a given [GenerationType].
 *
 * Spring injects every [Generator] bean, so modules register themselves simply
 * by existing. Nothing else in the application needs changing when one is added.
 */
@Service
class GeneratorRegistry(
    generators: List<Generator>,
) {

    private val log = LoggerFactory.getLogger(GeneratorRegistry::class.java)

    private val byType: Map<GenerationType, Generator> = generators.associateBy { it.type }

    init {
        val duplicates = generators.groupBy { it.type }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "More than one generator registered for: $duplicates"
        }
        log.info(
            "Registered {} generator modules: {}",
            byType.size,
            byType.keys.sortedBy { it.name }.joinToString { it.label },
        )
    }

    fun forType(type: GenerationType): Generator =
        byType[type] ?: throw NoSuchElementException(
            "No generator available for ${type.name}. Available: " +
                byType.keys.sortedBy { it.name }.joinToString { it.name },
        )

    fun has(type: GenerationType): Boolean = byType.containsKey(type)

    /** The catalogue of modules, for the frontend to render generator panels. */
    fun catalogue(): List<GeneratorInfo> =
        byType.values
            .map {
                GeneratorInfo(
                    type = it.type,
                    label = it.type.label,
                    tokenCost = it.type.tokenCost,
                    persists = it.persists,
                    description = it.description,
                )
            }
            .sortedBy { it.label }
}


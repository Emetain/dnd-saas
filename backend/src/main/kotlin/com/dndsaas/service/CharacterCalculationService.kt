package com.dndsaas.service

import com.dndsaas.domain.Character
import org.springframework.stereotype.Service

/**
 * Service layer — where "everything is calculated".
 * Pure D&D game-rule logic lives here, independent of web or persistence concerns.
 */
@Service
class CharacterCalculationService {

    /** D&D 5e ability modifier: floor((score - 10) / 2). */
    fun abilityModifier(score: Int): Int = Math.floorDiv(score - 10, 2)

    /** D&D 5e proficiency bonus scales with level. */
    fun proficiencyBonus(level: Int): Int = 2 + ((level - 1) / 4)

    fun abilityScores(character: Character): Map<String, Int> = mapOf(
        "strength" to character.strength,
        "dexterity" to character.dexterity,
        "constitution" to character.constitution,
        "intelligence" to character.intelligence,
        "wisdom" to character.wisdom,
        "charisma" to character.charisma,
    )

    fun abilityModifiers(character: Character): Map<String, Int> =
        abilityScores(character).mapValues { (_, score) -> abilityModifier(score) }
}


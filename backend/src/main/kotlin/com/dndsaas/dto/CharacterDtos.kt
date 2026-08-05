package com.dndsaas.dto

/**
 * Incoming request payload for creating a character.
 */
data class CreateCharacterRequest(
    val name: String,
    val characterClass: String,
    val level: Int,
    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val intelligence: Int,
    val wisdom: Int,
    val charisma: Int,
)

/**
 * Outgoing response, including values calculated by the service layer.
 */
data class CharacterResponse(
    val id: Long,
    val name: String,
    val characterClass: String,
    val level: Int,
    val abilityScores: Map<String, Int>,
    val abilityModifiers: Map<String, Int>,
    val proficiencyBonus: Int,
)


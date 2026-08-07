package com.dndsaas.dto

import java.time.Instant

/**
 * Incoming request payload for creating or updating a player character.
 * The campaign is taken from the URL path, not the body.
 */
data class CharacterRequest(
    val name: String,
    val playerName: String = "",
    val characterClass: String = "",
    val race: String = "",
    val background: String = "",
    val level: Int = 1,
    val strength: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val intelligence: Int = 10,
    val wisdom: Int = 10,
    val charisma: Int = 10,
    val maxHitPoints: Int = 0,
    val currentHitPoints: Int = 0,
    val armorClass: Int = 10,
    val backstory: String = "",
    val active: Boolean = true,
)

/**
 * Outgoing response, including values calculated by the service layer.
 */
data class CharacterResponse(
    val id: Long,
    val campaignId: Long?,
    val name: String,
    val playerName: String,
    val characterClass: String,
    val race: String,
    val background: String,
    val level: Int,
    val abilityScores: Map<String, Int>,
    val abilityModifiers: Map<String, Int>,
    val proficiencyBonus: Int,
    val savingThrows: Map<String, Int>,
    val initiative: Int,
    val passivePerception: Int,
    val maxHitPoints: Int,
    val currentHitPoints: Int,
    val armorClass: Int,
    val backstory: String,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

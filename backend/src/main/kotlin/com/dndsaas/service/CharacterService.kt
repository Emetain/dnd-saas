package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.Character
import com.dndsaas.dto.CharacterRequest
import com.dndsaas.dto.CharacterResponse
import com.dndsaas.repository.CharacterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Business logic for player characters: persistence, state changes and mapping
 * to DTOs. Pure D&D rule math is delegated to [CharacterCalculationService].
 */
@Service
@Transactional
class CharacterService(
    private val characterRepository: CharacterRepository,
    private val calculationService: CharacterCalculationService,
) {

    fun create(campaign: Campaign, request: CharacterRequest): CharacterResponse {
        val character = Character(campaign = campaign)
        apply(character, request)
        // Sensible default: start at full health if no current HP was given.
        if (request.currentHitPoints == 0) {
            character.currentHitPoints = request.maxHitPoints
        }
        return toResponse(characterRepository.save(character))
    }

    fun update(id: Long, request: CharacterRequest): CharacterResponse {
        val character = findEntity(id)
        apply(character, request)
        return toResponse(characterRepository.save(character))
    }

    @Transactional(readOnly = true)
    fun get(id: Long): CharacterResponse = toResponse(findEntity(id))

    @Transactional(readOnly = true)
    fun listByCampaign(campaignId: Long): List<CharacterResponse> =
        characterRepository.findByCampaignIdOrderByNameAsc(campaignId).map(::toResponse)

    @Transactional(readOnly = true)
    fun listAll(): List<CharacterResponse> = characterRepository.findAll().map(::toResponse)

    fun delete(id: Long) = characterRepository.deleteById(id)

    /** Sets a character's backstory directly — used by the backstory generator. */
    fun updateBackstory(id: Long, backstory: String): CharacterResponse {
        val character = findEntity(id)
        character.backstory = backstory
        return toResponse(characterRepository.save(character))
    }

    fun findEntity(id: Long): Character =
        characterRepository.findById(id)
            .orElseThrow { NoSuchElementException("Character $id not found") }

    private fun apply(character: Character, request: CharacterRequest) {
        character.name = request.name
        character.playerName = request.playerName
        character.characterClass = request.characterClass
        character.race = request.race
        character.background = request.background
        character.level = request.level
        character.strength = request.strength
        character.dexterity = request.dexterity
        character.constitution = request.constitution
        character.intelligence = request.intelligence
        character.wisdom = request.wisdom
        character.charisma = request.charisma
        character.maxHitPoints = request.maxHitPoints
        character.currentHitPoints = request.currentHitPoints
        character.armorClass = request.armorClass
        character.backstory = request.backstory
        character.active = request.active
    }

    /** Assembles a response, delegating all game-rule math to the calculation service. */
    fun toResponse(character: Character): CharacterResponse =
        CharacterResponse(
            id = character.id!!,
            campaignId = character.campaign?.id,
            name = character.name,
            playerName = character.playerName,
            characterClass = character.characterClass,
            race = character.race,
            background = character.background,
            level = character.level,
            abilityScores = calculationService.abilityScores(character),
            abilityModifiers = calculationService.abilityModifiers(character),
            proficiencyBonus = calculationService.proficiencyBonus(character.level),
            savingThrows = calculationService.savingThrows(character),
            initiative = calculationService.initiative(character),
            passivePerception = calculationService.passivePerception(character),
            maxHitPoints = character.maxHitPoints,
            currentHitPoints = character.currentHitPoints,
            armorClass = character.armorClass,
            backstory = character.backstory,
            active = character.active,
            createdAt = character.createdAt,
            updatedAt = character.updatedAt,
        )
}


package com.dndsaas.orchestration

import com.dndsaas.domain.Character
import com.dndsaas.dto.CharacterResponse
import com.dndsaas.dto.CreateCharacterRequest
import com.dndsaas.repository.CharacterRepository
import com.dndsaas.service.CharacterCalculationService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer.
 *
 * The controller hands a task to this layer. This layer decides *what* needs to
 * happen and coordinates the other layers — it delegates calculations to the
 * service layer and persistence to the repository — then assembles the result.
 *
 * It contains NO game-rule math itself; that belongs to the service layer.
 */
@Component
class CharacterOrchestrator(
    private val characterRepository: CharacterRepository,
    private val calculationService: CharacterCalculationService,
) {

    /** Task: create a character, persist it, then return calculated stats. */
    fun createCharacter(request: CreateCharacterRequest): CharacterResponse {
        val character = Character(
            name = request.name,
            characterClass = request.characterClass,
            level = request.level,
            strength = request.strength,
            dexterity = request.dexterity,
            constitution = request.constitution,
            intelligence = request.intelligence,
            wisdom = request.wisdom,
            charisma = request.charisma,
        )
        val saved = characterRepository.save(character)
        return toResponse(saved)
    }

    /** Task: fetch a character and return calculated stats. */
    fun getCharacter(id: Long): CharacterResponse {
        val character = characterRepository.findById(id)
            .orElseThrow { NoSuchElementException("Character $id not found") }
        return toResponse(character)
    }

    /** Task: list all characters with calculated stats. */
    fun listCharacters(): List<CharacterResponse> =
        characterRepository.findAll().map(::toResponse)

    /** Assembles a response, delegating all math to the service layer. */
    private fun toResponse(character: Character): CharacterResponse =
        CharacterResponse(
            id = character.id!!,
            name = character.name,
            characterClass = character.characterClass,
            level = character.level,
            abilityScores = calculationService.abilityScores(character),
            abilityModifiers = calculationService.abilityModifiers(character),
            proficiencyBonus = calculationService.proficiencyBonus(character.level),
        )
}


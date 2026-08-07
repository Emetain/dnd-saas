package com.dndsaas.orchestration

import com.dndsaas.dto.CharacterRequest
import com.dndsaas.dto.CharacterResponse
import com.dndsaas.service.CampaignService
import com.dndsaas.service.CharacterService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for player characters.
 *
 * The controller hands a task to this layer. This layer decides *what* needs to
 * happen and coordinates the services required — here it resolves the campaign
 * a character belongs to before delegating the work to the character service.
 *
 * It contains NO game-rule math and NO persistence code itself.
 */
@Component
class CharacterOrchestrator(
    private val characterService: CharacterService,
    private val campaignService: CampaignService,
) {

    /** Task: add a character to a campaign's party. */
    fun addCharacterToCampaign(campaignId: Long, request: CharacterRequest): CharacterResponse {
        val campaign = campaignService.findEntity(campaignId)
        return characterService.create(campaign, request)
    }

    /** Task: list the party roster of a campaign. */
    fun listCampaignCharacters(campaignId: Long): List<CharacterResponse> {
        // Ensures a 404 is returned for an unknown campaign instead of an empty list.
        campaignService.findEntity(campaignId)
        return characterService.listByCampaign(campaignId)
    }

    fun getCharacter(id: Long): CharacterResponse = characterService.get(id)

    fun updateCharacter(id: Long, request: CharacterRequest): CharacterResponse =
        characterService.update(id, request)

    fun deleteCharacter(id: Long) = characterService.delete(id)

    fun listCharacters(): List<CharacterResponse> = characterService.listAll()
}

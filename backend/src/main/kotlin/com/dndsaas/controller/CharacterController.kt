package com.dndsaas.controller

import com.dndsaas.dto.CharacterRequest
import com.dndsaas.dto.CharacterResponse
import com.dndsaas.orchestration.CharacterOrchestrator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST entry point for player characters. Receives HTTP requests and forwards
 * each task to the orchestration (middle) layer. Contains no business logic.
 *
 * Characters are created inside a campaign, so the create/list endpoints are
 * nested under /v1/campaigns/{campaignId}/characters.
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "Characters", description = "Player characters in a campaign and their calculated stats")
class CharacterController(
    private val orchestrator: CharacterOrchestrator,
) {

    @PostMapping("/campaigns/{campaignId}/characters")
    @Operation(summary = "Add a player character to a campaign")
    fun create(
        @PathVariable campaignId: Long,
        @RequestBody request: CharacterRequest,
    ): ResponseEntity<CharacterResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(orchestrator.addCharacterToCampaign(campaignId, request))

    @GetMapping("/campaigns/{campaignId}/characters")
    @Operation(summary = "List the party (all characters) of a campaign")
    fun listByCampaign(@PathVariable campaignId: Long): List<CharacterResponse> =
        orchestrator.listCampaignCharacters(campaignId)

    @GetMapping("/characters/{id}")
    @Operation(summary = "Get a character by id (with calculated modifiers, saves, initiative)")
    fun get(@PathVariable id: Long): CharacterResponse = orchestrator.getCharacter(id)

    @PutMapping("/characters/{id}")
    @Operation(summary = "Update a character")
    fun update(@PathVariable id: Long, @RequestBody request: CharacterRequest): CharacterResponse =
        orchestrator.updateCharacter(id, request)

    @DeleteMapping("/characters/{id}")
    @Operation(summary = "Delete a character")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        orchestrator.deleteCharacter(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/characters")
    @Operation(summary = "List all characters across campaigns")
    fun list(): List<CharacterResponse> = orchestrator.listCharacters()
}

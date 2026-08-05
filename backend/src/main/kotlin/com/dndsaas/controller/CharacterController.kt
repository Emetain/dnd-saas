package com.dndsaas.controller

import com.dndsaas.dto.CharacterResponse
import com.dndsaas.dto.CreateCharacterRequest
import com.dndsaas.orchestration.CharacterOrchestrator
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST entry point. Receives HTTP requests and forwards each task to the
 * orchestration (middle) layer. Contains no business logic.
 */
@RestController
@RequestMapping("/v1/characters")
class CharacterController(
    private val orchestrator: CharacterOrchestrator,
) {

    @PostMapping
    fun create(@RequestBody request: CreateCharacterRequest): ResponseEntity<CharacterResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.createCharacter(request))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): CharacterResponse = orchestrator.getCharacter(id)

    @GetMapping
    fun list(): List<CharacterResponse> = orchestrator.listCharacters()
}


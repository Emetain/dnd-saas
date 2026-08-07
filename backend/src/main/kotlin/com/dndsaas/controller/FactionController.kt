package com.dndsaas.controller

import com.dndsaas.dto.FactionRequest
import com.dndsaas.dto.FactionResponse
import com.dndsaas.dto.NpcResponse
import com.dndsaas.orchestration.FactionOrchestrator
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

/** REST entry point for factions. */
@RestController
@RequestMapping("/v1")
@Tag(name = "Factions", description = "Organisations shaping the campaign's politics")
class FactionController(
    private val orchestrator: FactionOrchestrator,
) {

    @PostMapping("/campaigns/{campaignId}/factions")
    @Operation(summary = "Add a faction to a campaign")
    fun create(
        @PathVariable campaignId: Long,
        @RequestBody request: FactionRequest,
    ): ResponseEntity<FactionResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.addFaction(campaignId, request))

    @GetMapping("/campaigns/{campaignId}/factions")
    @Operation(summary = "List all factions in a campaign")
    fun list(@PathVariable campaignId: Long): List<FactionResponse> =
        orchestrator.listFactions(campaignId)

    @GetMapping("/factions/{id}")
    @Operation(summary = "Get a faction by id")
    fun get(@PathVariable id: Long): FactionResponse = orchestrator.getFaction(id)

    @GetMapping("/factions/{id}/members")
    @Operation(summary = "List the NPCs belonging to this faction")
    fun members(@PathVariable id: Long): List<NpcResponse> = orchestrator.listFactionMembers(id)

    @PutMapping("/factions/{id}")
    @Operation(summary = "Update a faction")
    fun update(@PathVariable id: Long, @RequestBody request: FactionRequest): FactionResponse =
        orchestrator.updateFaction(id, request)

    @DeleteMapping("/factions/{id}")
    @Operation(summary = "Delete a faction")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        orchestrator.deleteFaction(id)
        return ResponseEntity.noContent().build()
    }
}


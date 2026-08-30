package com.dndsaas.controller

import com.dndsaas.dto.EncounterRequest
import com.dndsaas.dto.EncounterResponse
import com.dndsaas.orchestration.EncounterOrchestrator
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
 * REST entry point for encounters prepared by hand.
 *
 * To generate one with AI, use `POST /v1/campaigns/{id}/generate/ENCOUNTER`
 * instead — this controller is for direct CRUD, and for reading back what the
 * generator saved.
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "Encounters", description = "Prepared encounters with a structured, costed creature list")
class EncounterController(
    private val orchestrator: EncounterOrchestrator,
) {

    @PostMapping("/campaigns/{campaignId}/encounters")
    @Operation(summary = "Add an encounter to a campaign by hand")
    fun create(
        @PathVariable campaignId: Long,
        @RequestBody request: EncounterRequest,
    ): ResponseEntity<EncounterResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.addEncounter(campaignId, request))

    @GetMapping("/campaigns/{campaignId}/encounters")
    @Operation(summary = "List all encounters in a campaign")
    fun list(@PathVariable campaignId: Long): List<EncounterResponse> =
        orchestrator.listEncounters(campaignId)

    @GetMapping("/sessions/{sessionId}/encounters")
    @Operation(summary = "List the encounters prepared for a session")
    fun listForSession(@PathVariable sessionId: Long): List<EncounterResponse> =
        orchestrator.listEncountersForSession(sessionId)

    @GetMapping("/encounters/{id}")
    @Operation(summary = "Get an encounter, with its difficulty recalculated against the current party")
    fun get(@PathVariable id: Long): EncounterResponse = orchestrator.getEncounter(id)

    @PutMapping("/encounters/{id}")
    @Operation(summary = "Update an encounter")
    fun update(@PathVariable id: Long, @RequestBody request: EncounterRequest): EncounterResponse =
        orchestrator.updateEncounter(id, request)

    @DeleteMapping("/encounters/{id}")
    @Operation(summary = "Delete an encounter")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        orchestrator.deleteEncounter(id)
        return ResponseEntity.noContent().build()
    }
}


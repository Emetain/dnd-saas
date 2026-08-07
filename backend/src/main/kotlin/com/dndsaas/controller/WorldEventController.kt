package com.dndsaas.controller

import com.dndsaas.dto.WorldEventRequest
import com.dndsaas.dto.WorldEventResponse
import com.dndsaas.orchestration.WorldEventOrchestrator
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

/** REST entry point for world events (the campaign timeline). */
@RestController
@RequestMapping("/v1")
@Tag(name = "World Events", description = "The campaign timeline and its consequences")
class WorldEventController(
    private val orchestrator: WorldEventOrchestrator,
) {

    @PostMapping("/campaigns/{campaignId}/events")
    @Operation(summary = "Record a world event")
    fun create(
        @PathVariable campaignId: Long,
        @RequestBody request: WorldEventRequest,
    ): ResponseEntity<WorldEventResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.addWorldEvent(campaignId, request))

    @GetMapping("/campaigns/{campaignId}/events")
    @Operation(summary = "List the campaign timeline, oldest first")
    fun list(@PathVariable campaignId: Long): List<WorldEventResponse> =
        orchestrator.listWorldEvents(campaignId)

    @GetMapping("/events/{id}")
    @Operation(summary = "Get a world event by id")
    fun get(@PathVariable id: Long): WorldEventResponse = orchestrator.getWorldEvent(id)

    @PutMapping("/events/{id}")
    @Operation(summary = "Update a world event")
    fun update(@PathVariable id: Long, @RequestBody request: WorldEventRequest): WorldEventResponse =
        orchestrator.updateWorldEvent(id, request)

    @DeleteMapping("/events/{id}")
    @Operation(summary = "Delete a world event")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        orchestrator.deleteWorldEvent(id)
        return ResponseEntity.noContent().build()
    }
}


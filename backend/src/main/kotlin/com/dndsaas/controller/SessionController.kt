package com.dndsaas.controller

import com.dndsaas.dto.SessionRequest
import com.dndsaas.dto.SessionResponse
import com.dndsaas.orchestration.SessionOrchestrator
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
 * REST entry point for sessions. Sessions are nested under a campaign for
 * creation/listing, and addressable directly by id for update/delete.
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "Sessions", description = "Play sessions and session notes within a campaign")
class SessionController(
    private val orchestrator: SessionOrchestrator,
) {

    @PostMapping("/campaigns/{campaignId}/sessions")
    @Operation(summary = "Add a session to a campaign")
    fun create(
        @PathVariable campaignId: Long,
        @RequestBody request: SessionRequest,
    ): ResponseEntity<SessionResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.createSession(campaignId, request))

    @GetMapping("/campaigns/{campaignId}/sessions")
    @Operation(summary = "List all sessions of a campaign")
    fun list(@PathVariable campaignId: Long): List<SessionResponse> =
        orchestrator.listSessions(campaignId)

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Get a session by id")
    fun get(@PathVariable id: Long): SessionResponse = orchestrator.getSession(id)

    @PutMapping("/sessions/{id}")
    @Operation(summary = "Update a session")
    fun update(@PathVariable id: Long, @RequestBody request: SessionRequest): SessionResponse =
        orchestrator.updateSession(id, request)

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Delete a session")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        orchestrator.deleteSession(id)
        return ResponseEntity.noContent().build()
    }
}


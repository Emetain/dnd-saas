package com.dndsaas.controller

import com.dndsaas.domain.NpcStatus
import com.dndsaas.dto.NpcDetailResponse
import com.dndsaas.dto.NpcRequest
import com.dndsaas.dto.NpcResponse
import com.dndsaas.orchestration.NpcOrchestrator
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** REST entry point for NPCs. */
@RestController
@RequestMapping("/v1")
@Tag(name = "NPCs", description = "Non-player characters and everything connected to them")
class NpcController(
    private val orchestrator: NpcOrchestrator,
) {

    @PostMapping("/campaigns/{campaignId}/npcs")
    @Operation(summary = "Add an NPC to a campaign")
    fun create(
        @PathVariable campaignId: Long,
        @RequestBody request: NpcRequest,
    ): ResponseEntity<NpcResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.addNpc(campaignId, request))

    @GetMapping("/campaigns/{campaignId}/npcs")
    @Operation(summary = "List NPCs in a campaign, optionally filtered by status")
    fun list(
        @PathVariable campaignId: Long,
        @RequestParam(required = false) status: NpcStatus?,
    ): List<NpcResponse> = orchestrator.listNpcs(campaignId, status)

    @GetMapping("/npcs/{id}")
    @Operation(summary = "Get an NPC by id")
    fun get(@PathVariable id: Long): NpcResponse = orchestrator.getNpc(id)

    @GetMapping("/npcs/{id}/detail")
    @Operation(summary = "Get an NPC with its relationships, quests and items (full memory view)")
    fun detail(@PathVariable id: Long): NpcDetailResponse = orchestrator.getNpcDetail(id)

    @PutMapping("/npcs/{id}")
    @Operation(summary = "Update an NPC")
    fun update(@PathVariable id: Long, @RequestBody request: NpcRequest): NpcResponse =
        orchestrator.updateNpc(id, request)

    @DeleteMapping("/npcs/{id}")
    @Operation(summary = "Delete an NPC")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        orchestrator.deleteNpc(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/npcs/{id}/appearances/{sessionId}")
    @Operation(summary = "Record that this NPC appeared in a session")
    fun addAppearance(@PathVariable id: Long, @PathVariable sessionId: Long): NpcResponse =
        orchestrator.addAppearance(id, sessionId)

    @DeleteMapping("/npcs/{id}/appearances/{sessionId}")
    @Operation(summary = "Remove a session appearance from this NPC")
    fun removeAppearance(@PathVariable id: Long, @PathVariable sessionId: Long): NpcResponse =
        orchestrator.removeAppearance(id, sessionId)
}


package com.dndsaas.controller

import com.dndsaas.dto.RelationshipRequest
import com.dndsaas.dto.RelationshipResponse
import com.dndsaas.orchestration.RelationshipOrchestrator
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

/** REST entry point for the social web between NPCs. */
@RestController
@RequestMapping("/v1")
@Tag(name = "Relationships", description = "How NPCs are connected to each other")
class RelationshipController(
    private val orchestrator: RelationshipOrchestrator,
) {

    @PostMapping("/campaigns/{campaignId}/relationships")
    @Operation(summary = "Link two NPCs together")
    fun create(
        @PathVariable campaignId: Long,
        @RequestBody request: RelationshipRequest,
    ): ResponseEntity<RelationshipResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(orchestrator.addRelationship(campaignId, request))

    @GetMapping("/campaigns/{campaignId}/relationships")
    @Operation(summary = "List every relationship in a campaign")
    fun list(@PathVariable campaignId: Long): List<RelationshipResponse> =
        orchestrator.listRelationships(campaignId)

    @GetMapping("/npcs/{npcId}/relationships")
    @Operation(summary = "List every connection a single NPC has, in either direction")
    fun listForNpc(@PathVariable npcId: Long): List<RelationshipResponse> =
        orchestrator.listRelationshipsForNpc(npcId)

    @GetMapping("/relationships/{id}")
    @Operation(summary = "Get a relationship by id")
    fun get(@PathVariable id: Long): RelationshipResponse = orchestrator.getRelationship(id)

    @PutMapping("/relationships/{id}")
    @Operation(summary = "Update a relationship")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: RelationshipRequest,
    ): RelationshipResponse = orchestrator.updateRelationship(id, request)

    @DeleteMapping("/relationships/{id}")
    @Operation(summary = "Delete a relationship")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        orchestrator.deleteRelationship(id)
        return ResponseEntity.noContent().build()
    }
}


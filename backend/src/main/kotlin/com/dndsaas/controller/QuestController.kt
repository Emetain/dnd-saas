package com.dndsaas.controller

import com.dndsaas.domain.QuestStatus
import com.dndsaas.dto.QuestRequest
import com.dndsaas.dto.QuestResponse
import com.dndsaas.orchestration.QuestOrchestrator
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

/** REST entry point for quests. */
@RestController
@RequestMapping("/v1")
@Tag(name = "Quests", description = "What the party is chasing")
class QuestController(
    private val orchestrator: QuestOrchestrator,
) {

    @PostMapping("/campaigns/{campaignId}/quests")
    @Operation(summary = "Add a quest to a campaign")
    fun create(
        @PathVariable campaignId: Long,
        @RequestBody request: QuestRequest,
    ): ResponseEntity<QuestResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.addQuest(campaignId, request))

    @GetMapping("/campaigns/{campaignId}/quests")
    @Operation(summary = "List quests in a campaign, optionally filtered by status")
    fun list(
        @PathVariable campaignId: Long,
        @RequestParam(required = false) status: QuestStatus?,
    ): List<QuestResponse> = orchestrator.listQuests(campaignId, status)

    @GetMapping("/campaigns/{campaignId}/quests/active")
    @Operation(summary = "List the quests currently in play (rumoured, available or active)")
    fun listActive(@PathVariable campaignId: Long): List<QuestResponse> =
        orchestrator.listActiveQuests(campaignId)

    @GetMapping("/quests/{id}")
    @Operation(summary = "Get a quest by id")
    fun get(@PathVariable id: Long): QuestResponse = orchestrator.getQuest(id)

    @PutMapping("/quests/{id}")
    @Operation(summary = "Update a quest (e.g. change its status)")
    fun update(@PathVariable id: Long, @RequestBody request: QuestRequest): QuestResponse =
        orchestrator.updateQuest(id, request)

    @DeleteMapping("/quests/{id}")
    @Operation(summary = "Delete a quest")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        orchestrator.deleteQuest(id)
        return ResponseEntity.noContent().build()
    }
}


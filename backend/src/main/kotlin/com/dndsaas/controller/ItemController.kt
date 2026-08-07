package com.dndsaas.controller

import com.dndsaas.dto.ItemRequest
import com.dndsaas.dto.ItemResponse
import com.dndsaas.orchestration.ItemOrchestrator
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

/** REST entry point for items. */
@RestController
@RequestMapping("/v1")
@Tag(name = "Items", description = "Loot, artefacts and quest objects")
class ItemController(
    private val orchestrator: ItemOrchestrator,
) {

    @PostMapping("/campaigns/{campaignId}/items")
    @Operation(summary = "Add an item to a campaign")
    fun create(
        @PathVariable campaignId: Long,
        @RequestBody request: ItemRequest,
    ): ResponseEntity<ItemResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.addItem(campaignId, request))

    @GetMapping("/campaigns/{campaignId}/items")
    @Operation(summary = "List all items in a campaign")
    fun list(@PathVariable campaignId: Long): List<ItemResponse> = orchestrator.listItems(campaignId)

    @GetMapping("/characters/{characterId}/inventory")
    @Operation(summary = "List the items carried by a player character")
    fun inventory(@PathVariable characterId: Long): List<ItemResponse> =
        orchestrator.listCharacterInventory(characterId)

    @GetMapping("/items/{id}")
    @Operation(summary = "Get an item by id")
    fun get(@PathVariable id: Long): ItemResponse = orchestrator.getItem(id)

    @PutMapping("/items/{id}")
    @Operation(summary = "Update an item (e.g. hand it to a different owner)")
    fun update(@PathVariable id: Long, @RequestBody request: ItemRequest): ItemResponse =
        orchestrator.updateItem(id, request)

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Delete an item")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        orchestrator.deleteItem(id)
        return ResponseEntity.noContent().build()
    }
}


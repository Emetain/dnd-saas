package com.dndsaas.controller

import com.dndsaas.dto.CampaignRequest
import com.dndsaas.dto.CampaignResponse
import com.dndsaas.orchestration.CampaignOrchestrator
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
 * REST entry point for campaigns. Forwards each task to the orchestration layer.
 */
@RestController
@RequestMapping("/v1/campaigns")
@Tag(name = "Campaigns", description = "Create and manage campaigns")
class CampaignController(
    private val orchestrator: CampaignOrchestrator,
) {

    @PostMapping
    @Operation(summary = "Create a campaign")
    fun create(@RequestBody request: CampaignRequest): ResponseEntity<CampaignResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.createCampaign(request))

    @GetMapping
    @Operation(summary = "List all campaigns")
    fun list(): List<CampaignResponse> = orchestrator.listCampaigns()

    @GetMapping("/{id}")
    @Operation(summary = "Get a campaign by id")
    fun get(@PathVariable id: Long): CampaignResponse = orchestrator.getCampaign(id)

    @PutMapping("/{id}")
    @Operation(summary = "Update a campaign")
    fun update(@PathVariable id: Long, @RequestBody request: CampaignRequest): CampaignResponse =
        orchestrator.updateCampaign(id, request)

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a campaign")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        orchestrator.deleteCampaign(id)
        return ResponseEntity.noContent().build()
    }
}


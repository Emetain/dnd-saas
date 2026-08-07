package com.dndsaas.controller

import com.dndsaas.dto.LocationRequest
import com.dndsaas.dto.LocationResponse
import com.dndsaas.orchestration.LocationOrchestrator
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

/** REST entry point for locations. Forwards every task to the orchestration layer. */
@RestController
@RequestMapping("/v1")
@Tag(name = "Locations", description = "Places in the campaign world (Campaign Memory)")
class LocationController(
    private val orchestrator: LocationOrchestrator,
) {

    @PostMapping("/campaigns/{campaignId}/locations")
    @Operation(summary = "Add a location to a campaign")
    fun create(
        @PathVariable campaignId: Long,
        @RequestBody request: LocationRequest,
    ): ResponseEntity<LocationResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orchestrator.addLocation(campaignId, request))

    @GetMapping("/campaigns/{campaignId}/locations")
    @Operation(summary = "List all locations in a campaign")
    fun list(@PathVariable campaignId: Long): List<LocationResponse> =
        orchestrator.listLocations(campaignId)

    @GetMapping("/campaigns/{campaignId}/locations/roots")
    @Operation(summary = "List only top-level locations (for a world tree)")
    fun listRoots(@PathVariable campaignId: Long): List<LocationResponse> =
        orchestrator.listRootLocations(campaignId)

    @GetMapping("/locations/{id}")
    @Operation(summary = "Get a location by id")
    fun get(@PathVariable id: Long): LocationResponse = orchestrator.getLocation(id)

    @GetMapping("/locations/{id}/children")
    @Operation(summary = "List the locations contained within this location")
    fun listChildren(@PathVariable id: Long): List<LocationResponse> =
        orchestrator.listChildLocations(id)

    @PutMapping("/locations/{id}")
    @Operation(summary = "Update a location")
    fun update(@PathVariable id: Long, @RequestBody request: LocationRequest): LocationResponse =
        orchestrator.updateLocation(id, request)

    @DeleteMapping("/locations/{id}")
    @Operation(summary = "Delete a location")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        orchestrator.deleteLocation(id)
        return ResponseEntity.noContent().build()
    }
}


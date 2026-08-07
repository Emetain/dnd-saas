package com.dndsaas.orchestration

import com.dndsaas.dto.LocationRequest
import com.dndsaas.dto.LocationResponse
import com.dndsaas.service.CampaignService
import com.dndsaas.service.LocationService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for locations. Resolves the campaign a location
 * belongs to, then delegates the work to the service layer.
 */
@Component
class LocationOrchestrator(
    private val locationService: LocationService,
    private val campaignService: CampaignService,
) {

    fun addLocation(campaignId: Long, request: LocationRequest): LocationResponse {
        val campaign = campaignService.findEntity(campaignId)
        return locationService.create(campaign, request)
    }

    fun listLocations(campaignId: Long): List<LocationResponse> {
        campaignService.findEntity(campaignId)
        return locationService.listByCampaign(campaignId)
    }

    /** Only top-level locations, for rendering the world as a tree. */
    fun listRootLocations(campaignId: Long): List<LocationResponse> {
        campaignService.findEntity(campaignId)
        return locationService.listRoots(campaignId)
    }

    fun listChildLocations(id: Long): List<LocationResponse> {
        locationService.findEntity(id)
        return locationService.listChildren(id)
    }

    fun getLocation(id: Long): LocationResponse = locationService.get(id)

    fun updateLocation(id: Long, request: LocationRequest): LocationResponse =
        locationService.update(id, request)

    fun deleteLocation(id: Long) = locationService.delete(id)
}


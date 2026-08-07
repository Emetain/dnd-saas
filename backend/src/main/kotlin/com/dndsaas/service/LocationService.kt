package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.Location
import com.dndsaas.dto.EntityRef
import com.dndsaas.dto.LocationRequest
import com.dndsaas.dto.LocationResponse
import com.dndsaas.repository.LocationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Business logic for locations — the places that make up the campaign world.
 */
@Service
@Transactional
class LocationService(
    private val locationRepository: LocationRepository,
) {

    fun create(campaign: Campaign, request: LocationRequest): LocationResponse {
        val location = Location(campaign = campaign)
        apply(location, request, campaign)
        return toResponse(locationRepository.save(location))
    }

    fun update(id: Long, request: LocationRequest): LocationResponse {
        val location = findEntity(id)
        apply(location, request, location.campaign!!)
        return toResponse(locationRepository.save(location))
    }

    @Transactional(readOnly = true)
    fun get(id: Long): LocationResponse = toResponse(findEntity(id))

    @Transactional(readOnly = true)
    fun listByCampaign(campaignId: Long): List<LocationResponse> =
        locationRepository.findByCampaignIdOrderByNameAsc(campaignId).map(::toResponse)

    /** Only top-level locations, for rendering the world as a tree. */
    @Transactional(readOnly = true)
    fun listRoots(campaignId: Long): List<LocationResponse> =
        locationRepository.findByCampaignIdAndParentIsNullOrderByNameAsc(campaignId).map(::toResponse)

    @Transactional(readOnly = true)
    fun listChildren(parentId: Long): List<LocationResponse> =
        locationRepository.findByParentIdOrderByNameAsc(parentId).map(::toResponse)

    fun delete(id: Long) = locationRepository.deleteById(id)

    fun findEntity(id: Long): Location =
        locationRepository.findById(id)
            .orElseThrow { NoSuchElementException("Location $id not found") }

    /** Resolves an optional id to an entity, used when linking other objects here. */
    fun findEntityOrNull(id: Long?): Location? = id?.let { findEntity(it) }

    fun countForCampaign(campaignId: Long): Long = locationRepository.countByCampaignId(campaignId)

    /** Clears parent links so a whole campaign's locations can be deleted safely. */
    fun detachHierarchy(campaignId: Long) {
        val locations = locationRepository.findByCampaignIdOrderByNameAsc(campaignId)
        locations.forEach { it.parent = null }
        locationRepository.saveAll(locations)
    }

    fun deleteAllForCampaign(campaignId: Long) {
        detachHierarchy(campaignId)
        locationRepository.deleteAll(locationRepository.findByCampaignIdOrderByNameAsc(campaignId))
    }

    private fun apply(location: Location, request: LocationRequest, campaign: Campaign) {
        val parent = findEntityOrNull(request.parentId)
        require(parent == null || parent.campaign?.id == campaign.id) {
            "Parent location ${request.parentId} belongs to a different campaign"
        }
        require(parent == null || parent.id != location.id) {
            "A location cannot be its own parent"
        }
        location.name = request.name
        location.type = request.type
        location.description = request.description
        location.atmosphere = request.atmosphere
        location.parent = parent
        location.notes = request.notes
        location.discovered = request.discovered
    }

    fun toResponse(location: Location): LocationResponse =
        LocationResponse(
            id = location.id!!,
            campaignId = location.campaign!!.id!!,
            name = location.name,
            type = location.type,
            description = location.description,
            atmosphere = location.atmosphere,
            parent = location.parent?.let { EntityRef(it.id!!, it.name) },
            notes = location.notes,
            discovered = location.discovered,
            createdAt = location.createdAt,
            updatedAt = location.updatedAt,
        )
}


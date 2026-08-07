package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.WorldEvent
import com.dndsaas.dto.EntityRef
import com.dndsaas.dto.WorldEventRequest
import com.dndsaas.dto.WorldEventResponse
import com.dndsaas.repository.WorldEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Business logic for world events — the campaign's history and consequences.
 */
@Service
@Transactional
class WorldEventService(
    private val worldEventRepository: WorldEventRepository,
    private val sessionService: SessionService,
    private val locationService: LocationService,
) {

    fun create(campaign: Campaign, request: WorldEventRequest): WorldEventResponse {
        val event = WorldEvent(campaign = campaign)
        apply(event, request)
        return toResponse(worldEventRepository.save(event))
    }

    fun update(id: Long, request: WorldEventRequest): WorldEventResponse {
        val event = findEntity(id)
        apply(event, request)
        return toResponse(worldEventRepository.save(event))
    }

    @Transactional(readOnly = true)
    fun get(id: Long): WorldEventResponse = toResponse(findEntity(id))

    /** The campaign timeline, oldest first. */
    @Transactional(readOnly = true)
    fun listByCampaign(campaignId: Long): List<WorldEventResponse> =
        worldEventRepository.findByCampaignIdOrderByCreatedAtAsc(campaignId).map(::toResponse)

    fun delete(id: Long) = worldEventRepository.deleteById(id)

    fun findEntity(id: Long): WorldEvent =
        worldEventRepository.findById(id)
            .orElseThrow { NoSuchElementException("World event $id not found") }

    fun countForCampaign(campaignId: Long): Long = worldEventRepository.countByCampaignId(campaignId)

    fun deleteAllForCampaign(campaignId: Long) =
        worldEventRepository.deleteAll(worldEventRepository.findByCampaignIdOrderByCreatedAtAsc(campaignId))

    private fun apply(event: WorldEvent, request: WorldEventRequest) {
        event.title = request.title
        event.description = request.description
        event.inGameDate = request.inGameDate
        event.importance = request.importance
        event.session = request.sessionId?.let { sessionService.findEntity(it) }
        event.location = locationService.findEntityOrNull(request.locationId)
        event.consequences = request.consequences
        event.knownToPlayers = request.knownToPlayers
    }

    fun toResponse(event: WorldEvent): WorldEventResponse =
        WorldEventResponse(
            id = event.id!!,
            campaignId = event.campaign!!.id!!,
            title = event.title,
            description = event.description,
            inGameDate = event.inGameDate,
            importance = event.importance,
            session = event.session?.let { EntityRef(it.id!!, "Session ${it.sessionNumber}: ${it.title}") },
            location = event.location?.let { EntityRef(it.id!!, it.name) },
            consequences = event.consequences,
            knownToPlayers = event.knownToPlayers,
            createdAt = event.createdAt,
            updatedAt = event.updatedAt,
        )
}


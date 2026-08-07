package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.Npc
import com.dndsaas.domain.NpcStatus
import com.dndsaas.dto.EntityRef
import com.dndsaas.dto.NpcRequest
import com.dndsaas.dto.NpcResponse
import com.dndsaas.repository.NpcRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Business logic for NPCs — the most connected objects in Campaign Memory.
 */
@Service
@Transactional
class NpcService(
    private val npcRepository: NpcRepository,
    private val locationService: LocationService,
    private val factionService: FactionService,
    private val sessionService: SessionService,
) {

    fun create(campaign: Campaign, request: NpcRequest, aiGenerated: Boolean = false): NpcResponse {
        val npc = Npc(campaign = campaign)
        npc.aiGenerated = aiGenerated
        apply(npc, request)
        return toResponse(npcRepository.save(npc))
    }

    fun update(id: Long, request: NpcRequest): NpcResponse {
        val npc = findEntity(id)
        apply(npc, request)
        return toResponse(npcRepository.save(npc))
    }

    @Transactional(readOnly = true)
    fun get(id: Long): NpcResponse = toResponse(findEntity(id))

    @Transactional(readOnly = true)
    fun listByCampaign(campaignId: Long): List<NpcResponse> =
        npcRepository.findByCampaignIdOrderByNameAsc(campaignId).map(::toResponse)

    @Transactional(readOnly = true)
    fun listByCampaignAndStatus(campaignId: Long, status: NpcStatus): List<NpcResponse> =
        npcRepository.findByCampaignIdAndStatusOrderByNameAsc(campaignId, status).map(::toResponse)

    @Transactional(readOnly = true)
    fun listByLocation(locationId: Long): List<NpcResponse> =
        npcRepository.findByLocationIdOrderByNameAsc(locationId).map(::toResponse)

    @Transactional(readOnly = true)
    fun listByFaction(factionId: Long): List<NpcResponse> =
        npcRepository.findByFactionIdOrderByNameAsc(factionId).map(::toResponse)

    /** Records that an NPC appeared in a session — builds the NPC's own history. */
    fun addAppearance(npcId: Long, sessionId: Long): NpcResponse {
        val npc = findEntity(npcId)
        val session = sessionService.findEntity(sessionId)
        require(session.campaign?.id == npc.campaign?.id) {
            "Session $sessionId belongs to a different campaign than NPC $npcId"
        }
        npc.appearances.add(session)
        return toResponse(npcRepository.save(npc))
    }

    fun removeAppearance(npcId: Long, sessionId: Long): NpcResponse {
        val npc = findEntity(npcId)
        npc.appearances.removeIf { it.id == sessionId }
        return toResponse(npcRepository.save(npc))
    }

    fun delete(id: Long) = npcRepository.deleteById(id)

    fun findEntity(id: Long): Npc =
        npcRepository.findById(id)
            .orElseThrow { NoSuchElementException("NPC $id not found") }

    fun findEntityOrNull(id: Long?): Npc? = id?.let { findEntity(it) }

    fun findAll(ids: Collection<Long>): List<Npc> = ids.map(::findEntity)

    fun countForCampaign(campaignId: Long): Long = npcRepository.countByCampaignId(campaignId)

    fun deleteAllForCampaign(campaignId: Long) =
        npcRepository.deleteAll(npcRepository.findByCampaignIdOrderByNameAsc(campaignId))

    private fun apply(npc: Npc, request: NpcRequest) {
        npc.name = request.name
        npc.race = request.race
        npc.occupation = request.occupation
        npc.role = request.role
        npc.description = request.description
        npc.appearance = request.appearance
        npc.personality = request.personality
        npc.motivation = request.motivation
        npc.secret = request.secret
        npc.voice = request.voice
        npc.status = request.status
        npc.disposition = request.disposition
        npc.location = locationService.findEntityOrNull(request.locationId)
        npc.faction = factionService.findEntityOrNull(request.factionId)
        npc.notes = request.notes
    }

    fun toResponse(npc: Npc): NpcResponse =
        NpcResponse(
            id = npc.id!!,
            campaignId = npc.campaign!!.id!!,
            name = npc.name,
            race = npc.race,
            occupation = npc.occupation,
            role = npc.role,
            description = npc.description,
            appearance = npc.appearance,
            personality = npc.personality,
            motivation = npc.motivation,
            secret = npc.secret,
            voice = npc.voice,
            status = npc.status,
            disposition = npc.disposition,
            location = npc.location?.let { EntityRef(it.id!!, it.name) },
            faction = npc.faction?.let { EntityRef(it.id!!, it.name) },
            appearances = npc.appearances
                .sortedBy { it.sessionNumber }
                .map { EntityRef(it.id!!, "Session ${it.sessionNumber}: ${it.title}") },
            notes = npc.notes,
            aiGenerated = npc.aiGenerated,
            createdAt = npc.createdAt,
            updatedAt = npc.updatedAt,
        )
}


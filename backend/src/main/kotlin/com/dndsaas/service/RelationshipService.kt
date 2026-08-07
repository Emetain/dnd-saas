package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.Relationship
import com.dndsaas.dto.EntityRef
import com.dndsaas.dto.RelationshipRequest
import com.dndsaas.dto.RelationshipResponse
import com.dndsaas.repository.RelationshipRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Business logic for the social web between NPCs.
 */
@Service
@Transactional
class RelationshipService(
    private val relationshipRepository: RelationshipRepository,
    private val npcService: NpcService,
) {

    fun create(campaign: Campaign, request: RelationshipRequest): RelationshipResponse {
        val relationship = Relationship(campaign = campaign)
        apply(relationship, request, campaign)
        return toResponse(relationshipRepository.save(relationship))
    }

    fun update(id: Long, request: RelationshipRequest): RelationshipResponse {
        val relationship = findEntity(id)
        apply(relationship, request, relationship.campaign!!)
        return toResponse(relationshipRepository.save(relationship))
    }

    @Transactional(readOnly = true)
    fun get(id: Long): RelationshipResponse = toResponse(findEntity(id))

    @Transactional(readOnly = true)
    fun listByCampaign(campaignId: Long): List<RelationshipResponse> =
        relationshipRepository.findByCampaignId(campaignId).map(::toResponse)

    /** Every connection an NPC has, in either direction. */
    @Transactional(readOnly = true)
    fun listForNpc(npcId: Long): List<RelationshipResponse> =
        relationshipRepository.findByFromNpcIdOrToNpcId(npcId, npcId).map(::toResponse)

    fun delete(id: Long) = relationshipRepository.deleteById(id)

    fun findEntity(id: Long): Relationship =
        relationshipRepository.findById(id)
            .orElseThrow { NoSuchElementException("Relationship $id not found") }

    fun countForCampaign(campaignId: Long): Long = relationshipRepository.countByCampaignId(campaignId)

    fun deleteAllForCampaign(campaignId: Long) =
        relationshipRepository.deleteAll(relationshipRepository.findByCampaignId(campaignId))

    private fun apply(relationship: Relationship, request: RelationshipRequest, campaign: Campaign) {
        require(request.fromNpcId != request.toNpcId) {
            "An NPC cannot have a relationship with itself"
        }
        val from = npcService.findEntity(request.fromNpcId)
        val to = npcService.findEntity(request.toNpcId)
        require(from.campaign?.id == campaign.id && to.campaign?.id == campaign.id) {
            "Both NPCs must belong to campaign ${campaign.id}"
        }
        relationship.fromNpc = from
        relationship.toNpc = to
        relationship.type = request.type
        relationship.description = request.description
        relationship.strength = request.strength.coerceIn(-100, 100)
        relationship.knownToPlayers = request.knownToPlayers
    }

    fun toResponse(relationship: Relationship): RelationshipResponse =
        RelationshipResponse(
            id = relationship.id!!,
            campaignId = relationship.campaign!!.id!!,
            fromNpc = relationship.fromNpc!!.let { EntityRef(it.id!!, it.name) },
            toNpc = relationship.toNpc!!.let { EntityRef(it.id!!, it.name) },
            type = relationship.type,
            description = relationship.description,
            strength = relationship.strength,
            knownToPlayers = relationship.knownToPlayers,
            createdAt = relationship.createdAt,
            updatedAt = relationship.updatedAt,
        )
}


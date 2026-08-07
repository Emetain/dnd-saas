package com.dndsaas.orchestration

import com.dndsaas.dto.RelationshipRequest
import com.dndsaas.dto.RelationshipResponse
import com.dndsaas.service.CampaignService
import com.dndsaas.service.NpcService
import com.dndsaas.service.RelationshipService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for NPC relationships.
 */
@Component
class RelationshipOrchestrator(
    private val relationshipService: RelationshipService,
    private val npcService: NpcService,
    private val campaignService: CampaignService,
) {

    fun addRelationship(campaignId: Long, request: RelationshipRequest): RelationshipResponse {
        val campaign = campaignService.findEntity(campaignId)
        return relationshipService.create(campaign, request)
    }

    fun listRelationships(campaignId: Long): List<RelationshipResponse> {
        campaignService.findEntity(campaignId)
        return relationshipService.listByCampaign(campaignId)
    }

    /** Task: every connection a single NPC has, in either direction. */
    fun listRelationshipsForNpc(npcId: Long): List<RelationshipResponse> {
        npcService.findEntity(npcId)
        return relationshipService.listForNpc(npcId)
    }

    fun getRelationship(id: Long): RelationshipResponse = relationshipService.get(id)

    fun updateRelationship(id: Long, request: RelationshipRequest): RelationshipResponse =
        relationshipService.update(id, request)

    fun deleteRelationship(id: Long) = relationshipService.delete(id)
}


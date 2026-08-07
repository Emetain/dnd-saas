package com.dndsaas.orchestration

import com.dndsaas.domain.NpcStatus
import com.dndsaas.dto.EntityRef
import com.dndsaas.dto.NpcDetailResponse
import com.dndsaas.dto.NpcRequest
import com.dndsaas.dto.NpcResponse
import com.dndsaas.service.CampaignService
import com.dndsaas.service.ItemService
import com.dndsaas.service.NpcService
import com.dndsaas.service.QuestService
import com.dndsaas.service.RelationshipService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for NPCs.
 *
 * This is the clearest example of why the middle layer exists: building the
 * full picture of an NPC requires four different services, and coordinating
 * them is exactly this layer's job.
 */
@Component
class NpcOrchestrator(
    private val npcService: NpcService,
    private val relationshipService: RelationshipService,
    private val questService: QuestService,
    private val itemService: ItemService,
    private val campaignService: CampaignService,
) {

    fun addNpc(campaignId: Long, request: NpcRequest): NpcResponse {
        val campaign = campaignService.findEntity(campaignId)
        return npcService.create(campaign, request)
    }

    fun listNpcs(campaignId: Long, status: NpcStatus?): List<NpcResponse> {
        campaignService.findEntity(campaignId)
        return if (status == null) {
            npcService.listByCampaign(campaignId)
        } else {
            npcService.listByCampaignAndStatus(campaignId, status)
        }
    }

    fun getNpc(id: Long): NpcResponse = npcService.get(id)

    /**
     * Task: everything connected to one NPC — who they are, who they know,
     * what they are involved in and what they carry.
     */
    fun getNpcDetail(id: Long): NpcDetailResponse {
        val npc = npcService.findEntity(id)
        val campaignId = npc.campaign!!.id!!
        return NpcDetailResponse(
            npc = npcService.toResponse(npc),
            relationships = relationshipService.listForNpc(id),
            quests = questService.listForNpc(campaignId, id).map { EntityRef(it.id, it.title) },
            items = itemService.listForNpc(campaignId, id).map { EntityRef(it.id, it.name) },
        )
    }

    fun updateNpc(id: Long, request: NpcRequest): NpcResponse = npcService.update(id, request)

    fun deleteNpc(id: Long) = npcService.delete(id)

    /** Task: record that an NPC appeared in a session. */
    fun addAppearance(npcId: Long, sessionId: Long): NpcResponse =
        npcService.addAppearance(npcId, sessionId)

    fun removeAppearance(npcId: Long, sessionId: Long): NpcResponse =
        npcService.removeAppearance(npcId, sessionId)
}


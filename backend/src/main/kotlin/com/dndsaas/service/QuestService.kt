package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.Quest
import com.dndsaas.domain.QuestStatus
import com.dndsaas.dto.EntityRef
import com.dndsaas.dto.QuestRequest
import com.dndsaas.dto.QuestResponse
import com.dndsaas.repository.QuestRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Business logic for quests — what the party is currently chasing.
 */
@Service
@Transactional
class QuestService(
    private val questRepository: QuestRepository,
    private val npcService: NpcService,
    private val locationService: LocationService,
) {

    /** Statuses that count as "currently going on" for AI context. */
    private val activeStatuses = setOf(QuestStatus.AVAILABLE, QuestStatus.ACTIVE, QuestStatus.RUMOURED)

    fun create(campaign: Campaign, request: QuestRequest, aiGenerated: Boolean = false): QuestResponse {
        val quest = Quest(campaign = campaign)
        quest.aiGenerated = aiGenerated
        apply(quest, request)
        return toResponse(questRepository.save(quest))
    }

    fun update(id: Long, request: QuestRequest): QuestResponse {
        val quest = findEntity(id)
        apply(quest, request)
        return toResponse(questRepository.save(quest))
    }

    @Transactional(readOnly = true)
    fun get(id: Long): QuestResponse = toResponse(findEntity(id))

    @Transactional(readOnly = true)
    fun listByCampaign(campaignId: Long): List<QuestResponse> =
        questRepository.findByCampaignIdOrderByTitleAsc(campaignId).map(::toResponse)

    @Transactional(readOnly = true)
    fun listByCampaignAndStatus(campaignId: Long, status: QuestStatus): List<QuestResponse> =
        questRepository.findByCampaignIdAndStatusOrderByTitleAsc(campaignId, status).map(::toResponse)

    /** The quests the AI should treat as "what is happening right now". */
    @Transactional(readOnly = true)
    fun listActive(campaignId: Long): List<QuestResponse> =
        questRepository.findByCampaignIdAndStatusInOrderByTitleAsc(campaignId, activeStatuses).map(::toResponse)

    /** Quests a given NPC is entangled in, either as giver or participant. */
    @Transactional(readOnly = true)
    fun listForNpc(campaignId: Long, npcId: Long): List<QuestResponse> =
        questRepository.findByCampaignIdOrderByTitleAsc(campaignId)
            .filter { quest -> quest.questGiver?.id == npcId || quest.involvedNpcs.any { it.id == npcId } }
            .map(::toResponse)

    fun delete(id: Long) = questRepository.deleteById(id)

    fun findEntity(id: Long): Quest =
        questRepository.findById(id)
            .orElseThrow { NoSuchElementException("Quest $id not found") }

    fun countForCampaign(campaignId: Long): Long = questRepository.countByCampaignId(campaignId)

    fun deleteAllForCampaign(campaignId: Long) =
        questRepository.deleteAll(questRepository.findByCampaignIdOrderByTitleAsc(campaignId))

    private fun apply(quest: Quest, request: QuestRequest) {
        quest.title = request.title
        quest.description = request.description
        quest.hook = request.hook
        quest.objective = request.objective
        quest.status = request.status
        quest.questGiver = npcService.findEntityOrNull(request.questGiverId)
        quest.location = locationService.findEntityOrNull(request.locationId)
        quest.involvedNpcs = npcService.findAll(request.involvedNpcIds).toMutableSet()
        quest.reward = request.reward
        quest.notes = request.notes
    }

    fun toResponse(quest: Quest): QuestResponse =
        QuestResponse(
            id = quest.id!!,
            campaignId = quest.campaign!!.id!!,
            title = quest.title,
            description = quest.description,
            hook = quest.hook,
            objective = quest.objective,
            status = quest.status,
            questGiver = quest.questGiver?.let { EntityRef(it.id!!, it.name) },
            location = quest.location?.let { EntityRef(it.id!!, it.name) },
            involvedNpcs = quest.involvedNpcs.map { EntityRef(it.id!!, it.name) }.sortedBy { it.name },
            reward = quest.reward,
            notes = quest.notes,
            aiGenerated = quest.aiGenerated,
            createdAt = quest.createdAt,
            updatedAt = quest.updatedAt,
        )
}


package com.dndsaas.dto

import com.dndsaas.domain.QuestStatus
import java.time.Instant

/** Request to create or update a quest. */
data class QuestRequest(
    val title: String,
    val description: String = "",
    val hook: String = "",
    val objective: String = "",
    val status: QuestStatus = QuestStatus.AVAILABLE,
    val questGiverId: Long? = null,
    val locationId: Long? = null,
    val involvedNpcIds: List<Long> = emptyList(),
    val reward: String = "",
    val notes: String = "",
)

data class QuestResponse(
    val id: Long,
    val campaignId: Long,
    val title: String,
    val description: String,
    val hook: String,
    val objective: String,
    val status: QuestStatus,
    val questGiver: EntityRef?,
    val location: EntityRef?,
    val involvedNpcs: List<EntityRef>,
    val reward: String,
    val notes: String,
    val aiGenerated: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)


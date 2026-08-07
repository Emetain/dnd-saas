package com.dndsaas.repository

import com.dndsaas.domain.Quest
import com.dndsaas.domain.QuestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QuestRepository : JpaRepository<Quest, Long> {
    fun findByCampaignIdOrderByTitleAsc(campaignId: Long): List<Quest>
    fun countByCampaignId(campaignId: Long): Long
    fun findByCampaignIdAndStatusOrderByTitleAsc(campaignId: Long, status: QuestStatus): List<Quest>
    fun findByCampaignIdAndStatusInOrderByTitleAsc(campaignId: Long, statuses: Collection<QuestStatus>): List<Quest>
}


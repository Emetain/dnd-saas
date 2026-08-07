package com.dndsaas.repository

import com.dndsaas.domain.WorldEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorldEventRepository : JpaRepository<WorldEvent, Long> {
    fun findByCampaignIdOrderByCreatedAtAsc(campaignId: Long): List<WorldEvent>
    fun countByCampaignId(campaignId: Long): Long
}


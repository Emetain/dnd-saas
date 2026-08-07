package com.dndsaas.repository

import com.dndsaas.domain.GenerationLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface GenerationLogRepository : JpaRepository<GenerationLog, Long> {

    fun findByCampaignIdOrderByCreatedAtDesc(campaignId: Long): List<GenerationLog>

    fun countByCampaignId(campaignId: Long): Long

    /** Total provider tokens consumed by a campaign, or null if none yet. */
    @Query("SELECT SUM(g.totalTokens) FROM GenerationLog g WHERE g.campaign.id = :campaignId")
    fun sumTotalTokensByCampaignId(campaignId: Long): Long?

    /** Total platform tokens charged to a campaign, or null if none yet. */
    @Query("SELECT SUM(g.tokenCost) FROM GenerationLog g WHERE g.campaign.id = :campaignId")
    fun sumTokenCostByCampaignId(campaignId: Long): Long?
}


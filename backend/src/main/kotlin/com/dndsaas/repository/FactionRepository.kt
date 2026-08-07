package com.dndsaas.repository

import com.dndsaas.domain.Faction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FactionRepository : JpaRepository<Faction, Long> {
    fun findByCampaignIdOrderByNameAsc(campaignId: Long): List<Faction>
    fun countByCampaignId(campaignId: Long): Long
}


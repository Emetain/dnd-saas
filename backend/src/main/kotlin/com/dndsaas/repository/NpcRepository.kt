package com.dndsaas.repository

import com.dndsaas.domain.Npc
import com.dndsaas.domain.NpcStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NpcRepository : JpaRepository<Npc, Long> {
    fun findByCampaignIdOrderByNameAsc(campaignId: Long): List<Npc>
    fun countByCampaignId(campaignId: Long): Long
    fun findByCampaignIdAndStatusOrderByNameAsc(campaignId: Long, status: NpcStatus): List<Npc>
    fun findByLocationIdOrderByNameAsc(locationId: Long): List<Npc>
    fun findByFactionIdOrderByNameAsc(factionId: Long): List<Npc>
}


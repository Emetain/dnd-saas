package com.dndsaas.repository

import com.dndsaas.domain.Encounter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EncounterRepository : JpaRepository<Encounter, Long> {
    fun findByCampaignIdOrderByTitleAsc(campaignId: Long): List<Encounter>
    fun countByCampaignId(campaignId: Long): Long
    fun findBySessionIdOrderByTitleAsc(sessionId: Long): List<Encounter>
}


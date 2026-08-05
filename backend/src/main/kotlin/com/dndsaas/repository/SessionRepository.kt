package com.dndsaas.repository

import com.dndsaas.domain.Session
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SessionRepository : JpaRepository<Session, Long> {
    fun findByCampaignIdOrderBySessionNumberAsc(campaignId: Long): List<Session>
}


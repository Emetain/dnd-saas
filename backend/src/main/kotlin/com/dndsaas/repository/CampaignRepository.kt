package com.dndsaas.repository

import com.dndsaas.domain.Campaign
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CampaignRepository : JpaRepository<Campaign, Long> {
    fun findByUserIdOrderByUpdatedAtDesc(userId: Long): List<Campaign>
}

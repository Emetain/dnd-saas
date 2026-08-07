package com.dndsaas.repository

import com.dndsaas.domain.Location
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LocationRepository : JpaRepository<Location, Long> {
    fun findByCampaignIdOrderByNameAsc(campaignId: Long): List<Location>
    fun countByCampaignId(campaignId: Long): Long
    fun findByCampaignIdAndParentIsNullOrderByNameAsc(campaignId: Long): List<Location>
    fun findByParentIdOrderByNameAsc(parentId: Long): List<Location>
}


package com.dndsaas.repository

import com.dndsaas.domain.Relationship
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RelationshipRepository : JpaRepository<Relationship, Long> {
    fun findByCampaignId(campaignId: Long): List<Relationship>
    fun countByCampaignId(campaignId: Long): Long
    fun findByFromNpcIdOrToNpcId(fromNpcId: Long, toNpcId: Long): List<Relationship>
}


package com.dndsaas.repository

import com.dndsaas.domain.Item
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ItemRepository : JpaRepository<Item, Long> {
    fun findByCampaignIdOrderByNameAsc(campaignId: Long): List<Item>
    fun countByCampaignId(campaignId: Long): Long
    fun findByOwnerCharacterIdOrderByNameAsc(characterId: Long): List<Item>
}


package com.dndsaas.repository

import com.dndsaas.domain.Character
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Data-access layer. Spring Data JPA generates the implementation.
 */
@Repository
interface CharacterRepository : JpaRepository<Character, Long> {

    /** The party roster of a campaign, ordered by name. */
    fun findByCampaignIdOrderByNameAsc(campaignId: Long): List<Character>

    fun countByCampaignId(campaignId: Long): Long
}

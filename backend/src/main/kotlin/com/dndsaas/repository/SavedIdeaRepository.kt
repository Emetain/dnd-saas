package com.dndsaas.repository

import com.dndsaas.domain.CampaignKind
import com.dndsaas.domain.SavedIdea
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SavedIdeaRepository : JpaRepository<SavedIdea, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<SavedIdea>
    fun findByUserIdAndKindOrderByCreatedAtDesc(userId: Long, kind: CampaignKind): List<SavedIdea>
}

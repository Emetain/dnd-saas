package com.dndsaas.repository

import com.dndsaas.domain.AdView
import com.dndsaas.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface AdViewRepository : JpaRepository<AdView, Long> {
    @Query("SELECT COUNT(a) FROM AdView a WHERE a.user = ?1 AND a.adId = ?2")
    fun countViewsByUserAndAdId(user: User, adId: String): Long
    
    @Query("SELECT COUNT(a) FROM AdView a WHERE a.user = ?1 AND a.createdAt >= ?2")
    fun countViewsSince(user: User, since: Instant): Long
    
    @Query("SELECT COALESCE(SUM(a.tokensEarned), 0) FROM AdView a WHERE a.user = ?1")
    fun sumTokensEarnedByUser(user: User): Long
}


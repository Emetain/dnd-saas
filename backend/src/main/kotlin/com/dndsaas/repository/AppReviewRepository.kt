package com.dndsaas.repository

import com.dndsaas.domain.AppReview
import com.dndsaas.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AppReviewRepository : JpaRepository<AppReview, Long> {
    @Query("SELECT COUNT(r) FROM AppReview r WHERE r.user = ?1 AND r.hasBeenRewarded = true")
    fun countRewardedReviewsByUser(user: User): Long
    
    @Query("SELECT r FROM AppReview r WHERE r.user = ?1 AND r.platformId = ?2")
    fun findByUserAndPlatform(user: User, platformId: String): AppReview?
}


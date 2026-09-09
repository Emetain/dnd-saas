package com.dndsaas.repository

import com.dndsaas.domain.Referral
import com.dndsaas.domain.ReferralStatus
import com.dndsaas.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ReferralRepository : JpaRepository<Referral, Long> {
    fun findByReferralCode(code: String): Referral?
    
    @Query("SELECT r FROM Referral r WHERE r.referrer = ?1 AND r.status = ?2")
    fun findByReferrerAndStatus(referrer: User, status: ReferralStatus): List<Referral>
    
    @Query("SELECT COUNT(r) FROM Referral r WHERE r.referrer = ?1 AND r.status = ?2")
    fun countByReferrerAndStatus(referrer: User, status: ReferralStatus): Long
    
    @Query("SELECT COALESCE(SUM(r.tokensAwarded), 0) FROM Referral r WHERE r.referrer = ?1 AND r.referrerRewarded = true")
    fun sumTokensAwardedToReferrer(referrer: User): Long
}


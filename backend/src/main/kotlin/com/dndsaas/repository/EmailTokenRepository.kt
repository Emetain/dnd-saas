package com.dndsaas.repository

import com.dndsaas.domain.EmailToken
import com.dndsaas.domain.EmailTokenPurpose
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface EmailTokenRepository : JpaRepository<EmailToken, Long> {
    fun findByTokenHashAndPurpose(tokenHash: String, purpose: EmailTokenPurpose): EmailToken?

    /** The newest link of a kind, to stop users requesting a flood of emails. */
    fun findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId: Long, purpose: EmailTokenPurpose): EmailToken?

    @Modifying
    @Query("delete from EmailToken t where t.expiresAt < :now")
    fun deleteExpired(now: Instant): Int
}

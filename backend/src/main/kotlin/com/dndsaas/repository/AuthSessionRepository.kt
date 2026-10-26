package com.dndsaas.repository

import com.dndsaas.domain.AuthSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface AuthSessionRepository : JpaRepository<AuthSession, Long> {
    fun findByTokenHash(tokenHash: String): AuthSession?

    fun deleteByTokenHash(tokenHash: String)

    /** Signs a user out everywhere except the session with [keepTokenHash]. */
    @Modifying
    @Query("delete from AuthSession s where s.user.id = :userId and s.tokenHash <> :keepTokenHash")
    fun deleteOtherSessions(userId: Long, keepTokenHash: String): Int

    @Modifying
    @Query("delete from AuthSession s where s.expiresAt < :now")
    fun deleteExpired(now: Instant): Int
}

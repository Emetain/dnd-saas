package com.dndsaas.repository

import com.dndsaas.domain.TokenTransaction
import com.dndsaas.domain.TokenTransactionType
import com.dndsaas.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface TokenTransactionRepository : JpaRepository<TokenTransaction, Long> {
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TokenTransaction t WHERE t.user = ?1")
    fun sumTokensByUser(user: User): Long
    
    @Query("SELECT t FROM TokenTransaction t WHERE t.user = ?1 ORDER BY t.createdAt DESC")
    fun findByUserOrderByNewest(user: User): List<TokenTransaction>
    
    @Query("SELECT t FROM TokenTransaction t WHERE t.user = ?1 AND t.type = ?2 ORDER BY t.createdAt DESC")
    fun findByUserAndType(user: User, type: TokenTransactionType): List<TokenTransaction>
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TokenTransaction t WHERE t.user = ?1 AND t.createdAt >= ?2")
    fun sumTokensSince(user: User, since: Instant): Long
}


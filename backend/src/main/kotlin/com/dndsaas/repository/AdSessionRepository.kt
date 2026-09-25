package com.dndsaas.repository

import com.dndsaas.domain.AdSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AdSessionRepository : JpaRepository<AdSession, Long> {
    fun findByTicket(ticket: String): AdSession?
}

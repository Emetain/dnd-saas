package com.dndsaas.repository

import com.dndsaas.domain.StripeEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StripeEventRepository : JpaRepository<StripeEvent, Long> {
    fun existsByEventId(eventId: String): Boolean
}

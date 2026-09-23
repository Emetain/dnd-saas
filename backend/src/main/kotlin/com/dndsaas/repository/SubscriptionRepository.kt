package com.dndsaas.repository

import com.dndsaas.domain.Subscription
import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface SubscriptionRepository : JpaRepository<Subscription, Long> {
    fun findByUserAndIsActiveTrue(user: User): Subscription?
    
    @Query("SELECT s FROM Subscription s WHERE s.user = ?1 AND s.isActive = true AND s.tier = ?2")
    fun findActiveByUserAndTier(user: User, tier: SubscriptionTier): Subscription?
    
    /** Cycles our own scheduler renews; Stripe-billed subscriptions are renewed by Stripe's invoices. */
    @Query("SELECT s FROM Subscription s WHERE s.billingCycleEnd < ?1 AND s.isActive = true AND s.stripeSubscriptionId IS NULL")
    fun findExpiredBillingCycles(now: Instant): List<Subscription>

    fun findByStripeSubscriptionId(stripeSubscriptionId: String): Subscription?
}


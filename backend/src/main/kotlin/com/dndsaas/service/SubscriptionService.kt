package com.dndsaas.service

import com.dndsaas.domain.Subscription
import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.domain.TokenTransactionType
import com.dndsaas.dto.SubscriptionDetailsResponse
import com.dndsaas.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service for managing user subscriptions and billing cycles.
 *
 * Handles subscription tier upgrades, billing cycle management, and
 * monthly token allowance allocation.
 */
@Service
@Transactional
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val userService: UserService,
    private val tokenService: TokenService,
) {

    /**
     * Upgrade a user to a new subscription tier.
     */
    fun upgradeTier(userId: Long, newTier: SubscriptionTier) {
        val user = userService.findEntity(userId)
        
        if (user.subscriptionTier == newTier) {
            return // Already on this tier
        }

        // Deactivate old subscription
        val oldSubscription = subscriptionRepository.findByUserAndIsActiveTrue(user)
        if (oldSubscription != null) {
            oldSubscription.isActive = false
            subscriptionRepository.save(oldSubscription)
        }

        // Create new subscription
        val now = Instant.now()
        val billingCycleEnd = now.plus(30, ChronoUnit.DAYS)

        val newSubscription = Subscription(
            user = user,
            tier = newTier,
            billingCycleStart = now,
            billingCycleEnd = billingCycleEnd,
            isActive = true,
        )

        subscriptionRepository.save(newSubscription)

        // Update user's tier
        user.subscriptionTier = newTier
        userService.updateSubscriptionTier(userId, newTier)

        // Grant monthly tokens (only if not a free tier upgrade)
        if (newTier != SubscriptionTier.FREE) {
            tokenService.recordTransaction(
                userId = userId,
                type = TokenTransactionType.SUBSCRIPTION_GRANT,
                amount = newTier.monthlyTokens,
                description = "Monthly token allowance for ${newTier.label}",
            )
        }
    }

    /**
     * Get details about a user's current subscription.
     */
    fun getSubscriptionDetails(userId: Long): SubscriptionDetailsResponse {
        val user = userService.findEntity(userId)
        val subscription = subscriptionRepository.findByUserAndIsActiveTrue(user)

        val billingStart = subscription?.billingCycleStart ?: Instant.now()
        val billingEnd = subscription?.billingCycleEnd ?: Instant.now().plus(30, ChronoUnit.DAYS)

        // Calculate tokens used this month
        val chargedThisMonth = tokenService.getBalanceDetails(userId).tokensUsedThisMonth

        return SubscriptionDetailsResponse(
            userId = userId,
            currentTier = user.subscriptionTier,
            monthlyTokenAllowance = user.subscriptionTier.monthlyTokens,
            tokensUsedThisMonth = chargedThisMonth,
            billingCycleStart = billingStart,
            billingCycleEnd = billingEnd,
            nextBillingDate = billingEnd,
        )
    }

    /**
     * Process expired billing cycles and reset monthly tokens.
     * Should be called by a scheduled task.
     */
    fun processExpiredBillingCycles() {
        val now = Instant.now()
        val expiredSubscriptions = subscriptionRepository.findExpiredBillingCycles(now)

        expiredSubscriptions.forEach { subscription ->
            // Start a new billing cycle
            val newEnd = now.plus(30, ChronoUnit.DAYS)
            subscription.billingCycleStart = now
            subscription.billingCycleEnd = newEnd

            subscriptionRepository.save(subscription)

            // Grant new month's tokens
            if (subscription.tier != SubscriptionTier.FREE) {
                tokenService.recordTransaction(
                    userId = subscription.user!!.id!!,
                    type = TokenTransactionType.SUBSCRIPTION_GRANT,
                    amount = subscription.tier.monthlyTokens,
                    description = "Monthly token allowance for ${subscription.tier.label}",
                )
            }
        }
    }

    /**
     * Downgrade a user to free tier (e.g., when subscription expires or is cancelled).
     */
    fun downgradeToFree(userId: Long) {
        upgradeTier(userId, SubscriptionTier.FREE)
    }
}


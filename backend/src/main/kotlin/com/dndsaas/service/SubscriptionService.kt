package com.dndsaas.service

import com.dndsaas.domain.Subscription
import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.domain.TokenTransactionType
import com.dndsaas.dto.SubscriptionDetailsResponse
import com.dndsaas.repository.SubscriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneOffset

/**
 * Service for managing user subscriptions and billing cycles.
 *
 * Handles subscription tier changes, billing cycle management, and
 * monthly token allowance allocation.
 *
 * Tier changes follow the usual SaaS rules:
 *  - an **upgrade** applies immediately (a paid tier from Free starts a new
 *    cycle with a full month; paid → higher paid grants only the difference);
 *  - a **downgrade** is scheduled for the end of the cycle, so the user keeps
 *    what they paid for — and cannot farm tokens by downgrading and re-upgrading.
 */
@Service
@Transactional
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val userService: UserService,
    private val tokenService: TokenService,
) {

    private val log = LoggerFactory.getLogger(SubscriptionService::class.java)

    /**
     * Change a user's subscription tier. Upgrades apply now; downgrades
     * (including to Free) take effect when the current cycle ends.
     */
    fun upgradeTier(userId: Long, newTier: SubscriptionTier) {
        val user = userService.findEntity(userId)
        val current = user.subscriptionTier
        val subscription = subscriptionRepository.findByUserAndIsActiveTrue(user)
        // A plan that Stripe bills is only ever changed through Stripe (the Customer Portal).
        if (subscription?.stripeSubscriptionId != null) {
            throw AlreadySubscribedException("Change your plan with “Manage subscription”")
        }

        when {
            // Same tier: cancels a scheduled downgrade, grants nothing.
            newTier == current -> subscription?.pendingTier = null

            // Downgrade: keep the paid tier until the cycle ends.
            newTier < current -> subscription?.pendingTier = newTier

            // Free -> paid: a new cycle starts with a full month of tokens.
            subscription == null -> {
                val now = Instant.now()
                subscriptionRepository.save(
                    Subscription(user = user, tier = newTier, billingCycleStart = now, billingCycleEnd = oneMonthAfter(now)),
                )
                userService.updateSubscriptionTier(userId, newTier)
                tokenService.renewAllowance(userId, newTier)
            }

            // Paid -> higher paid: same cycle, top up by the difference.
            else -> {
                subscription.tier = newTier
                subscription.pendingTier = null
                userService.updateSubscriptionTier(userId, newTier)
                val topUp = minOf(newTier.monthlyTokens - current.monthlyTokens, newTier.allowanceCap - user.allowanceTokens)
                if (topUp > 0) {
                    tokenService.recordTransaction(
                        userId = userId,
                        type = TokenTransactionType.SUBSCRIPTION_GRANT,
                        amount = topUp,
                        description = "Upgrade from ${current.label} to ${newTier.label}",
                    )
                }
            }
        }
    }

    /**
     * Get details about a user's current subscription.
     */
    fun getSubscriptionDetails(userId: Long): SubscriptionDetailsResponse {
        val user = userService.findEntity(userId)
        val subscription = subscriptionRepository.findByUserAndIsActiveTrue(user)
        val balance = tokenService.getBalanceDetails(userId)

        return SubscriptionDetailsResponse(
            userId = userId,
            currentTier = user.subscriptionTier,
            managedByStripe = subscription?.stripeSubscriptionId != null,
            pendingTier = subscription?.pendingTier,
            monthlyTokenAllowance = user.subscriptionTier.monthlyTokens,
            allowanceCap = user.subscriptionTier.allowanceCap,
            tokensUsedThisMonth = balance.tokensUsedThisMonth,
            // Free users have no billing cycle.
            billingCycleStart = subscription?.billingCycleStart,
            billingCycleEnd = subscription?.billingCycleEnd,
            nextBillingDate = subscription?.billingCycleEnd,
        )
    }

    /**
     * Renews every subscription whose cycle has ended: applies a scheduled
     * downgrade, then rolls the allowance over. Called by [BillingScheduler].
     */
    fun processExpiredBillingCycles() {
        val now = Instant.now()
        subscriptionRepository.findExpiredBillingCycles(now).forEach { subscription ->
            val userId = subscription.user!!.id!!

            subscription.pendingTier?.let { pending ->
                subscription.tier = pending
                subscription.pendingTier = null
                userService.updateSubscriptionTier(userId, pending)
            }

            if (subscription.tier == SubscriptionTier.FREE) {
                // Leftover allowance stays usable; Free simply gets no new grant.
                subscription.isActive = false
                log.info("User {} moved to Free at the end of their billing cycle", userId)
                return@forEach
            }

            // Catch up on missed cycles (e.g. after downtime) without granting twice.
            while (subscription.billingCycleEnd <= now) {
                subscription.billingCycleStart = subscription.billingCycleEnd
                subscription.billingCycleEnd = oneMonthAfter(subscription.billingCycleStart)
            }
            tokenService.renewAllowance(userId, subscription.tier)
            log.info("Renewed {} subscription for user {}", subscription.tier.label, userId)
        }
    }

    // -----------------------------------------------------------------------
    // Stripe-billed subscriptions. Stripe decides when a plan starts, changes
    // and ends; these apply what its webhooks report.
    // -----------------------------------------------------------------------

    /**
     * Stripe created or changed a subscription (new plan, upgrade, downgrade that
     * took effect, cancellation scheduled or undone). Upgrades top up the allowance
     * by the difference, just like [upgradeTier]; the first month's tokens come with
     * the first paid invoice ([stripeInvoicePaid]).
     */
    fun applyStripeSubscription(
        userId: Long,
        stripeSubscriptionId: String,
        tier: SubscriptionTier,
        periodStart: Instant,
        periodEnd: Instant,
        cancelAtPeriodEnd: Boolean,
    ) {
        val user = userService.findEntity(userId)
        val previous = user.subscriptionTier

        val subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
            ?: run {
                // Replace any subscription from the free test mode.
                subscriptionRepository.findByUserAndIsActiveTrue(user)?.isActive = false
                subscriptionRepository.save(Subscription(user = user, stripeSubscriptionId = stripeSubscriptionId))
            }
        subscription.tier = tier
        subscription.isActive = true
        subscription.billingCycleStart = periodStart
        subscription.billingCycleEnd = periodEnd
        subscription.pendingTier = if (cancelAtPeriodEnd) SubscriptionTier.FREE else null
        userService.updateSubscriptionTier(userId, tier)

        if (previous != SubscriptionTier.FREE && tier > previous) {
            val topUp = minOf(tier.monthlyTokens - previous.monthlyTokens, tier.allowanceCap - user.allowanceTokens)
            if (topUp > 0) {
                tokenService.recordTransaction(
                    userId = userId,
                    type = TokenTransactionType.SUBSCRIPTION_GRANT,
                    amount = topUp,
                    description = "Upgrade from ${previous.label} to ${tier.label}",
                )
            }
        }
        log.info("Stripe subscription {} for user {}: {} -> {}", stripeSubscriptionId, userId, previous.label, tier.label)
    }

    /** A subscription invoice was paid: the first month or a renewal grants the monthly allowance. */
    fun stripeInvoicePaid(userId: Long, billingReason: String?) {
        if (billingReason != "subscription_create" && billingReason != "subscription_cycle") return
        val tier = userService.findEntity(userId).subscriptionTier
        if (tier != SubscriptionTier.FREE) tokenService.renewAllowance(userId, tier)
    }

    /** Stripe ended the subscription (cancelled at period end, or unpaid). */
    fun endStripeSubscription(stripeSubscriptionId: String) {
        val subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId) ?: return
        subscription.isActive = false
        subscription.pendingTier = null
        val userId = subscription.user!!.id!!
        userService.updateSubscriptionTier(userId, SubscriptionTier.FREE)
        log.info("Stripe subscription {} ended; user {} is on Free", stripeSubscriptionId, userId)
    }

    /**
     * Downgrade a user to free tier at the end of the current cycle.
     */
    fun downgradeToFree(userId: Long) {
        upgradeTier(userId, SubscriptionTier.FREE)
    }

    private fun oneMonthAfter(instant: Instant): Instant =
        instant.atZone(ZoneOffset.UTC).plusMonths(1).toInstant()
}

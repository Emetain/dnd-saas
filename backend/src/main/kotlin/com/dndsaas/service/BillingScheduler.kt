package com.dndsaas.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Runs the monthly subscription renewal.
 *
 * Checks hourly by default; override `app.billing.renewal-cron` (e.g. to
 * every few seconds) when testing renewals locally.
 */
@Component
class BillingScheduler(
    private val subscriptionService: SubscriptionService,
) {

    @Scheduled(cron = "\${app.billing.renewal-cron:0 0 * * * *}")
    fun renewExpiredSubscriptions() = subscriptionService.processExpiredBillingCycles()
}

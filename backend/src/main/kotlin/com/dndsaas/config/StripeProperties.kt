package com.dndsaas.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Stripe settings, bound from `app.stripe.*` in application.yml.
 *
 * Like the OpenAI key, the secrets only ever come from environment variables
 * (STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET). Without a secret key, payments
 * are switched off and plan changes stay free for testing.
 */
@ConfigurationProperties(prefix = "app.stripe")
data class StripeProperties(
    val secretKey: String = "",
    val webhookSecret: String = "",
    /** Where Stripe sends the user back after checkout or the customer portal. */
    val returnBaseUrl: String = "http://localhost:5173",
) {
    val isConfigured: Boolean
        get() = secretKey.isNotBlank()

    /** Test-mode keys start with sk_test_; handy to show in logs and the UI. */
    val isTestMode: Boolean
        get() = secretKey.startsWith("sk_test_")
}

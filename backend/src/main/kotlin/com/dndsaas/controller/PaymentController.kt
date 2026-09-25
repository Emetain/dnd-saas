package com.dndsaas.controller

import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.domain.TokenPack
import com.dndsaas.service.StripeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Payments: Stripe Checkout for plans and token packs, the Customer Portal,
 * and the webhook through which Stripe reports payments.
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "Payments", description = "Stripe Checkout, Customer Portal and webhooks")
class PaymentController(
    private val stripeService: StripeService,
) {

    @GetMapping("/billing/config")
    @Operation(summary = "Whether real payments are switched on (otherwise plan changes are free for testing)")
    fun config(): Map<String, Boolean> =
        mapOf("paymentsEnabled" to stripeService.paymentsEnabled, "testMode" to stripeService.testMode)

    @PostMapping("/users/{userId}/billing/checkout")
    @Operation(
        summary = "Start a Stripe Checkout for a plan or a token pack",
        description = "Send either `tier` (PRO, PRO_PLUS, ULTIMATE_DM) or `pack` (SMALL, MEDIUM, LARGE). " +
            "Returns the Stripe URL to redirect to. Tokens are granted when Stripe confirms the payment.",
    )
    fun checkout(@PathVariable userId: Long, @RequestBody request: CheckoutRequest): RedirectResponse {
        val url = when {
            request.tier != null -> stripeService.checkoutForPlan(userId, request.tier)
            request.pack != null -> stripeService.checkoutForPack(userId, request.pack)
            else -> throw IllegalArgumentException("Choose a plan (tier) or a token pack (pack)")
        }
        return RedirectResponse(url)
    }

    @PostMapping("/users/{userId}/billing/portal")
    @Operation(summary = "Open the Stripe Customer Portal to change or cancel the plan")
    fun portal(@PathVariable userId: Long): RedirectResponse = RedirectResponse(stripeService.portal(userId))

    @PostMapping("/stripe/webhook")
    @Operation(summary = "Stripe webhook (called by Stripe, verified by signature)")
    fun webhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature", required = false) signature: String?,
    ): ResponseEntity<Void> {
        stripeService.handleWebhook(payload, signature)
        return ResponseEntity.ok().build()
    }
}

data class CheckoutRequest(
    val tier: SubscriptionTier? = null,
    val pack: TokenPack? = null,
)

data class RedirectResponse(val url: String)

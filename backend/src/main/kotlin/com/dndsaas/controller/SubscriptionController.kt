package com.dndsaas.controller

import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.dto.SubscriptionDetailsResponse
import com.dndsaas.orchestration.BillingOrchestrator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST endpoints for subscription management and billing.
 */
@RestController
@RequestMapping("/v1/users/{userId}/subscription")
@Tag(name = "Subscriptions", description = "Subscription tier management and billing")
class SubscriptionController(
    private val billingOrchestrator: BillingOrchestrator,
) {

    @GetMapping
    @Operation(
        summary = "Get subscription details",
    )
    fun getDetails(@PathVariable userId: Long): SubscriptionDetailsResponse =
        billingOrchestrator.getSubscriptionDetails(userId)

    @PostMapping("/upgrade")
    @Operation(
        summary = "Upgrade subscription tier",
    )
    fun upgrade(
        @PathVariable userId: Long,
        @RequestBody request: UpgradeTierRequest,
    ): ResponseEntity<SubscriptionDetailsResponse> {
        billingOrchestrator.upgradeSubscription(userId, request.tier)
        return ResponseEntity.ok(billingOrchestrator.getSubscriptionDetails(userId))
    }

    @PostMapping("/downgrade-free")
    @Operation(
        summary = "Downgrade to free tier",
    )
    fun downgradeToFree(@PathVariable userId: Long): ResponseEntity<SubscriptionDetailsResponse> {
        billingOrchestrator.downgradeToFree(userId)
        return ResponseEntity.ok(billingOrchestrator.getSubscriptionDetails(userId))
    }
}

data class UpgradeTierRequest(
    val tier: SubscriptionTier,
)


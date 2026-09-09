package com.dndsaas.controller

import com.dndsaas.dto.TokenBalanceResponse
import com.dndsaas.dto.TokenTransactionResponse
import com.dndsaas.orchestration.BillingOrchestrator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * REST endpoints for token management and balance.
 */
@RestController
@RequestMapping("/v1/users/{userId}/tokens")
@Tag(name = "Tokens", description = "Token balance and transaction history")
class TokenController(
    private val billingOrchestrator: BillingOrchestrator,
) {

    @GetMapping
    @Operation(
        summary = "Get current token balance and recent transactions",
    )
    fun getBalance(@PathVariable userId: Long): TokenBalanceResponse =
        billingOrchestrator.getTokenBalance(userId)

    @GetMapping("/history")
    @Operation(
        summary = "Get detailed token transaction history",
    )
    fun getHistory(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "50") limit: Int,
    ): List<TokenTransactionResponse> =
        billingOrchestrator.getTokenHistory(userId, limit)
}


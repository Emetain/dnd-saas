package com.dndsaas.controller

import com.dndsaas.dto.UserRegistrationRequest
import com.dndsaas.dto.UserResponse
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
 * REST endpoints for user account management.
 */
@RestController
@RequestMapping("/v1/users")
@Tag(name = "User Management", description = "User account registration and profile")
class UserController(
    private val billingOrchestrator: BillingOrchestrator,
) {

    @PostMapping("/register")
    @Operation(
        summary = "Register a new user account",
        description = "Creates a new user account. Free tier users start with 50 tokens.",
    )
    fun register(@RequestBody request: UserRegistrationRequest): ResponseEntity<UserResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(billingOrchestrator.registerUser(request))

    @GetMapping("/{userId}")
    @Operation(
        summary = "Get user account information",
    )
    fun getAccount(@PathVariable userId: Long): UserResponse =
        billingOrchestrator.getUserAccount(userId)
}


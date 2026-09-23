package com.dndsaas.service

import com.dndsaas.domain.User
import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.dto.UserResponse
import com.dndsaas.dto.UserRegistrationRequest
import com.dndsaas.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing user accounts.
 *
 * Handles account creation, lookup, and basic user data management.
 */
@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
) {

    /**
     * Register a new user account.
     *
     * New free users start with 50 platform tokens.
     */
    fun register(request: UserRegistrationRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already registered: ${request.email}")
        }

        val user = User(
            email = request.email,
            displayName = request.displayName,
            passwordHash = hashPassword(request.password), // TODO: use proper password hashing (bcrypt)
            subscriptionTier = SubscriptionTier.FREE,
            allowanceTokens = 50, // Free tier starting tokens
        )

        val saved = userRepository.save(user)
        return toResponse(saved)
    }

    /**
     * Find a user by ID.
     */
    fun findEntity(id: Long): User =
        userRepository.findById(id)
            .orElseThrow { NoSuchElementException("User $id not found") }

    /**
     * Look up a user by email address.
     */
    fun findByEmail(email: String): User? =
        userRepository.findByEmail(email)

    /**
     * Get user response by ID.
     */
    fun get(id: Long): UserResponse =
        toResponse(findEntity(id))

    /**
     * Update a user's subscription tier.
     */
    fun updateSubscriptionTier(userId: Long, tier: SubscriptionTier) {
        val user = findEntity(userId)
        user.subscriptionTier = tier
        userRepository.save(user)
    }

    private fun toResponse(user: User): UserResponse =
        UserResponse(
            id = user.id!!,
            email = user.email,
            displayName = user.displayName,
            subscriptionTier = user.subscriptionTier,
            platformTokens = user.totalTokens,
            allowanceTokens = user.allowanceTokens,
            purchasedTokens = user.purchasedTokens,
            createdAt = user.createdAt,
        )

    private fun hashPassword(password: String): String {
        // TODO: Replace with proper bcrypt hashing
        return password // Placeholder
    }
}


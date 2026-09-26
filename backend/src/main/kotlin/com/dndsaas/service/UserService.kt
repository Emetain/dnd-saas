package com.dndsaas.service

import com.dndsaas.domain.User
import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.dto.UserResponse
import com.dndsaas.dto.UserRegistrationRequest
import com.dndsaas.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
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
    private val passwordEncoder: PasswordEncoder,
) {

    /**
     * Register a new user account.
     *
     * New free users start with 50 platform tokens.
     */
    fun register(request: UserRegistrationRequest): UserResponse {
        val email = request.email.trim().lowercase()
        val displayName = request.displayName.trim()
        require(EMAIL.matches(email)) { "Enter a valid email address" }
        require(displayName.length in 1..MAX_NAME_LENGTH) { "Choose a display name of 1 to $MAX_NAME_LENGTH characters" }
        checkPasswordRules(request.password)
        require(!userRepository.existsByEmailIgnoreCase(email)) { "There is already an account with this email — sign in instead" }

        val user = User(
            email = email,
            displayName = displayName,
            passwordHash = passwordEncoder.encode(request.password),
            emailVerified = false,
            subscriptionTier = SubscriptionTier.FREE,
            allowanceTokens = 50, // Free tier starting tokens
        )

        val saved = userRepository.save(user)
        return toResponse(saved)
    }

    /** At least 8 characters, with a capital letter, a number and a special character. */
    fun checkPasswordRules(password: String) {
        require(password.length >= MIN_PASSWORD_LENGTH) { "Choose a password of at least $MIN_PASSWORD_LENGTH characters" }
        require(password.length <= MAX_PASSWORD_LENGTH) { "That password is too long" }
        require(password.any(Char::isUpperCase)) { "Add at least one capital letter to your password" }
        require(password.any(Char::isDigit)) { "Add at least one number to your password" }
        require(password.any { it in SPECIAL_CHARACTERS }) { "Add at least one of these special characters: $SPECIAL_CHARACTERS" }
    }

    fun updateDisplayName(userId: Long, displayName: String): UserResponse {
        val name = displayName.trim()
        require(name.length in 1..MAX_NAME_LENGTH) { "Choose a display name of 1 to $MAX_NAME_LENGTH characters" }
        val user = findEntity(userId)
        user.displayName = name
        return toResponse(userRepository.save(user))
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
            emailVerified = user.emailVerified,
        )

    private companion object {
        val EMAIL = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")
        const val MAX_NAME_LENGTH = 50
        const val MIN_PASSWORD_LENGTH = 8

        /** The characters that count as "special" — the frontend lists the same ones (utils/password.js). */
        const val SPECIAL_CHARACTERS = "!@#$%^&*()-_=+[]{}?.,:;"

        /** BCrypt only looks at the first 72 bytes. */
        const val MAX_PASSWORD_LENGTH = 72
    }
}


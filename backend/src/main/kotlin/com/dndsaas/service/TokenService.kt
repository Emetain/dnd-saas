package com.dndsaas.service

import com.dndsaas.domain.User
import com.dndsaas.domain.TokenTransaction
import com.dndsaas.domain.TokenTransactionType
import com.dndsaas.dto.TokenBalanceResponse
import com.dndsaas.dto.TokenTransactionResponse
import com.dndsaas.repository.TokenTransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.YearMonth

/**
 * Service for managing platform tokens.
 *
 * Handles token transactions, balance updates, and auditing.
 * Phase 5 uses tokens as the billing unit — every generation costs tokens,
 * and subscriptions grant monthly token allowances.
 */
@Service
@Transactional
class TokenService(
    private val tokenTransactionRepository: TokenTransactionRepository,
    private val userService: UserService,
) {

    /**
     * Record a token transaction (gain or loss).
     */
    fun recordTransaction(
        userId: Long,
        type: TokenTransactionType,
        amount: Long,
        description: String = "",
        campaignId: Long? = null,
        generationLogId: Long? = null,
    ): TokenTransactionResponse {
        val user = userService.findEntity(userId)
        
        val transaction = TokenTransaction(
            user = user,
            type = type,
            amount = amount,
            description = description,
            campaignId = campaignId,
            generationLogId = generationLogId,
        )

        val saved = tokenTransactionRepository.save(transaction)
        
        // Update user's total token balance
        user.platformTokens += amount
        // This will be saved via cascade but force it explicitly
        
        return toResponse(saved)
    }

    /**
     * Charge tokens for a generation.
     * Returns false if the user doesn't have enough tokens.
     */
    fun chargeTokens(
        userId: Long,
        amount: Long,
        generationLogId: Long?,
        description: String = "Generation",
    ): Boolean {
        val user = userService.findEntity(userId)
        
        if (user.platformTokens < amount) {
            return false // Not enough tokens
        }

        recordTransaction(
            userId = userId,
            type = TokenTransactionType.GENERATION_CHARGE,
            amount = -amount,
            description = description,
            generationLogId = generationLogId,
        )
        
        return true
    }

    /**
     * Reward tokens to a user (from ads, referrals, etc).
     */
    fun rewardTokens(
        userId: Long,
        type: TokenTransactionType,
        amount: Long,
        description: String = "",
    ): TokenTransactionResponse =
        recordTransaction(
            userId = userId,
            type = type,
            amount = amount,
            description = description,
        )

    /**
     * Get current token balance for a user.
     */
    fun getBalance(userId: Long): Long {
        val user = userService.findEntity(userId)
        return user.platformTokens
    }

    /**
     * Get detailed token balance and recent transactions.
     */
    fun getBalanceDetails(userId: Long): TokenBalanceResponse {
        val user = userService.findEntity(userId)
        val monthlyAllowance = user.subscriptionTier.monthlyTokens
        
        // Calculate tokens used this month
        val thisMonthStart = Instant.now().let { now ->
            val ym = YearMonth.now()
            ym.atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        }
        
        val chargedThisMonth = tokenTransactionRepository.sumTokensSince(user, thisMonthStart)
            .let { if (it < 0) -it else 0 } // Only negative transactions (charges)
        
        return TokenBalanceResponse(
            userId = userId,
            currentTokens = user.platformTokens,
            recentTransactions = tokenTransactionRepository.findByUserOrderByNewest(user)
                .take(20)
                .map(::toResponse),
            monthlyAllowance = monthlyAllowance,
            tokensUsedThisMonth = chargedThisMonth,
        )
    }

    /**
     * Get transaction history for a user.
     */
    fun getTransactionHistory(userId: Long, limit: Int = 50): List<TokenTransactionResponse> {
        val user = userService.findEntity(userId)
        return tokenTransactionRepository.findByUserOrderByNewest(user)
            .take(limit)
            .map(::toResponse)
    }

    private fun toResponse(transaction: TokenTransaction): TokenTransactionResponse =
        TokenTransactionResponse(
            id = transaction.id!!,
            type = transaction.type.name,
            amount = transaction.amount,
            description = transaction.description,
            campaignId = transaction.campaignId,
            generationLogId = transaction.generationLogId,
            createdAt = transaction.createdAt,
        )
}


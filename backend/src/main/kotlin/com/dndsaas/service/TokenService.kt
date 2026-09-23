package com.dndsaas.service

import com.dndsaas.domain.SubscriptionTier
import com.dndsaas.domain.TokenPack
import com.dndsaas.domain.TokenTransaction
import com.dndsaas.domain.TokenTransactionType
import com.dndsaas.domain.User
import com.dndsaas.dto.TokenBalanceResponse
import com.dndsaas.dto.TokenTransactionResponse
import com.dndsaas.repository.TokenTransactionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Service for managing platform tokens.
 *
 * Handles token transactions, balance updates, and auditing.
 * Phase 5 uses tokens as the billing unit — every generation costs tokens,
 * and subscriptions grant monthly token allowances.
 *
 * A user holds tokens in two buckets:
 *  - **allowance** — the monthly subscription grant plus tokens earned from free
 *    actions. Rolls over at renewal, but never beyond the tier's cap (2× monthly).
 *  - **purchased** — bought token packs. Never reset, never capped.
 *
 * Generations spend the allowance first, so bought tokens last as long as possible.
 */
@Service
@Transactional
class TokenService(
    private val tokenTransactionRepository: TokenTransactionRepository,
    private val userService: UserService,
) {

    /**
     * Record a gain or loss of allowance tokens (grants, rewards, adjustments).
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
        user.allowanceTokens += amount
        return log(user, type, amount, description, campaignId, generationLogId)
    }

    /**
     * Reward tokens to a user (from ads, referrals, etc). Earned tokens join the
     * allowance, so they roll over under the same cap as the monthly grant.
     *
     * Repeatable rewards (ads, login streaks) share a monthly cap so free
     * earning never rivals a paid tier. Returns the tokens actually awarded.
     */
    fun rewardTokens(
        userId: Long,
        type: TokenTransactionType,
        amount: Long,
        description: String = "",
    ): Long {
        val awarded = if (type in MONTHLY_CAPPED_REWARDS) minOf(amount, remainingCappedEarnings(userId)) else amount
        if (awarded > 0) {
            recordTransaction(userId = userId, type = type, amount = awarded, description = description)
        }
        return awarded
    }

    /** How many more tokens ads and login streaks may earn this month. */
    @Transactional(readOnly = true)
    fun remainingCappedEarnings(userId: Long): Long {
        val user = userService.findEntity(userId)
        val earned = MONTHLY_CAPPED_REWARDS.sumOf { tokenTransactionRepository.sumByTypeSince(user, it, monthStart()) }
        return (MONTHLY_EARNING_CAP - earned).coerceAtLeast(0)
    }

    /**
     * Fails fast, before any AI call is made, when the user cannot afford it.
     */
    @Transactional(readOnly = true)
    fun requireBalance(userId: Long, amount: Long) {
        val balance = userService.findEntity(userId).totalTokens
        if (balance < amount) throw InsufficientTokensException(required = amount, available = balance)
    }

    /**
     * Charge tokens for a generation: allowance first, then purchased tokens.
     * Returns false if the user doesn't have enough tokens.
     */
    fun chargeTokens(
        userId: Long,
        amount: Long,
        generationLogId: Long?,
        description: String = "Generation",
        campaignId: Long? = null,
    ): Boolean {
        val user = userService.findEntity(userId)

        if (user.totalTokens < amount) {
            return false // Not enough tokens
        }

        val fromAllowance = minOf(user.allowanceTokens, amount)
        user.allowanceTokens -= fromAllowance
        user.purchasedTokens -= amount - fromAllowance

        log(user, TokenTransactionType.GENERATION_CHARGE, -amount, description, campaignId, generationLogId)
        return true
    }

    /**
     * Add a bought token pack. Available on every tier, including Free.
     *
     * TODO: call this from the payment provider's confirmation (e.g. a Stripe
     * webhook) once payments exist — until then the pack is granted without payment.
     */
    fun purchaseTokens(userId: Long, pack: TokenPack): TokenTransactionResponse {
        val user = userService.findEntity(userId)
        user.purchasedTokens += pack.tokens
        return log(user, TokenTransactionType.TOKEN_PURCHASE, pack.tokens, "Bought the ${pack.name} token pack")
    }

    /**
     * Renews the allowance: adds a month of tokens, but the result never exceeds
     * the tier's rollover cap. Allowance already above the cap (e.g. from earned
     * tokens) is trimmed back to it. Purchased tokens are never touched.
     */
    fun renewAllowance(userId: Long, tier: SubscriptionTier) {
        val user = userService.findEntity(userId)
        val renewed = minOf(user.allowanceTokens + tier.monthlyTokens, tier.allowanceCap)
        val change = renewed - user.allowanceTokens
        user.allowanceTokens = renewed

        when {
            change > 0 -> log(
                user, TokenTransactionType.SUBSCRIPTION_GRANT, change,
                "Monthly ${tier.label} allowance (rollover capped at ${tier.allowanceCap})",
            )
            change < 0 -> log(
                user, TokenTransactionType.ALLOWANCE_EXPIRED, change,
                "Allowance above the ${tier.label} rollover cap of ${tier.allowanceCap} expired",
            )
        }
    }

    /**
     * Get current token balance for a user.
     */
    fun getBalance(userId: Long): Long = userService.findEntity(userId).totalTokens

    /**
     * Get detailed token balance and recent transactions.
     */
    fun getBalanceDetails(userId: Long): TokenBalanceResponse {
        val user = userService.findEntity(userId)
        val tier = user.subscriptionTier

        val chargedThisMonth = -tokenTransactionRepository.sumByTypeSince(
            user, TokenTransactionType.GENERATION_CHARGE, monthStart(),
        )

        return TokenBalanceResponse(
            userId = userId,
            currentTokens = user.totalTokens,
            allowanceTokens = user.allowanceTokens,
            purchasedTokens = user.purchasedTokens,
            recentTransactions = tokenTransactionRepository.findByUserOrderByNewest(user)
                .take(20)
                .map(::toResponse),
            monthlyAllowance = tier.monthlyTokens,
            allowanceCap = tier.allowanceCap,
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

    private fun monthStart() = YearMonth.now(ZoneOffset.UTC).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()

    companion object {
        /** Most tokens ads and login streaks together can earn per calendar month. */
        const val MONTHLY_EARNING_CAP = 300L

        /** Referrals and reviews are one-off and bring in users, so they stay uncapped. */
        private val MONTHLY_CAPPED_REWARDS = listOf(
            TokenTransactionType.AD_VIEW_REWARD,
            TokenTransactionType.LOGIN_STREAK_REWARD,
        )
    }

    /** Writes a ledger entry. Balances are adjusted by the caller. */
    private fun log(
        user: User,
        type: TokenTransactionType,
        amount: Long,
        description: String,
        campaignId: Long? = null,
        generationLogId: Long? = null,
    ): TokenTransactionResponse {
        val transaction = TokenTransaction(
            user = user,
            type = type,
            amount = amount,
            description = description,
            campaignId = campaignId,
            generationLogId = generationLogId,
        )
        return toResponse(tokenTransactionRepository.save(transaction))
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

/** Raised when a user does not have enough platform tokens for a generation. */
class InsufficientTokensException(val required: Long, val available: Long) :
    RuntimeException("Not enough tokens: this generation costs $required, but only $available are available")

/** Raised when the user's subscription tier does not include a feature. */
class TierRestrictionException(message: String) : RuntimeException(message)

package com.dndsaas.service

import com.dndsaas.domain.Referral
import com.dndsaas.domain.ReferralStatus
import com.dndsaas.domain.TokenTransactionType
import com.dndsaas.dto.ReferralResponse
import com.dndsaas.repository.ReferralRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service for managing referral programs.
 *
 * Users can earn tokens by referring friends. Both the referrer and
 * the referred user receive rewards when the referred user completes
 * a qualifying action (e.g., first generation).
 */
@Service
@Transactional
class ReferralService(
    private val referralRepository: ReferralRepository,
    private val userService: UserService,
    private val tokenService: TokenService,
) {

    companion object {
        private const val REFERRER_REWARD = 50L // Tokens for successful referral
        private const val REFERRED_REWARD = 25L // Tokens for being referred
    }

    /**
     * Create a new referral code for a user to share.
     */
    fun generateReferralCode(userId: Long): ReferralResponse {
        val referrer = userService.findEntity(userId)
        
        val referral = Referral(
            referrer = referrer,
            referralCode = generateUniqueCode(),
            status = ReferralStatus.PENDING,
            tokensAwarded = REFERRER_REWARD,
        )

        val saved = referralRepository.save(referral)
        return toResponse(saved)
    }

    /**
     * Record a new user clicking a referral link.
     * The referred user will be linked once they complete onboarding.
     */
    fun applyReferralCode(referralCode: String, newUserId: Long) {
        val referral = referralRepository.findByReferralCode(referralCode)
            ?: throw IllegalArgumentException("Invalid referral code")

        if (referral.status != ReferralStatus.PENDING) {
            throw IllegalStateException("Referral code is no longer valid")
        }

        val referredUser = userService.findEntity(newUserId)
        
        // Update referral with the referred user
        referral.referredUser = referredUser
        referral.status = ReferralStatus.INVITED
        referralRepository.save(referral)
    }

    /**
     * Complete a referral — both users should be rewarded.
     * Called when the referred user completes their first qualifying action.
     */
    fun completeReferral(referralCode: String) {
        val referral = referralRepository.findByReferralCode(referralCode)
            ?: throw IllegalArgumentException("Invalid referral code")

        if (referral.referredUser == null) {
            throw IllegalStateException("Referral not yet linked to a user")
        }

        if (referral.status == ReferralStatus.REWARDED) {
            return // Already rewarded
        }

        // Reward the referrer
        if (!referral.referrerRewarded) {
            tokenService.rewardTokens(
                userId = referral.referrer!!.id!!,
                type = TokenTransactionType.REFERRAL_REWARD,
                amount = REFERRER_REWARD,
                description = "Referral reward from ${referral.referredUser!!.email}",
            )
            referral.referrerRewarded = true
        }

        // Reward the referred user
        if (!referral.referredRewarded) {
            tokenService.rewardTokens(
                userId = referral.referredUser!!.id!!,
                type = TokenTransactionType.REFERRAL_REWARD,
                amount = REFERRED_REWARD,
                description = "Welcome bonus from referral",
            )
            referral.referredRewarded = true
        }

        referral.status = ReferralStatus.REWARDED
        referralRepository.save(referral)
    }

    /**
     * Get referral stats for a user.
     */
    fun getReferralStats(userId: Long): ReferralResponse {
        val user = userService.findEntity(userId)
        
        // Generate or get existing referral code
        val pendingReferrals = referralRepository.findByReferrerAndStatus(user, ReferralStatus.PENDING)
        val referral = if (pendingReferrals.isNotEmpty()) {
            pendingReferrals.first()
        } else {
            val newReferral = Referral(
                referrer = user,
                referralCode = generateUniqueCode(),
                status = ReferralStatus.PENDING,
                tokensAwarded = REFERRER_REWARD,
            )
            referralRepository.save(newReferral)
        }

        return toResponse(referral)
    }

    /**
     * Get all referrals for a user.
     */
    fun getUserReferrals(userId: Long): List<ReferralResponse> {
        val user = userService.findEntity(userId)
        return referralRepository.findAll()
            .filter { it.referrer?.id == userId }
            .sortedByDescending { it.createdAt }
            .map(::toResponse)
    }

    /**
     * Get total tokens earned from referrals.
     */
    fun getTotalTokensEarnedFromReferrals(userId: Long): Long {
        val user = userService.findEntity(userId)
        return referralRepository.sumTokensAwardedToReferrer(user)
    }

    private fun generateUniqueCode(): String {
        // Generate a short, unique-ish code (e.g., "REF_abc12def34")
        val uuid = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        return "REF_$uuid"
    }

    private fun toResponse(referral: Referral): ReferralResponse {
        val referredCount = referralRepository.findByReferrerAndStatus(referral.referrer!!, ReferralStatus.INVITED).size.toLong()
        val completedCount = referralRepository.findByReferrerAndStatus(referral.referrer!!, ReferralStatus.REWARDED).size.toLong()
        
        return ReferralResponse(
            id = referral.id!!,
            referralCode = referral.referralCode,
            status = referral.status.name,
            referredCount = referredCount,
            completedCount = completedCount,
            tokensEarned = if (referral.referrerRewarded) referral.tokensAwarded else 0,
        )
    }
}


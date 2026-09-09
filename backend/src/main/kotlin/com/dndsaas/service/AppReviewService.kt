package com.dndsaas.service

import com.dndsaas.domain.AppReview
import com.dndsaas.domain.TokenTransactionType
import com.dndsaas.dto.AppReviewResponse
import com.dndsaas.repository.AppReviewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing app review rewards.
 *
 * Users earn a one-time reward for leaving an app store review.
 * This incentivizes positive reviews while preventing duplicate rewards.
 */
@Service
@Transactional
class AppReviewService(
    private val appReviewRepository: AppReviewRepository,
    private val userService: UserService,
    private val tokenService: TokenService,
) {

    companion object {
        private const val REVIEW_REWARD_TOKENS = 25L
    }

    /**
     * Record an app review and reward tokens (if applicable).
     * Returns the tokens awarded, or 0 if already rewarded or on this platform.
     */
    fun submitReview(
        userId: Long,
        platformId: String,
        reviewUrl: String,
        reviewContent: String? = null,
    ): Long {
        val user = userService.findEntity(userId)

        // Check if user has already been rewarded for a review on this platform
        val existingReview = appReviewRepository.findByUserAndPlatform(user, platformId)
        if (existingReview != null && existingReview.hasBeenRewarded) {
            return 0 // Already rewarded for this platform
        }

        // Create or update the review record
        val review = existingReview ?: AppReview(
            user = user,
            platformId = platformId,
            reviewUrl = reviewUrl,
            tokensAwarded = REVIEW_REWARD_TOKENS,
        )

        review.reviewUrl = reviewUrl
        review.reviewContent = reviewContent
        review.hasBeenRewarded = true

        appReviewRepository.save(review)

        // Award tokens
        tokenService.rewardTokens(
            userId = userId,
            type = TokenTransactionType.APP_REVIEW_REWARD,
            amount = REVIEW_REWARD_TOKENS,
            description = "App review reward for $platformId",
        )

        return REVIEW_REWARD_TOKENS
    }

    /**
     * Get app review information for a user.
     */
    fun getUserReviews(userId: Long): List<AppReviewResponse> {
        val user = userService.findEntity(userId)
        return appReviewRepository.findAll()
            .filter { it.user?.id == userId }
            .sortedByDescending { it.createdAt }
            .map(::toResponse)
    }

    /**
     * Check if a user has already been rewarded for a review on a platform.
     */
    fun hasBeenRewarded(userId: Long, platformId: String): Boolean {
        val user = userService.findEntity(userId)
        val review = appReviewRepository.findByUserAndPlatform(user, platformId)
        return review?.hasBeenRewarded ?: false
    }

    /**
     * Get total reviews left and tokens earned from reviews.
     */
    fun getTotalReviewRewards(userId: Long): Long {
        val user = userService.findEntity(userId)
        val rewardedCount = appReviewRepository.countRewardedReviewsByUser(user)
        return rewardedCount * REVIEW_REWARD_TOKENS
    }

    private fun toResponse(review: AppReview): AppReviewResponse =
        AppReviewResponse(
            id = review.id!!,
            platformId = review.platformId,
            tokensAwarded = review.tokensAwarded,
            hasBeenRewarded = review.hasBeenRewarded,
            reviewUrl = review.reviewUrl,
        )
}


package com.dndsaas.controller

import com.dndsaas.dto.EarningOpportunitiesResponse
import com.dndsaas.dto.LoginStreakResponse
import com.dndsaas.dto.ReferralResponse
import com.dndsaas.dto.TokenEarnedResponse
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * REST endpoints for free tier earning opportunities.
 *
 * Free users can earn tokens by:
 * - Watching ads
 * - Referring friends
 * - Maintaining login streaks
 * - Leaving app reviews
 * - Sharing on social media
 */
@RestController
@RequestMapping("/v1/users/{userId}/earn")
@Tag(name = "Free Tier Earnings", description = "Earn tokens without spending money")
class FreeTierEarningController(
    private val billingOrchestrator: BillingOrchestrator,
) {

    @GetMapping
    @Operation(
        summary = "Get all available earning opportunities for free users",
    )
    fun getOpportunities(@PathVariable userId: Long): EarningOpportunitiesResponse =
        billingOrchestrator.getEarningOpportunities(userId)

    @GetMapping("/total")
    @Operation(
        summary = "Get total tokens earned from all free tier sources",
    )
    fun getTotalEarnings(@PathVariable userId: Long): Map<String, Long> =
        mapOf("totalEarned" to billingOrchestrator.getTotalEarningsFromFreeTier(userId))

    // -------- Ad Watching --------

    @PostMapping("/ads/watch")
    @Operation(
        summary = "Record an ad view and claim reward",
    )
    fun watchAd(
        @PathVariable userId: Long,
        @RequestBody request: WatchAdRequest,
    ): ResponseEntity<TokenEarnedResponse> {
        val result = billingOrchestrator.watchAd(userId, request.adId, request.adDetails ?: "")
        return ResponseEntity.status(if (result.tokensEarned > 0) HttpStatus.OK else HttpStatus.CONFLICT)
            .body(result)
    }

    // -------- Referral Program --------

    @GetMapping("/referral")
    @Operation(
        summary = "Get user's referral code and stats",
    )
    fun getReferralStats(@PathVariable userId: Long): ReferralResponse =
        billingOrchestrator.getReferralStats(userId)

    @PostMapping("/referral/generate-code")
    @Operation(
        summary = "Generate a new referral code",
    )
    fun generateReferralCode(@PathVariable userId: Long): ResponseEntity<ReferralResponse> =
        ResponseEntity.ok(billingOrchestrator.generateReferralCode(userId))

    @PostMapping("/referral/apply")
    @Operation(
        summary = "Apply a referral code during signup",
    )
    fun applyReferralCode(
        @PathVariable userId: Long,
        @RequestBody request: ApplyReferralRequest,
    ): ResponseEntity<Unit> {
        billingOrchestrator.applyReferralCode(request.referralCode, userId)
        return ResponseEntity.ok().build()
    }

    // -------- Login Streak --------

    @PostMapping("/login")
    @Operation(
        summary = "Record a login and claim streak bonus",
    )
    fun recordLogin(@PathVariable userId: Long): ResponseEntity<TokenEarnedResponse> =
        ResponseEntity.ok(billingOrchestrator.recordLogin(userId))

    @GetMapping("/login-streak")
    @Operation(
        summary = "Get user's login streak information",
    )
    fun getLoginStreak(@PathVariable userId: Long): LoginStreakResponse =
        billingOrchestrator.getLoginStreak(userId)

    // -------- App Reviews --------

    @PostMapping("/app-review")
    @Operation(
        summary = "Submit an app review and claim one-time reward",
    )
    fun submitAppReview(
        @PathVariable userId: Long,
        @RequestBody request: SubmitAppReviewRequest,
    ): ResponseEntity<TokenEarnedResponse> {
        val result = billingOrchestrator.submitAppReview(
            userId,
            request.platformId,
            request.reviewUrl,
            request.reviewContent,
        )
        return ResponseEntity.status(if (result.tokensEarned > 0) HttpStatus.OK else HttpStatus.CONFLICT)
            .body(result)
    }
}

// -------- Request DTOs --------

data class WatchAdRequest(
    val adId: String,
    val adDetails: String? = null,
)

data class ApplyReferralRequest(
    val referralCode: String,
)

data class SubmitAppReviewRequest(
    val platformId: String, // e.g., "google_play", "app_store"
    val reviewUrl: String,
    val reviewContent: String? = null,
)


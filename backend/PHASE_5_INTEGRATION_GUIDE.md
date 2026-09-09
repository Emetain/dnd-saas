#!/Users/jay/IdeaProjects/dnd-saas/backend/PHASE_5_INTEGRATION_GUIDE.md

# Phase 5 Integration Guide - Connecting Billing to Generators

## Overview

This guide explains how to integrate Phase 5 billing system with the existing Phase 4 generators.

## Step 1: Update Campaign Creation to Associate with User

**File**: `CampaignService.kt`

The `create()` method currently doesn't know about users. It needs to be updated:

```kotlin
// BEFORE
fun create(request: CampaignRequest): CampaignResponse {
    val campaign = Campaign(
        name = request.name,
        description = request.description,
        // ...
    )
    // ...
}

// AFTER
fun create(userId: Long, request: CampaignRequest): CampaignResponse {
    val user = userService.findEntity(userId)
    val campaign = Campaign(
        user = user,
        name = request.name,
        description = request.description,
        // ...
    )
    // ...
}
```

Similarly, update all Campaign creation in `CampaignGeneratorService.kt`:

```kotlin
// In the persist() method
val campaignResponse = campaignService.create(
    userId,  // ADD THIS
    CampaignRequest(
        name = blueprint.name.ifBlank { "Untitled Campaign" },
        // ...
    ),
)
```

## Step 2: Check Token Balance Before Generation

**File**: `CampaignGeneratorService.kt` (and other generators)

Before calling `aiService.generate()`, check if user has tokens:

```kotlin
@Transactional
fun generate(userId: Long, request: CampaignGenerationRequest): CampaignGenerationResult {
    // NEW: Check token balance
    val requiredTokens = GenerationType.CAMPAIGN.tokenCost
    if (!tokenService.chargeTokens(userId, requiredTokens)) {
        throw IllegalStateException("Insufficient tokens")
    }
    
    try {
        // ... existing generation logic ...
        val response = aiService.generate(...)
        // SUCCESS: tokens already deducted
        return CampaignGenerationResult(...)
    } catch (ex: Exception) {
        // IMPORTANT: Refund tokens on failure
        tokenService.recordTransaction(
            userId = userId,
            type = TokenTransactionType.MANUAL_REFUND,
            amount = requiredTokens,
            description = "Refund for failed generation",
        )
        throw ex
    }
}
```

## Step 3: Pass User Context Through Handler Chain

Controllers need to extract userId from authentication headers:

```kotlin
// UserController - already has userId in path
fun generateCampaign(
    @PathVariable userId: Long,
    @RequestBody request: CampaignGenerationRequest,
) {
    return campaignGeneratorOrchestrator.generate(userId, request)
}
```

Update orchestrator:

```kotlin
fun generate(userId: Long, request: CampaignGenerationRequest): CampaignGenerationResult =
    campaignGeneratorService.generate(userId, request)
```

## Step 4: Track Generation in Token Transactions

**File**: `AiService.kt`

When recording generation success, include userId context:

```kotlin
fun generate(
    campaign: Campaign?,
    request: GenerationRequest,
    // ... other params ...
): GenerationResponse {
    val startedAt = System.currentTimeMillis()
    
    val userId = campaign?.user?.id // Get userId from campaign if possible
    
    try {
        // ... generate ...
        
        val logEntry = generationLogService.recordSuccess(
            campaign = campaign,
            type = request.type,
            instruction = request.instruction,
            // ...
        )
        
        // NEW: Record token transaction
        if (userId != null && !result.mocked) {
            tokenService.recordTransaction(
                userId = userId,
                type = TokenTransactionType.GENERATION_CHARGE,
                amount = -request.type.tokenCost.toLong(),
                generationLogId = logEntry.id,
                description = "Generated ${request.type.label}",
            )
        }
        
        return GenerationResponse(...)
    }
    // ...
}
```

## Step 5: Update Campaign Generator Orchestrator

**File**: `CampaignGeneratorOrchestrator.kt`

Add userId parameter:

```kotlin
fun generate(userId: Long, request: CampaignGenerationRequest): CampaignGenerationResult =
    campaignGeneratorService.generate(userId, request)

fun generateOneShot(userId: Long, request: CampaignGenerationRequest): CampaignGenerationResult =
    campaignGeneratorService.generateOneShot(userId, request)
```

## Step 6: Create Campaign Generator Controller Endpoints

**File**: Create `CampaignGeneratorApiController.kt` (if not exists)

Add user-aware endpoints:

```kotlin
@RestController
@RequestMapping("/v1/users/{userId}/campaigns/generate")
class CampaignGeneratorApiController(
    private val campaignGeneratorOrchestrator: CampaignGeneratorOrchestrator,
    private val billingOrchestrator: BillingOrchestrator,
) {
    
    @PostMapping
    fun generate(
        @PathVariable userId: Long,
        @RequestBody request: CampaignGenerationRequest,
    ): ResponseEntity<CampaignGenerationResult> {
        // Check balance first
        val cost = GenerationType.CAMPAIGN.tokenCost
        if (billingOrchestrator.getTokenBalance(userId).currentTokens < cost) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(/* error response */)
        }
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(campaignGeneratorOrchestrator.generate(userId, request))
    }
    
    @PostMapping("/one-shot")
    fun generateOneShot(
        @PathVariable userId: Long,
        @RequestBody request: CampaignGenerationRequest,
    ): ResponseEntity<CampaignGenerationResult> {
        val cost = GenerationType.ONE_SHOT.tokenCost
        if (billingOrchestrator.getTokenBalance(userId).currentTokens < cost) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(/* error response */)
        }
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(campaignGeneratorOrchestrator.generateOneShot(userId, request))
    }
}
```

## Step 7: Implement Scheduled Tasks

**File**: Create `ScheduledTasks.kt`

Set up background jobs:

```kotlin
@Component
class ScheduledTasks(
    private val loginStreakService: LoginStreakService,
    private val subscriptionService: SubscriptionService,
) {
    
    @Scheduled(cron = "0 0 * * * *") // Daily at midnight
    fun dailyMaintenance() {
        log.info("Running daily maintenance tasks")
        loginStreakService.checkAndBreakStreaks()
        subscriptionService.processExpiredBillingCycles()
    }
}
```

Enable scheduling in `DndSaasApplication.kt`:

```kotlin
@SpringBootApplication
@EnableScheduling
class DndSaasApplication
```

## Step 8: Update API Response Models

**File**: `CampaignGenerationResult.kt` (or similar)

Add token cost info to response:

```kotlin
data class CampaignGenerationResult(
    val campaign: CampaignResponse,
    val created: CampaignMemoryStats,
    val firstSession: SessionResponse,
    val firstSessionPlan: GeneratedFirstSession,
    val usage: TokenUsage,
    val model: String,
    val mocked: Boolean,
    val durationMillis: Long,
    val generatedAt: Instant,
    // NEW:
    val tokenCost: Long = GenerationType.CAMPAIGN.tokenCost.toLong(),
    val userRemainingTokens: Long? = null,
)
```

## Step 9: Frontend API Changes

When calling generation endpoints, include userId:

```javascript
// BEFORE
POST /v1/ai/campaign-generator/generate

// AFTER
POST /v1/users/{userId}/campaigns/generate
```

Response now includes token info:

```javascript
{
    // ... existing response data ...
    "tokenCost": 10,
    "userRemainingTokens": 90
}
```

## Testing Checklist

- [ ] New user registers and has 50 tokens
- [ ] Generating campaign costs 10 tokens
- [ ] Token balance updates after generation
- [ ] Insufficient balance prevents generation (402 response)
- [ ] Failed generation refunds tokens
- [ ] Campaign has correct user_id association
- [ ] Token transaction logged for each generation
- [ ] Free tier earning endpoints work
- [ ] Referral code flow works end-to-end
- [ ] Login streak tracking works
- [ ] App review rewards issued correctly
- [ ] Subscription tier upgrades grant tokens
- [ ] Scheduled tasks execute on time

## Common Issues & Solutions

**Issue**: "Cannot insert null into user_id"
- **Solution**: Ensure userId is passed through the entire call chain

**Issue**: Campaigns show up for wrong users
- **Solution**: Add `WHERE user_id = ?` to campaign queries in `CampaignRepository`

**Issue**: Tokens not deducted after generation
- **Solution**: Verify `@Transactional` is on the service method and transaction commits

**Issue**: Scheduled tasks not running
- **Solution**: Add `@EnableScheduling` to main application class

## Next Steps

1. **Prioritize**: Token charging in generators (Step 2-3)
2. **Test**: With existing Phase 4 generators
3. **Deploy**: Once all tests pass
4. **Monitor**: Token usage patterns, conversion rates, referral effectiveness
5. **Tune**: Adjust token costs based on user feedback
6. **Phase 6**: Build frontend UI for token dashboard and earning opportunities


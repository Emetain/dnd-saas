# Phase 5: Billing, Subscriptions & Free Tier Earnings - Implementation Summary

## Overview

Phase 5 implements a complete billing system for the dnd-saas platform with the following features:

1. **User Accounts** - User registration and profile management
2. **Subscription Tiers** - Free, Pro, Pro+, and Ultimate DM with different token allowances
3. **Token System** - Platform tokens used to pay for generations
4. **Free Tier Earnings** - Multiple ways for free users to earn tokens without spending money:
   - **Ad Viewing** - Watch ads to earn 5 tokens (max 10 per day)
   - **Referral Program** - Earn 50 tokens per successful referral
   - **Login Streaks** - Earn 1 token per day, plus 25 bonus for 7-day milestones
   - **App Reviews** - Earn 25 tokens for leaving a review (per platform)
   - **Social Sharing** - Earn 10 tokens per share (max 2 per day, framework in place)

## Architecture

### 3-Layer Structure

**Domain Layer** (`/domain`)
- `User.kt` - User entity with token balance and subscription
- `BillingEnums.kt` - SubscriptionTier, TokenTransactionType, ReferralStatus enums
- `Billing.kt` - Subscription, TokenTransaction, AdView, Referral, LoginStreak, AppReview entities

**Repository Layer** (`/repository`)
- `UserRepository.kt` - User lookup and persistence
- `SubscriptionRepository.kt` - Subscription management
- `TokenTransactionRepository.kt` - Token transaction history and analytics
- `AdViewRepository.kt` - Ad tracking
- `ReferralRepository.kt` - Referral tracking
- `LoginStreakRepository.kt` - Login streak persistence
- `AppReviewRepository.kt` - App review tracking

**Service Layer** (`/service`)
- `UserService.kt` - Account creation, user lookups
- `TokenService.kt` - Token balance, charging, and rewards
- `SubscriptionService.kt` - Tier upgrades, billing cycles, monthly grants
- `AdService.kt` - Ad view processing, cooldown enforcement, daily limits
- `ReferralService.kt` - Referral code generation, completion, rewards
- `LoginStreakService.kt` - Streak tracking, milestone calculation, rewards
- `AppReviewService.kt` - Review submission, one-time reward enforcement
- `FreeTierEarningService.kt` - Aggregated view of all earning opportunities

**Orchestration Layer** (`/orchestration`)
- `BillingOrchestrator.kt` - Coordinates services for complete billing workflows

**Controller Layer** (`/controller`)
- `UserController.kt` - User registration and profile endpoints
- `TokenController.kt` - Token balance and history endpoints
- `SubscriptionController.kt` - Subscription management endpoints
- `FreeTierEarningController.kt` - Free tier earning opportunity endpoints

## Key Implementation Details

### Subscription Tiers

```kotlin
enum class SubscriptionTier(val monthlyTokens: Long) {
    FREE(50),              // Starter tokens
    PRO(1_000),
    PRO_PLUS(3_000),
    ULTIMATE_DM(5_000),
}
```

### Token Transaction Types

```
SUBSCRIPTION_GRANT      - Monthly token allowance
GENERATION_CHARGE       - Cost of using generators
AD_VIEW_REWARD          - Earned from ads
REFERRAL_REWARD         - Earned from referrals
LOGIN_STREAK_REWARD     - Earned from daily login streaks
APP_REVIEW_REWARD       - Earned from app reviews
SOCIAL_SHARE_REWARD     - Earned from sharing
MANUAL_REFUND           - Admin adjustments
```

### Free Tier Safeguards

**Ad Viewing**
- Maximum 10 ads per day per user
- 60-minute cooldown between viewing the same ad
- Prevents token farming and fraud

**Login Streaks**
- 1 token per login + milestone bonuses
- 25 bonus tokens every 7 days
- Streak breaks if user doesn't log in for a day
- Scheduled task `checkAndBreakStreaks()` resets broken streaks

**Referrals**
- 50 tokens for referrer, 25 tokens for referred user
- Referral completes when referred user executes first qualifying action
- One-time reward per referral relationship

**App Reviews**
- 25 tokens per platform (Google Play, App Store)
- One-time reward per user per platform
- Prevents duplicate rewards

### Referral Flow

1. User generates a referral code: `REF_ABC123XYZ`
2. User shares code with friend
3. Friend uses code during signup
4. Referred user is linked to referral relationship
5. When referred user completes first generation, referral completes
6. Both users receive tokens

## REST API Endpoints

### User Management
- `POST /v1/users/register` - Create new account
- `GET /v1/users/{userId}` - Get account info

### Token Management
- `GET /v1/users/{userId}/tokens` - Get balance and recent transactions
- `GET /v1/users/{userId}/tokens/history` - Full transaction history

### Subscriptions
- `GET /v1/users/{userId}/subscription` - Get subscription details
- `POST /v1/users/{userId}/subscription/upgrade` - Upgrade tier
- `POST /v1/users/{userId}/subscription/downgrade-free` - Downgrade to free

### Free Tier Earnings
- `GET /v1/users/{userId}/earn` - Get all earning opportunities
- `GET /v1/users/{userId}/earn/total` - Total earned from free actions
- `POST /v1/users/{userId}/earn/ads/watch` - Record ad view
- `GET /v1/users/{userId}/earn/referral` - Get referral stats
- `POST /v1/users/{userId}/earn/referral/generate-code` - Generate new code
- `POST /v1/users/{userId}/earn/referral/apply` - Apply referral code
- `POST /v1/users/{userId}/earn/login` - Record login and claim streak bonus
- `GET /v1/users/{userId}/earn/login-streak` - Get streak info
- `POST /v1/users/{userId}/earn/app-review` - Submit app review

## Database Schema

### New Tables

**users**
- id (PK)
- email (UNIQUE)
- displayName
- passwordHash
- subscriptionTier (ENUM)
- platformTokens (BIGINT)
- createdAt, updatedAt

**subscriptions**
- id (PK)
- user_id (FK → users)
- tier (ENUM)
- billingCycleStart
- billingCycleEnd
- isActive
- createdAt, updatedAt

**token_transactions**
- id (PK)
- user_id (FK → users)
- type (ENUM)
- amount (BIGINT, can be negative for charges)
- description
- campaignId (nullable)
- generationLogId (nullable)
- createdAt, updatedAt

**ad_views**
- id (PK)
- user_id (FK → users)
- adId
- tokensEarned
- adDetails
- createdAt, updatedAt

**referrals**
- id (PK)
- referrer_user_id (FK → users)
- referred_user_id (FK → users, nullable)
- referralCode (UNIQUE)
- status (ENUM: PENDING, INVITED, COMPLETED, REWARDED)
- tokensAwarded
- referrerRewarded, referredRewarded
- createdAt, updatedAt

**login_streaks**
- id (PK)
- user_id (FK → users)
- currentStreak (INT)
- longestStreak (INT)
- lastLoginDate
- tokensEarned
- createdAt, updatedAt

**app_reviews**
- id (PK)
- user_id (FK → users)
- platformId
- reviewUrl
- tokensAwarded
- hasBeenRewarded
- reviewContent (nullable)
- createdAt, updatedAt

### Modified Tables

**campaigns**
- Added `user_id` (FK → users) - Associates campaigns with their owner

## Integration Points

### With Generators (Phase 4)

When a user tries to use a generator:

1. System checks if user has enough tokens
2. If not, show earning opportunities (free tier earning controller)
3. If yes, charge tokens: `tokenService.chargeTokens(...)`
4. Store charge in TokenTransaction with generationLogId reference
5. Update user.platformTokens

### With Subscriptions

Monthly billing cycle refresh:
```kotlin
subscriptionService.processExpiredBillingCycles() // Called by scheduled task
```

This:
1. Finds subscriptions with expired cycles
2. Starts new 30-day cycle
3. Grants next month's tokens via TokenTransaction

## Future Enhancements

1. **Social Share Tracking** - Full implementation of social media sharing rewards
2. **Scheduled Tasks** - Set up cron jobs for:
   - `loginStreakService.checkAndBreakStreaks()` - Daily
   - `subscriptionService.processExpiredBillingCycles()` - Daily
3. **Fraud Detection** - Pattern analysis for referral bombing, ad spam
4. **Analytics Dashboard** - See token usage patterns, conversion funnels, LTV
5. **Trial Periods** - Free trial with full feature access for new users
6. **Promo Codes** - Marketing codes that grant bonus tokens
7. **Team/Family Plans** - Family accounts with shared tokens
8. **Token Marketplace** - Purchase additional tokens with cards/PayPal

## Configuration Notes

### For Development (H2 Database)
- No migration needed - Hibernate creates schema automatically
- Tables created on app startup with `ddl-auto: create-drop` or `update`

### For Production (PostgreSQL)
- Run migrations in `/migrations.sql` ONCE before deploying
- Migrations only add tables/columns, never remove
- Use `ddl-auto: validate` to ensure schema consistency

## Testing Recommendations

1. **User Registration** - Verify 50 starting tokens, tier defaults to FREE
2. **Token Charging** - Ensure insufficient balance is rejected
3. **Ad Viewing** - Test daily limits, cooldown, duplicate prevention
4. **Referrals** - Test code generation, linking, completion flow
5. **Login Streaks** - Test streak increment, milestone bonuses, streak breaking
6. **App Reviews** - Test one-time reward per platform
7. **Subscription Upgrades** - Verify new monthly allowance granted
8. **Billing Cycles** - Test rollover and new token grants

## Notes for Frontend Integration

The free tier earning dashboard should display:
1. Current token balance
2. List of earning opportunities with:
   - Token reward amount
   - Whether it's available now
   - Why it's not available (if applicable)
   - Progress/stats (daily limit, streak, referral count, etc.)
3. Total earned across all sources

The referral UI should:
1. Display unique referral code prominently
2. Show "Copy to clipboard" button
3. Display stats: pending referrals, completed, tokens earned
4. Show input field to apply a referral code during signup

All endpoints return meaningful HTTP status codes:
- `200 OK` - Successful action
- `201 CREATED` - New resource created
- `409 CONFLICT` - Action cannot be completed (e.g., insufficient tokens, already rewarded)
- `400 BAD REQUEST` - Invalid input
- `404 NOT FOUND` - Resource not found


package com.dndsaas.service

import com.dndsaas.domain.LoginStreak
import com.dndsaas.domain.TokenTransactionType
import com.dndsaas.dto.LoginStreakResponse
import com.dndsaas.repository.LoginStreakRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Service for tracking login streaks and rewarding engagement.
 *
 * Users earn tokens for maintaining consecutive login streaks.
 * The longer the streak, the more valuable the reward.
 */
@Service
@Transactional
class LoginStreakService(
    private val loginStreakRepository: LoginStreakRepository,
    private val userService: UserService,
    private val tokenService: TokenService,
) {

    companion object {
        private const val BASE_TOKENS_PER_LOGIN = 1L
        private const val MILESTONE_REWARD_DAYS = 7 // Extra reward every 7 days
        private const val MILESTONE_BONUS_TOKENS = 25L
    }

    /**
     * Record a user login and update their streak.
     * Returns tokens earned for this login.
     */
    fun recordLogin(userId: Long): Long {
        val user = userService.findEntity(userId)
        
        var streak = loginStreakRepository.findByUser(user)
            ?: LoginStreak(user = user, currentStreak = 0, longestStreak = 0)

        val today = LocalDate.now(ZoneOffset.UTC)
        val lastLoginDate = streak.lastLoginDate?.let { instant ->
            instant.atZone(ZoneOffset.UTC).toLocalDate()
        }

        // Update streak
        when {
            lastLoginDate == null -> {
                // First login ever
                streak.currentStreak = 1
            }
            lastLoginDate == today.minusDays(1) -> {
                // Consecutive day
                streak.currentStreak += 1
            }
            lastLoginDate == today -> {
                // Already logged in today, don't update streak
                return 0
            }
            else -> {
                // Streak broken, restart
                streak.currentStreak = 1
            }
        }

        streak.lastLoginDate = Instant.now()

        // Update longest streak
        if (streak.currentStreak > streak.longestStreak) {
            streak.longestStreak = streak.currentStreak
        }

        // Award tokens (the monthly earning cap may reduce the reward)
        val tokensEarned = tokenService.rewardTokens(
            userId = userId,
            type = TokenTransactionType.LOGIN_STREAK_REWARD,
            amount = calculateReward(streak.currentStreak),
            description = "Login streak reward (day ${streak.currentStreak})",
        )
        streak.tokensEarned += tokensEarned

        loginStreakRepository.save(streak)

        return tokensEarned
    }

    /**
     * Get login streak info for a user.
     */
    fun getStreakInfo(userId: Long): LoginStreakResponse {
        val user = userService.findEntity(userId)
        val streak = loginStreakRepository.findByUser(user)
            ?: LoginStreak(user = user)

        val today = LocalDate.now(ZoneOffset.UTC)
        val lastLoginDate = streak.lastLoginDate?.let { instant ->
            instant.atZone(ZoneOffset.UTC).toLocalDate()
        }

        // Calculate next reward (milestone-based)
        val daysUntilNextMilestone = if (streak.currentStreak > 0) {
            val remainder = streak.currentStreak % MILESTONE_REWARD_DAYS
            if (remainder == 0) MILESTONE_REWARD_DAYS.toInt() else (MILESTONE_REWARD_DAYS - remainder).toInt()
        } else {
            1
        }

        return LoginStreakResponse(
            currentStreak = streak.currentStreak,
            longestStreak = streak.longestStreak,
            totalTokensEarned = streak.tokensEarned,
            lastLoginDate = streak.lastLoginDate,
            nextReward = daysUntilNextMilestone,
        )
    }

    /**
     * Break a streak if the user hasn't logged in.
     * Called periodically by a scheduled task.
     */
    fun checkAndBreakStreaks() {
        val yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1)
        val yesterdayStart = yesterday.atStartOfDay(ZoneOffset.UTC).toInstant()

        loginStreakRepository.findAll().forEach { streak ->
            val lastLoginDate = streak.lastLoginDate?.let { instant ->
                instant.atZone(ZoneOffset.UTC).toLocalDate()
            }

            if (lastLoginDate != null && lastLoginDate.isBefore(yesterday)) {
                streak.currentStreak = 0
                loginStreakRepository.save(streak)
            }
        }
    }

    private fun calculateReward(streakDays: Int): Long {
        var reward = BASE_TOKENS_PER_LOGIN

        // Bonus for milestone days
        if (streakDays % MILESTONE_REWARD_DAYS == 0) {
            reward += MILESTONE_BONUS_TOKENS
        }

        return reward
    }
}


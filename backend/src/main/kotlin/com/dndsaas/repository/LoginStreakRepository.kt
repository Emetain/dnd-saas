package com.dndsaas.repository

import com.dndsaas.domain.LoginStreak
import com.dndsaas.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface LoginStreakRepository : JpaRepository<LoginStreak, Long> {
    fun findByUser(user: User): LoginStreak?
}


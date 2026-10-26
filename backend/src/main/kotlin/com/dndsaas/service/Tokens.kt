package com.dndsaas.service

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Random tokens for cookies and email links, and the hash that is stored instead of them. */
object Tokens {
    private val random = SecureRandom()

    /** 256 random bits, URL-safe. */
    fun newToken(): String {
        val bytes = ByteArray(32).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
}

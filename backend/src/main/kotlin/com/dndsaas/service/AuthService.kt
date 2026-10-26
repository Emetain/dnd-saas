package com.dndsaas.service

import com.dndsaas.config.AuthProperties
import com.dndsaas.domain.AuthSession
import com.dndsaas.domain.EmailToken
import com.dndsaas.domain.EmailTokenPurpose
import com.dndsaas.domain.User
import com.dndsaas.dto.UserRegistrationRequest
import com.dndsaas.dto.UserResponse
import com.dndsaas.repository.AuthSessionRepository
import com.dndsaas.repository.EmailTokenRepository
import com.dndsaas.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Signing in and out, verifying email addresses and resetting passwords.
 *
 * A successful sign-in creates an [AuthSession] and returns its random token,
 * which the controller puts in an HttpOnly cookie. Email links work the same way
 * ([EmailToken]). Only hashes of tokens are stored. Repeated wrong passwords for
 * one email are slowed down.
 */
@Service
@Transactional
class AuthService(
    private val userService: UserService,
    private val userRepository: UserRepository,
    private val authSessionRepository: AuthSessionRepository,
    private val emailTokenRepository: EmailTokenRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder,
    private val properties: AuthProperties,
) {
    private val log = LoggerFactory.getLogger(AuthService::class.java)

    /** A valid hash of a random string, compared against when the email is unknown. */
    private val dummyHash: String by lazy { passwordEncoder.encode(java.util.UUID.randomUUID().toString()) }

    /** Failed attempts per email, to stop password guessing. */
    private val failures = ConcurrentHashMap<String, Failures>()

    /** A new session: the user plus the token for the cookie. */
    data class SignedIn(val user: UserResponse, val token: String, val maxAge: Duration)

    /** Creates the account, signs it in and emails a verification link. */
    fun register(request: UserRegistrationRequest): SignedIn {
        val user = userService.register(request)
        sendVerification(userService.findEntity(user.id))
        return SignedIn(user, createSession(user.id), sessionLength)
    }

    // -----------------------------------------------------------------------
    // Email verification
    // -----------------------------------------------------------------------

    /** Marks the address as verified. Returns the user, so the page can greet them. */
    fun verifyEmail(token: String): UserResponse {
        val link = useLink(token, EmailTokenPurpose.VERIFY_EMAIL)
            ?: throw IllegalArgumentException("This confirmation link is invalid or has expired — sign in and ask for a new one")
        val user = checkNotNull(link.user)
        user.emailVerified = true
        userRepository.save(user)
        return userService.get(user.id!!)
    }

    fun resendVerification(userId: Long) {
        val user = userService.findEntity(userId)
        require(!user.emailVerified) { "Your email address is already confirmed" }
        requireNotTooSoon(user, EmailTokenPurpose.VERIFY_EMAIL)
        sendVerification(user)
    }

    private fun sendVerification(user: User) {
        emailService.sendVerification(user, createLink(user, EmailTokenPurpose.VERIFY_EMAIL, VERIFY_LINK_VALID))
    }

    // -----------------------------------------------------------------------
    // Forgotten passwords
    // -----------------------------------------------------------------------

    /**
     * Emails a reset link if the address has an account. Says nothing either way,
     * so the form can't be used to find out who has an account.
     */
    fun forgotPassword(email: String) {
        val user = userRepository.findByEmailIgnoreCase(email.trim()) ?: return
        val last = emailTokenRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.id!!, EmailTokenPurpose.RESET_PASSWORD)
        if (last != null && last.createdAt.isAfter(Instant.now().minus(RESEND_AFTER))) return
        emailService.sendPasswordReset(user, createLink(user, EmailTokenPurpose.RESET_PASSWORD, RESET_LINK_VALID))
    }

    /**
     * Sets a new password from a reset link and signs the account out everywhere.
     * The link also proves the address, so it counts as verified.
     */
    fun resetPassword(token: String, newPassword: String) {
        userService.checkPasswordRules(newPassword)
        val link = useLink(token, EmailTokenPurpose.RESET_PASSWORD)
            ?: throw IllegalArgumentException("This reset link is invalid or has expired — ask for a new one")
        val user = checkNotNull(link.user)
        user.passwordHash = passwordEncoder.encode(newPassword)
        user.emailVerified = true
        userRepository.save(user)
        authSessionRepository.deleteOtherSessions(user.id!!, "")
        failures.remove(user.email.lowercase())
    }

    private fun createLink(user: User, purpose: EmailTokenPurpose, validFor: Duration): String {
        val token = Tokens.newToken()
        emailTokenRepository.save(
            EmailToken(tokenHash = Tokens.hash(token), user = user, purpose = purpose, expiresAt = Instant.now().plus(validFor)),
        )
        return token
    }

    /** The link if it is valid, unused and not expired — and marks it used. */
    private fun useLink(token: String, purpose: EmailTokenPurpose): EmailToken? {
        val link = emailTokenRepository.findByTokenHashAndPurpose(Tokens.hash(token), purpose) ?: return null
        if (link.usedAt != null || link.expiresAt.isBefore(Instant.now())) return null
        link.usedAt = Instant.now()
        return emailTokenRepository.save(link)
    }

    private fun requireNotTooSoon(user: User, purpose: EmailTokenPurpose) {
        val last = emailTokenRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.id!!, purpose) ?: return
        require(last.createdAt.isBefore(Instant.now().minus(RESEND_AFTER))) { "We just sent you an email — wait a minute before asking again" }
    }

    // -----------------------------------------------------------------------
    // Signing in and out
    // -----------------------------------------------------------------------

    fun login(email: String, password: String): SignedIn {
        val key = email.trim().lowercase()
        failures[key]?.let { if (it.isLocked()) throw TooManyLoginAttemptsException() }

        val user = userRepository.findByEmailIgnoreCase(key)
        // Always run BCrypt, so a missing account takes as long as a wrong password.
        val matches = passwordEncoder.matches(password, user?.passwordHash ?: dummyHash)
        if (user == null || !matches) {
            failures.compute(key) { _, current -> (current?.takeUnless { it.expired() } ?: Failures()).also { it.count++ } }
            throw InvalidCredentialsException()
        }
        failures.remove(key)
        return SignedIn(userService.get(user.id!!), createSession(user.id!!), sessionLength)
    }

    /**
     * Changes the password after checking the current one, and signs the account
     * out of every other browser (the one making the change stays signed in).
     */
    fun changePassword(userId: Long, currentToken: String?, currentPassword: String, newPassword: String) {
        val user = userService.findEntity(userId)
        if (!passwordEncoder.matches(currentPassword, user.passwordHash)) {
            throw IllegalArgumentException("Your current password is incorrect")
        }
        userService.checkPasswordRules(newPassword)
        user.passwordHash = passwordEncoder.encode(newPassword)
        userRepository.save(user)
        authSessionRepository.deleteOtherSessions(userId, currentToken?.let(::hash) ?: "")
    }

    fun logout(token: String?) {
        if (token != null) authSessionRepository.deleteByTokenHash(hash(token))
    }

    /** The signed-in user for a cookie token, or null if it is unknown or expired. */
    fun userIdFor(token: String): Long? {
        val session = authSessionRepository.findByTokenHash(hash(token)) ?: return null
        if (session.expiresAt.isBefore(Instant.now())) {
            authSessionRepository.delete(session)
            return null
        }
        return session.user?.id
    }

    /** Removes expired sessions and email links once an hour. */
    @Scheduled(cron = "0 17 * * * *")
    fun cleanUp() {
        val removed = authSessionRepository.deleteExpired(Instant.now())
        emailTokenRepository.deleteExpired(Instant.now())
        if (removed > 0) log.info("Removed {} expired sign-in sessions", removed)
    }

    /**
     * Accounts made before sign-in existed have their password in plain text.
     * Hash those once at startup, so they can sign in with the password they chose.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun hashLegacyPasswords() {
        val legacy = userRepository.findAll().filter { it.passwordHash.isNotEmpty() && !it.passwordHash.startsWith("\$2") }
        legacy.forEach { it.passwordHash = passwordEncoder.encode(it.passwordHash) }
        userRepository.saveAll(legacy)
        if (legacy.isNotEmpty()) log.info("Hashed the plain-text passwords of {} existing accounts", legacy.size)
    }

    private val sessionLength: Duration get() = Duration.ofDays(properties.sessionDays)

    private fun createSession(userId: Long): String {
        val token = Tokens.newToken()
        authSessionRepository.save(
            AuthSession(
                tokenHash = hash(token),
                user = userService.findEntity(userId),
                expiresAt = Instant.now().plus(sessionLength),
            ),
        )
        return token
    }

    private fun hash(token: String): String = Tokens.hash(token)

    private class Failures {
        val since: Instant = Instant.now()
        @Volatile var count = 0

        fun expired() = since.plus(LOCK_WINDOW).isBefore(Instant.now())
        fun isLocked() = !expired() && count >= MAX_FAILURES
    }

    private companion object {
        const val MAX_FAILURES = 8
        val LOCK_WINDOW: Duration = Duration.ofMinutes(15)
        val VERIFY_LINK_VALID: Duration = Duration.ofHours(48)
        val RESET_LINK_VALID: Duration = Duration.ofHours(1)

        /** At most one verification or reset email per minute per account. */
        val RESEND_AFTER: Duration = Duration.ofMinutes(1)
    }
}

/** Not signed in, or the session has expired. */
class NotSignedInException : RuntimeException("Please sign in")

/** Wrong email or password; deliberately doesn't say which. */
class InvalidCredentialsException : RuntimeException("The email or password is incorrect")

class TooManyLoginAttemptsException :
    RuntimeException("Too many failed sign-in attempts — wait 15 minutes and try again")

/** A signed-in user reached for another user's account. */
class NotYourAccountException : RuntimeException("You can only access your own account")

/** Generating needs a confirmed email address (it stops free-token farming with throwaway accounts). */
class EmailNotVerifiedException :
    RuntimeException("Confirm your email address first — check your inbox, or ask for a new link on your dashboard")

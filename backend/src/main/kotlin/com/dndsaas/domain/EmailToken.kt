package com.dndsaas.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

enum class EmailTokenPurpose { VERIFY_EMAIL, RESET_PASSWORD }

/**
 * A one-time link sent by email (verify the address, reset the password).
 * Like sign-in sessions, only a SHA-256 hash of the token is stored.
 */
@Entity
@Table(name = "email_tokens")
class EmailToken(
    @Column(nullable = false, unique = true, length = 64)
    var tokenHash: String = "",

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var purpose: EmailTokenPurpose = EmailTokenPurpose.VERIFY_EMAIL,

    @Column(nullable = false)
    var expiresAt: Instant = Instant.now(),

    /** Set once the link has been used; a link works only once. */
    var usedAt: Instant? = null,
) : BaseEntity()

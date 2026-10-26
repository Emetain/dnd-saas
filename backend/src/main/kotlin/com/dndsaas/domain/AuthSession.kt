package com.dndsaas.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

/**
 * A signed-in browser. The browser holds a random token in an HttpOnly cookie;
 * only its SHA-256 hash is stored, so a leaked database can't be used to sign in.
 */
@Entity
@Table(name = "auth_sessions")
class AuthSession(
    @Column(nullable = false, unique = true, length = 64)
    var tokenHash: String = "",

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    var user: User? = null,

    @Column(nullable = false)
    var expiresAt: Instant = Instant.now(),
) : BaseEntity()

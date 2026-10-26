package com.dndsaas.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Sign-in settings, bound from `app.auth.*` in application.yml. */
@ConfigurationProperties(prefix = "app.auth")
data class AuthProperties(
    /** The frontend's address; links in emails point here. */
    val baseUrl: String = "http://localhost:5173",
    val mailFrom: String = "Narrion <no-reply@narrion.local>",
    val cookieName: String = "narrion_session",
    /** Send the cookie over HTTPS only. Must be true once the site runs on HTTPS. */
    val secureCookie: Boolean = false,
    /** How long a sign-in lasts without signing in again. */
    val sessionDays: Long = 30,
)

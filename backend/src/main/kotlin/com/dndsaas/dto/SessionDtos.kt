package com.dndsaas.dto

import java.time.LocalDate

/** Request to create or update a session. */
data class SessionRequest(
    val title: String,
    val sessionNumber: Int,
    val date: LocalDate? = null,
    val notes: String = "",
    val summary: String = "",
)

/** Session response. */
data class SessionResponse(
    val id: Long,
    val campaignId: Long,
    val title: String,
    val sessionNumber: Int,
    val date: LocalDate?,
    val notes: String,
    val summary: String,
)


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
    /** The session plan (Markdown). */
    val notes: String,
    /** What happened (Markdown); empty until the session has been debriefed. */
    val summary: String,
    /** The answers behind [summary], or null if the session has not been debriefed yet. */
    val debrief: SessionDebrief?,
)

/**
 * The set questions a DM answers after playing a session. Only [whatHappened]
 * is required; together they give the AI what it needs to plan the next session.
 */
data class SessionDebrief(
    /** What happened this session? */
    val whatHappened: String = "",
    /** Which quests moved forward, were completed or abandoned? */
    val questProgress: String = "",
    /** Which NPCs did the party meet, and how did it go? */
    val npcs: String = "",
    /** What important choices did the players make? */
    val decisions: String = "",
    /** Where and how did the session end? */
    val endedAt: String = "",
    /** Which loose threads, player theories or plans are still open? */
    val looseThreads: String = "",
    /** Did the party change: levels, items, deaths, new members? */
    val partyChanges: String = "",
    /** Anything the DM wants to happen next session? */
    val nextSessionWishes: String = "",
)

data class NextSessionRequest(
    val mock: Boolean = false,
)

/** The newly planned session, plus the structured plan behind its notes. */
data class NextSessionResult(
    val session: SessionResponse,
    val plan: GeneratedFirstSession,
    val usage: TokenUsage,
    val model: String,
    val mocked: Boolean,
)


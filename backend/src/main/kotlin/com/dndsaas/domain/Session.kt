package com.dndsaas.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate

/**
 * A single play session within a campaign. Sessions form the campaign
 * timeline and history, and their notes/summaries feed Campaign Memory.
 */
@Entity
@Table(name = "sessions")
class Session(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,

    var title: String = "",

    /** Sequential number within the campaign (Session 1, 2, 3...). */
    var sessionNumber: Int = 1,

    var date: LocalDate? = null,

    /** Raw DM notes taken during/after the session. */
    @Column(columnDefinition = "TEXT")
    var notes: String = "",

    /** What actually happened, used as AI context so future generations know history. */
    @Column(columnDefinition = "TEXT")
    var summary: String = "",

    /**
     * The DM's answers to the post-session questions, as JSON, so the form can be
     * reopened and edited. [summary] holds the same answers rendered as Markdown.
     */
    @Column(columnDefinition = "TEXT")
    var debrief: String? = null,
) : BaseEntity()


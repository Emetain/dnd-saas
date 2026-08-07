package com.dndsaas.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * A record of one AI generation.
 *
 * Every request through the central AI service is logged here: what was asked,
 * what came back, and how many tokens it cost. This gives Dungeon Masters a
 * history of what they generated, and gives Phase 5 the usage data it needs
 * for billing.
 */
@Entity
@Table(name = "generation_logs")
class GenerationLog(
    /**
     * The campaign this generation belongs to.
     *
     * Null for generations that happen *before* a campaign exists — the
     * campaign interview and the initial campaign blueprint.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,

    @Enumerated(EnumType.STRING)
    var type: GenerationType = GenerationType.FREEFORM,

    /** What the Dungeon Master asked for. */
    @Column(columnDefinition = "TEXT")
    var instruction: String = "",

    /** The model's raw reply, kept for auditing and re-parsing. */
    @Column(columnDefinition = "TEXT")
    var result: String = "",

    /** Whether the pipeline completed successfully. */
    var successful: Boolean = true,

    /** Populated when the generation failed. */
    @Column(columnDefinition = "TEXT")
    var errorMessage: String = "",

    var model: String = "",
    var promptTokens: Int = 0,
    var completionTokens: Int = 0,
    var totalTokens: Int = 0,

    /** Platform tokens charged for this generation (Phase 5). */
    var tokenCost: Int = 0,

    /** True when produced by the mock client rather than a real provider. */
    var mocked: Boolean = false,

    /** How long the whole pipeline took. */
    var durationMillis: Long = 0,

    /** Size of the campaign context that was injected, for prompt tuning. */
    var contextCharacters: Int = 0,
) : BaseEntity()


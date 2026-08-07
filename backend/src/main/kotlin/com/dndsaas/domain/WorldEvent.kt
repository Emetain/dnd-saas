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
 * Something that happened in the world — the campaign timeline.
 * World events give the AI a sense of history and consequence.
 */
@Entity
@Table(name = "world_events")
class WorldEvent(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,

    var title: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    /** In-world date as free text, e.g. "15th of Flamerule, 1492 DR". */
    var inGameDate: String = "",

    @Enumerated(EnumType.STRING)
    var importance: EventImportance = EventImportance.NOTABLE,

    /** The real session in which this happened, if applicable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    var session: Session? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    var location: Location? = null,

    /** Lasting effects on the world, used as AI context. */
    @Column(columnDefinition = "TEXT")
    var consequences: String = "",

    /** Whether the players know about this event. */
    var knownToPlayers: Boolean = true,
) : BaseEntity()


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
 * A place in the campaign world — a continent, city, tavern or dungeon.
 *
 * Locations can be nested via [parent], so "The Blood of the Vine Tavern"
 * can live inside "Village of Barovia", which lives inside "Barovia".
 */
@Entity
@Table(name = "locations")
class Location(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,

    var name: String = "",

    @Enumerated(EnumType.STRING)
    var type: LocationType = LocationType.OTHER,

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    /** Distinctive sights, sounds and smells — useful for AI descriptions. */
    @Column(columnDefinition = "TEXT")
    var atmosphere: String = "",

    /** Containing location, enabling a world hierarchy. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Location? = null,

    /** Private DM notes, not shown to players. */
    @Column(columnDefinition = "TEXT")
    var notes: String = "",

    /** Whether the party has been here yet. */
    var discovered: Boolean = false,
) : BaseEntity()


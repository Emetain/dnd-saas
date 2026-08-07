package com.dndsaas.dto

import com.dndsaas.domain.LocationType
import java.time.Instant

/** Request to create or update a location. */
data class LocationRequest(
    val name: String,
    val type: LocationType = LocationType.OTHER,
    val description: String = "",
    val atmosphere: String = "",
    /** Id of the containing location, or null for a top-level location. */
    val parentId: Long? = null,
    val notes: String = "",
    val discovered: Boolean = false,
)

data class LocationResponse(
    val id: Long,
    val campaignId: Long,
    val name: String,
    val type: LocationType,
    val description: String,
    val atmosphere: String,
    val parent: EntityRef?,
    val notes: String,
    val discovered: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)


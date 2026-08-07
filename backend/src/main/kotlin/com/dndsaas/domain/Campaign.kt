package com.dndsaas.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

/**
 * A D&D campaign — the root object that everything else (sessions, NPCs,
 * locations, quests, items, world lore) connects to. This is the container
 * for "Campaign Memory".
 */
@Entity
@Table(name = "campaigns")
class Campaign(
    var name: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    /** The game system, e.g. "D&D 5e". Kept flexible for other TTRPGs. */
    var system: String = "D&D 5e",

    /** High-level world lore / setting summary used as AI context. */
    @Column(columnDefinition = "TEXT")
    var worldLore: String = "",

    @OneToMany(mappedBy = "campaign", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var sessions: MutableList<Session> = mutableListOf(),

    /** The party — player characters taking part in this campaign. */
    @OneToMany(mappedBy = "campaign", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var characters: MutableList<Character> = mutableListOf(),
) : BaseEntity()


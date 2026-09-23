package com.dndsaas.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.hibernate.annotations.ColumnDefault
import jakarta.persistence.Enumerated
import jakarta.persistence.EnumType
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
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
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", foreignKey = ForeignKey(name = "fk_campaign_user"))
    var user: User? = null,

    var name: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    /** A full campaign (Pro and up) or a one-shot (every tier). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'CAMPAIGN'")
    var kind: CampaignKind = CampaignKind.CAMPAIGN,

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

enum class CampaignKind {
    CAMPAIGN,
    ONE_SHOT,
}


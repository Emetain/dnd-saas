package com.dndsaas.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * Something the party can pursue. Quests connect NPCs and locations, and their
 * status tells the AI what is currently going on in the campaign.
 */
@Entity
@Table(name = "quests")
class Quest(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,

    var title: String = "",

    @Column(columnDefinition = "TEXT")
    var description: String = "",

    /** The one-line hook used to present the quest to the players. */
    @Column(columnDefinition = "TEXT")
    var hook: String = "",

    /** What the party must actually do. */
    @Column(columnDefinition = "TEXT")
    var objective: String = "",

    @Enumerated(EnumType.STRING)
    var status: QuestStatus = QuestStatus.AVAILABLE,

    /** The NPC who offers the quest. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_giver_id")
    var questGiver: Npc? = null,

    /** Where the quest takes place. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    var location: Location? = null,

    /** Every NPC entangled in this quest. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "quest_npcs",
        joinColumns = [JoinColumn(name = "quest_id")],
        inverseJoinColumns = [JoinColumn(name = "npc_id")],
    )
    var involvedNpcs: MutableSet<Npc> = mutableSetOf(),

    @Column(columnDefinition = "TEXT")
    var reward: String = "",

    @Column(columnDefinition = "TEXT")
    var notes: String = "",

    var aiGenerated: Boolean = false,
) : BaseEntity()


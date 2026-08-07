package com.dndsaas.service

import com.dndsaas.domain.GenerationType
import com.dndsaas.dto.CampaignMemorySnapshot
import org.springframework.stereotype.Service

/**
 * Step 1 of the AI pipeline: collect campaign context.
 *
 * Reads the structured Campaign Memory built in Phase 2 and condenses it into
 * compact text the model can read. This is the single most important step in
 * the product: it is why a generated NPC fits the existing world instead of
 * being a random stranger.
 *
 * Context is trimmed per generation type — an NPC request does not need the
 * full loot inventory — to keep prompts small and cheap.
 */
@Service
class AiContextCollector(
    private val campaignMemoryService: CampaignMemoryService,
) {

    /** Caps that keep prompts affordable while staying representative. */
    private companion object {
        const val MAX_LOCATIONS = 25
        const val MAX_FACTIONS = 15
        const val MAX_NPCS = 30
        const val MAX_QUESTS = 20
        const val MAX_ITEMS = 20
        const val MAX_EVENTS = 15
        const val MAX_RELATIONSHIPS = 30
        const val MAX_SESSIONS = 10
        const val MAX_FIELD_CHARS = 240
    }

    /** Collects and renders the context for one generation request. */
    fun collect(campaignId: Long, type: GenerationType): String {
        val memory = campaignMemoryService.snapshot(campaignId)
        return render(memory, type)
    }

    private fun render(memory: CampaignMemorySnapshot, type: GenerationType): String {
        val sections = mutableListOf<String>()

        sections += buildString {
            appendLine("## CAMPAIGN")
            appendLine("Name: ${memory.campaign.name}")
            appendLine("System: ${memory.campaign.system}")
            if (memory.campaign.description.isNotBlank()) {
                appendLine("Premise: ${memory.campaign.description.truncate()}")
            }
            if (memory.campaign.worldLore.isNotBlank()) {
                appendLine("World lore: ${memory.campaign.worldLore.truncate()}")
            }
        }

        if (memory.locations.isNotEmpty()) {
            sections += buildString {
                appendLine("## EXISTING LOCATIONS (do not invent duplicates)")
                memory.locations.take(MAX_LOCATIONS).forEach { location ->
                    val parent = location.parent?.let { " (inside ${it.name})" } ?: ""
                    appendLine("- ${location.name} [${location.type}]$parent: ${location.description.truncate()}")
                }
            }
        }

        if (memory.factions.isNotEmpty()) {
            sections += buildString {
                appendLine("## FACTIONS AND POLITICAL SITUATION")
                memory.factions.take(MAX_FACTIONS).forEach { faction ->
                    appendLine(
                        "- ${faction.name} [${faction.status}], reputation with party " +
                            "${faction.reputationWithParty}: ${faction.goals.truncate()}",
                    )
                }
            }
        }

        if (memory.npcs.isNotEmpty()) {
            sections += buildString {
                appendLine("## EXISTING NPCS (reuse these where it makes sense; never duplicate a name)")
                memory.npcs.take(MAX_NPCS).forEach { npc ->
                    val where = npc.location?.let { ", in ${it.name}" } ?: ""
                    val faction = npc.faction?.let { ", member of ${it.name}" } ?: ""
                    appendLine(
                        "- ${npc.name} (${npc.race} ${npc.occupation}) [${npc.status}, " +
                            "${npc.disposition} to the party]$where$faction: ${npc.description.truncate()}",
                    )
                }
            }
        }

        if (type.needsRelationships && memory.relationships.isNotEmpty()) {
            sections += buildString {
                appendLine("## NPC RELATIONSHIPS")
                memory.relationships.take(MAX_RELATIONSHIPS).forEach { rel ->
                    appendLine("- ${rel.fromNpc.name} is ${rel.type} of ${rel.toNpc.name}: ${rel.description.truncate()}")
                }
            }
        }

        if (memory.quests.isNotEmpty()) {
            val quests = memory.quests.filter { it.status.name != "ABANDONED" }
            if (quests.isNotEmpty()) {
                sections += buildString {
                    appendLine("## QUESTS")
                    quests.take(MAX_QUESTS).forEach { quest ->
                        val giver = quest.questGiver?.let { ", given by ${it.name}" } ?: ""
                        appendLine("- ${quest.title} [${quest.status}]$giver: ${quest.objective.truncate()}")
                    }
                }
            }
        }

        if (memory.characters.isNotEmpty()) {
            sections += buildString {
                appendLine("## THE PARTY (player characters)")
                memory.characters.filter { it.active }.forEach { character ->
                    appendLine(
                        "- ${character.name}, level ${character.level} ${character.race} " +
                            "${character.characterClass}: ${character.backstory.truncate()}",
                    )
                }
            }
        }

        if (type.needsItems && memory.items.isNotEmpty()) {
            sections += buildString {
                appendLine("## KNOWN ITEMS (do not re-create these)")
                memory.items.take(MAX_ITEMS).forEach { item ->
                    val owner = item.ownerCharacter?.name ?: item.ownerNpc?.name ?: item.location?.name ?: "unclaimed"
                    appendLine("- ${item.name} [${item.rarity}], held by $owner")
                }
            }
        }

        if (memory.worldEvents.isNotEmpty()) {
            sections += buildString {
                appendLine("## WHAT HAS HAPPENED SO FAR")
                memory.worldEvents.takeLast(MAX_EVENTS).forEach { event ->
                    appendLine("- [${event.importance}] ${event.title}: ${event.consequences.truncate()}")
                }
            }
        }

        val recentSessions = memory.sessions.filter { it.summary.isNotBlank() }.takeLast(MAX_SESSIONS)
        if (recentSessions.isNotEmpty()) {
            sections += buildString {
                appendLine("## RECENT SESSION HISTORY")
                recentSessions.forEach { session ->
                    appendLine("- Session ${session.sessionNumber} (${session.title}): ${session.summary.truncate()}")
                }
            }
        }

        if (sections.size == 1) {
            sections += "## NOTE\nThis campaign has no worldbuilding stored yet. " +
                "Create something original that suits the premise above."
        }

        return sections.joinToString("\n")
    }

    private fun String.truncate(): String =
        if (length <= MAX_FIELD_CHARS) this else take(MAX_FIELD_CHARS).trimEnd() + "…"
}


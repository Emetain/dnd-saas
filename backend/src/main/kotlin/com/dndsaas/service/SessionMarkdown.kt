package com.dndsaas.service

import com.dndsaas.dto.GeneratedFirstSession
import com.dndsaas.dto.SessionDebrief

/**
 * Turns generated session plans and session debriefs into the Markdown that
 * is stored on a session and rendered in the frontend.
 */
object SessionMarkdown {

    /** A session plan as readable DM notes. */
    fun plan(plan: GeneratedFirstSession): String = buildString {
        section("Previously on…", plan.previouslyOn)
        section("What this session is about", plan.summary)
        section("Opening scene (read aloud)", plan.openingScene)
        if (plan.beats.isNotEmpty()) {
            appendLine("## Beats")
            plan.beats.forEachIndexed { index, beat -> appendLine("${index + 1}. $beat") }
            appendLine()
        }
        list("Encounters", plan.encounters)
        section("Cliffhanger", plan.cliffhanger)
        list("DM tips", plan.dmTips)
    }.trim()

    /** What happened in a session, from the DM's answers after playing it. */
    fun debrief(debrief: SessionDebrief): String = buildString {
        section("What happened", debrief.whatHappened)
        section("Quest progress", debrief.questProgress)
        section("NPCs the party dealt with", debrief.npcs)
        section("Key decisions", debrief.decisions)
        section("Where the session ended", debrief.endedAt)
        section("Loose threads and player plans", debrief.looseThreads)
        section("Changes to the party", debrief.partyChanges)
        section("Wishes for the next session", debrief.nextSessionWishes)
    }.trim()

    private fun StringBuilder.section(title: String, text: String) {
        if (text.isBlank()) return
        appendLine("## $title")
        appendLine(text.trim())
        appendLine()
    }

    private fun StringBuilder.list(title: String, items: List<String>) {
        if (items.isEmpty()) return
        appendLine("## $title")
        items.forEach { appendLine("- $it") }
        appendLine()
    }
}

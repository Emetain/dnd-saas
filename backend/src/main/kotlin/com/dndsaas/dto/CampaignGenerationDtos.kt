package com.dndsaas.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant

// ---------------------------------------------------------------------------
// STEP 1 — the interview
// ---------------------------------------------------------------------------

/**
 * The Dungeon Master's starting idea. Everything is optional except [idea],
 * and even that can be vague — the interview exists to draw out the rest.
 */
data class CampaignInterviewRequest(
    /** A rough pitch, e.g. "gothic horror where the party slowly becomes the villains". */
    val idea: String = "",
    val system: String = "D&D 5e",
    /** e.g. "dark and grim", "heroic", "comedic". */
    val tone: String = "",
    /** e.g. "betrayal, forbidden magic, family". */
    val themes: String = "",
    val playerCount: Int? = null,
    val startingLevel: Int? = null,
    /** Roughly how long the campaign should run, e.g. "one-shot", "10 sessions", "years". */
    val expectedLength: String = "",
    /** How many questions to ask back. */
    val questionCount: Int = 6,
    /** Interviewing for a one-shot rather than a full campaign (available on Free). */
    val oneShot: Boolean = false,
    val mock: Boolean = false,
)

/** One question the AI wants answered before it designs the campaign. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class InterviewQuestion(
    /** Stable key the client sends back with the answer. */
    val id: String = "",
    val question: String = "",
    /** Why this matters — helps the DM understand what is being asked. */
    val why: String = "",
    /** Ready-made answers the DM can pick instead of typing. */
    val suggestedAnswers: List<String> = emptyList(),
)

/** The questions the DM should answer, plus how the AI understood the pitch. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CampaignInterviewResponse(
    /** The AI's paraphrase of the idea, so the DM can correct misunderstandings. */
    val understanding: String = "",
    val questions: List<InterviewQuestion> = emptyList(),
    val usage: TokenUsage = TokenUsage(),
    val mocked: Boolean = false,
)

// ---------------------------------------------------------------------------
// STEP 2 — generating the campaign
// ---------------------------------------------------------------------------

/** One answer from the interview. Unanswered questions may simply be omitted. */
data class InterviewAnswer(
    val questionId: String = "",
    val question: String = "",
    val answer: String = "",
)

/**
 * Generate a full campaign from the original idea plus the interview answers.
 */
data class CampaignGenerationRequest(
    val idea: String = "",
    val system: String = "D&D 5e",
    val tone: String = "",
    val themes: String = "",
    val playerCount: Int? = null,
    val startingLevel: Int? = null,
    val expectedLength: String = "",
    /** Answers collected from the interview. Optional — generation works without them. */
    val answers: List<InterviewAnswer> = emptyList(),
    /** Anything else the DM wants to add in their own words. */
    val additionalNotes: String = "",
    /** Roughly how much starter world to create. */
    val locationCount: Int = 5,
    val factionCount: Int = 3,
    val npcCount: Int = 6,
    val questCount: Int = 4,
    val temperature: Double = 0.9,
    val mock: Boolean = false,
)

// --- The blueprint the model returns (parsed, then persisted) ---

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeneratedLocation(
    val name: String = "",
    val type: String = "OTHER",
    val description: String = "",
    val atmosphere: String = "",
    /** Name of the containing location; resolved to a real id when saved. */
    val parentName: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeneratedFaction(
    val name: String = "",
    val type: String = "",
    val description: String = "",
    val goals: String = "",
    val ideology: String = "",
    val headquartersName: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeneratedNpc(
    val name: String = "",
    val race: String = "",
    val occupation: String = "",
    val role: String = "",
    val description: String = "",
    val appearance: String = "",
    val personality: String = "",
    val motivation: String = "",
    val secret: String = "",
    val voice: String = "",
    val locationName: String? = null,
    val factionName: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeneratedQuest(
    val title: String = "",
    val description: String = "",
    val hook: String = "",
    val objective: String = "",
    val reward: String = "",
    val questGiverName: String? = null,
    val locationName: String? = null,
    val involvedNpcNames: List<String> = emptyList(),
)

/** A ready-to-run plan for the very first session. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GeneratedFirstSession(
    val title: String = "",
    val summary: String = "",
    /** Read-aloud text to start the session. */
    val openingScene: String = "",
    /** The beats the DM should hit, in order. */
    val beats: List<String> = emptyList(),
    val encounters: List<String> = emptyList(),
    /** How to end the session so players want the next one. */
    val cliffhanger: String = "",
    val dmTips: List<String> = emptyList(),
)

/** Everything the model produced for a new campaign. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CampaignBlueprint(
    val name: String = "",
    val premise: String = "",
    val worldLore: String = "",
    val tone: String = "",
    val locations: List<GeneratedLocation> = emptyList(),
    val factions: List<GeneratedFaction> = emptyList(),
    val npcs: List<GeneratedNpc> = emptyList(),
    val quests: List<GeneratedQuest> = emptyList(),
    val firstSession: GeneratedFirstSession = GeneratedFirstSession(),
)

/**
 * The result of generating a campaign: the saved campaign, what was written
 * into Campaign Memory, and the first session plan.
 */
data class CampaignGenerationResult(
    val campaign: CampaignResponse,
    val created: CampaignMemoryStats,
    val firstSession: SessionResponse,
    val firstSessionPlan: GeneratedFirstSession,
    val usage: TokenUsage,
    val model: String,
    val mocked: Boolean,
    val durationMillis: Long,
    val generatedAt: Instant,
)


package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.domain.CampaignKind
import com.dndsaas.domain.GenerationType
import com.dndsaas.domain.LocationType
import com.dndsaas.domain.QuestStatus
import com.dndsaas.domain.User
import com.dndsaas.dto.CampaignBlueprint
import com.dndsaas.dto.CampaignGenerationRequest
import com.dndsaas.dto.CampaignGenerationResult
import com.dndsaas.dto.GeneratedIdea
import com.dndsaas.dto.SavedIdeaResponse
import com.dndsaas.domain.SavedIdea
import com.dndsaas.repository.SavedIdeaRepository
import com.dndsaas.dto.CampaignInterviewRequest
import com.dndsaas.dto.CampaignInterviewResponse
import com.dndsaas.dto.CampaignMemoryStats
import com.dndsaas.dto.CampaignRequest
import com.dndsaas.dto.FactionRequest
import com.dndsaas.dto.GenerationRequest
import com.dndsaas.dto.LocationRequest
import com.dndsaas.dto.NpcRequest
import com.dndsaas.dto.QuestRequest
import com.dndsaas.dto.SessionRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The Campaign Generator — the first and most important generator module.
 *
 * It works in two steps:
 *
 *  1. [interview] — the DM gives a rough idea and the AI asks tailored
 *     questions back, so it understands the story before designing anything.
 *  2. [generate] — the idea plus the answers become a full campaign: lore,
 *     locations, factions, NPCs, quests and a ready-to-run first session.
 *
 * Crucially, the result is not a wall of text. Everything is written into
 * Campaign Memory as structured, connected rows, so every later generation is
 * already grounded in this world.
 */
@Service
class CampaignGeneratorService(
    private val aiService: AiService,
    private val objectMapper: ObjectMapper,
    private val campaignService: CampaignService,
    private val locationService: LocationService,
    private val factionService: FactionService,
    private val npcService: NpcService,
    private val questService: QuestService,
    private val sessionService: SessionService,
    private val savedIdeaRepository: SavedIdeaRepository,
) {

    private val log = LoggerFactory.getLogger(CampaignGeneratorService::class.java)

    // -----------------------------------------------------------------------
    // STEP 1 — the interview
    // -----------------------------------------------------------------------

    private val interviewSchema = """
        {
          "understanding": "one paragraph paraphrasing the DM's idea back to them",
          "questions": [
            {
              "id": "short_snake_case_key",
              "question": "the question to ask the Dungeon Master",
              "why": "one sentence on why this shapes the campaign",
              "suggestedAnswers": ["option 1", "option 2", "option 3"]
            }
          ]
        }
    """.trimIndent()

    private val interviewGuidance = """
        You are interviewing a Dungeon Master before designing their campaign.

        Ask the questions that will most change the shape of the campaign. Good
        questions cover: the central conflict, what the villain wants, why the
        party is involved, the kind of stories the table enjoys, what the players
        should feel, hard limits or content to avoid, and how the DM imagines the
        campaign ending.

        Rules for the questions:
        - Build on the DM's own idea; never ask what they already told you.
        - Ask about story and feel, not rules or statistics.
        - Keep each question to a single sentence.
        - Always offer 3 or 4 concrete suggested answers so the DM can just pick one.
        - Never ask more questions than requested.
    """.trimIndent()

    fun interview(request: CampaignInterviewRequest, user: User): CampaignInterviewResponse {
        validatePartySettings(request.playerCount, request.startingLevel)
        val instruction = buildString {
            appendLine(
                if (request.oneShot) "The Dungeon Master wants to build a one-shot for a single sitting."
                else "The Dungeon Master wants to build a new campaign.",
            )
            appendLine()
            appendLine("Their idea: ${request.idea.ifBlank { "(they have not said yet — ask broadly)" }}")
            appendLine("Game system: ${request.system}")
            if (request.tone.isNotBlank()) appendLine("Desired tone: ${request.tone}")
            if (request.themes.isNotBlank()) appendLine("Themes they like: ${request.themes}")
            request.playerCount?.let { appendLine("Number of players: $it") }
            request.startingLevel?.let { appendLine("Starting level: $it") }
            if (request.expectedLength.isNotBlank()) appendLine("Expected length: ${request.expectedLength}")
            appendLine()
            appendLine("Ask exactly ${request.questionCount.coerceIn(3, 10)} questions.")
        }

        val response = aiService.generate(
            campaign = null,
            request = GenerationRequest(
                type = if (request.oneShot) GenerationType.ONE_SHOT else GenerationType.CAMPAIGN,
                instruction = instruction,
                temperature = 0.7,
                mock = request.mock,
            ),
            jsonSchema = interviewSchema,
            requiredFields = listOf("questions"),
            systemGuidance = interviewGuidance,
            user = user,
            // Free: the interview is part of creating a campaign, which /generate charges for.
            tokenCost = 0,
        )

        val parsed = objectMapper.treeToValue(response.content, CampaignInterviewResponse::class.java)
        return parsed.copy(usage = response.usage, mocked = response.mocked)
    }

    /**
     * Suggests a fresh campaign or one-shot idea for the "Your idea" box,
     * shaped by whatever preferences the DM has already filled in.
     *
     * Costs [GenerationType.IDEA] tokens, so the idea is saved to the account
     * and can be reused later. Charging and saving share one transaction.
     */
    @Transactional
    fun suggestIdea(request: CampaignInterviewRequest, user: User): SavedIdeaResponse {
        validatePartySettings(request.playerCount, request.startingLevel)
        val instruction = buildString {
            appendLine(
                if (request.oneShot) "Suggest one original idea for a one-shot adventure for a single sitting."
                else "Suggest one original idea for a campaign.",
            )
            appendLine("Game system: ${request.system}")
            if (request.tone.isNotBlank()) appendLine("Tone: ${request.tone}")
            if (request.themes.isNotBlank()) appendLine("Themes: ${request.themes}")
            request.playerCount?.let { appendLine("Players: $it") }
            request.startingLevel?.let { appendLine("Starting level: $it") }
            if (request.idea.isNotBlank()) appendLine("Suggest something clearly different from: ${request.idea}")
            appendLine()
            appendLine(
                if (request.oneShot) "Choose a play time in hours (1-$MAX_ONE_SHOT_HOURS) that fits the idea."
                else "Choose a campaign length in sessions (1-$MAX_CAMPAIGN_SESSIONS) that fits the idea.",
            )
        }

        val response = aiService.generate(
            campaign = null,
            request = GenerationRequest(
                type = GenerationType.IDEA,
                instruction = instruction,
                temperature = 1.0,
                mock = request.mock,
            ),
            jsonSchema = ideaSchema(request.oneShot),
            requiredFields = listOf("idea"),
            systemGuidance = ideaGuidance,
            user = user,
        )
        val generated = objectMapper.treeToValue(response.content, GeneratedIdea::class.java)
        val maxLength = if (request.oneShot) MAX_ONE_SHOT_HOURS else MAX_CAMPAIGN_SESSIONS
        val saved = savedIdeaRepository.save(
            SavedIdea(
                user = user,
                kind = if (request.oneShot) CampaignKind.ONE_SHOT else CampaignKind.CAMPAIGN,
                idea = generated.idea.trim(),
                // What the DM already chose always wins over the model's suggestion.
                tone = request.tone.ifBlank { generated.tone.trim() },
                themes = request.themes.ifBlank { generated.themes.trim() },
                length = generated.length?.coerceIn(1, maxLength),
            ),
        )
        return toIdeaResponse(saved)
    }

    /** The user's saved ideas, newest first; optionally only one kind. */
    @Transactional(readOnly = true)
    fun savedIdeas(userId: Long, kind: CampaignKind?): List<SavedIdeaResponse> =
        (if (kind == null) savedIdeaRepository.findByUserIdOrderByCreatedAtDesc(userId)
        else savedIdeaRepository.findByUserIdAndKindOrderByCreatedAtDesc(userId, kind))
            .map(::toIdeaResponse)

    /** Removes one of the user's saved ideas. Another user's idea counts as not found. */
    @Transactional
    fun deleteIdea(userId: Long, ideaId: Long) {
        val idea = savedIdeaRepository.findById(ideaId)
            .filter { it.user?.id == userId }
            .orElseThrow { NoSuchElementException("Idea $ideaId not found") }
        savedIdeaRepository.delete(idea)
    }

    private fun toIdeaResponse(idea: SavedIdea) =
        SavedIdeaResponse(
            id = idea.id!!,
            kind = idea.kind,
            idea = idea.idea,
            tone = idea.tone.orEmpty(),
            themes = idea.themes.orEmpty(),
            length = idea.length,
            createdAt = idea.createdAt,
        )

    private fun ideaSchema(oneShot: Boolean) = """
        {
          "idea": "one or two sentences pitching the idea, with a hook and a twist",
          "tone": "two to four words for the feel, e.g. dark and tense",
          "themes": "two or three comma-separated themes, e.g. betrayal, forbidden magic",
          "length": ${if (oneShot) "play time in hours, a whole number from 1 to $MAX_ONE_SHOT_HOURS" else "number of sessions, a whole number from 1 to $MAX_CAMPAIGN_SESSIONS"}
        }
    """.trimIndent()

    private val ideaGuidance = """
        You suggest ideas that make a Dungeon Master want to run them immediately.
        - One or two sentences, never more.
        - Give it a clear hook and one surprising twist.
        - Avoid the obvious clichés (a tavern meeting, a generic dark lord, "save the princess").
        - Honour any tone and themes the DM has already chosen, and return them unchanged.
        - Otherwise choose a tone and themes that fit the idea.
        - Choose a length that suits the scope of the idea.
    """.trimIndent()

    // -----------------------------------------------------------------------
    // STEP 2 — generating the campaign
    // -----------------------------------------------------------------------

    private fun blueprintSchema(request: CampaignGenerationRequest) = """
        {
          "name": "an evocative campaign title",
          "premise": "2-3 sentences a DM could pitch to their players",
          "worldLore": "6-10 paragraphs of setting: history, geography, powers, the central conflict, what is at stake",
          "tone": "a few words describing the feel",
          "locations": [
            {
              "name": "unique name",
              "type": "one of CONTINENT, REGION, CITY, TOWN, VILLAGE, DISTRICT, BUILDING, DUNGEON, LANDMARK, PLANE, OTHER",
              "description": "what it is and why it matters",
              "atmosphere": "sights, sounds and smells",
              "parentName": "name of a location in this same list that contains it, or null"
            }
          ],
          "factions": [
            {
              "name": "unique name",
              "type": "e.g. Cult, Noble House, Guild",
              "description": "who they are",
              "goals": "what they are trying to achieve",
              "ideology": "what they believe",
              "headquartersName": "name of one of the locations above, or null"
            }
          ],
          "npcs": [
            {
              "name": "unique name",
              "race": "",
              "occupation": "",
              "role": "e.g. Villain, Quest giver, Ally",
              "description": "",
              "appearance": "",
              "personality": "",
              "motivation": "what drives them",
              "secret": "something the players do not know",
              "voice": "how they speak",
              "locationName": "one of the locations above, or null",
              "factionName": "one of the factions above, or null"
            }
          ],
          "quests": [
            {
              "title": "",
              "description": "",
              "hook": "how the party hears about it",
              "objective": "what they must do",
              "reward": "",
              "questGiverName": "one of the NPCs above, or null",
              "locationName": "one of the locations above, or null",
              "involvedNpcNames": ["names of NPCs above"]
            }
          ],
          "firstSession": {
            "title": "",
            "summary": "what this session is about",
            "openingScene": "read-aloud text to start the session",
            "beats": ["the scenes to hit, in order"],
            "encounters": ["combat, social or exploration encounters"],
            "cliffhanger": "how to end so players want more",
            "dmTips": ["practical advice for running this session"]
          }
        }

        Produce exactly ${request.locationCount} locations, ${request.factionCount} factions,
        ${request.npcCount} NPCs and ${request.questCount} quests.
    """.trimIndent()

    private val blueprintGuidance = """
        You are designing a brand new campaign from the Dungeon Master's idea and
        their interview answers.

        Requirements:
        - Honour the DM's answers. They outrank your own instincts, and if they
          named something (a villain, a place, a theme) it MUST appear.
        - Everything must interconnect: NPCs belong to locations and factions,
          quests are given by NPCs and happen in locations. Reference them by the
          exact names you used elsewhere in the response, never invent a name in
          a reference field that does not appear in its own list.
        - Every name must be unique across the whole response.
        - Give the villain a concrete, achievable plan that is already in motion.
        - The first session must be genuinely runnable: an opening scene, several
          beats, at least one encounter, and a hook into an ongoing quest.
        - Write the world lore as flowing prose, not bullet points.
    """.trimIndent()

    private val oneShotGuidance = """
        This is a one-shot for a single sitting, not the opening chapter of a campaign.

        Requirements:
        - Keep the scope compact enough to finish in the expected play time.
        - Give the party a clear objective, meaningful escalation and a decisive finale.
        - Make every generated NPC, location and quest serve this one adventure.
        - Use the cliffhanger field for the complete resolution and aftermath, not a sequel hook.
        - The first session plan must cover the entire adventure from opening scene to ending.
    """.trimIndent()

    @Transactional
    fun generate(request: CampaignGenerationRequest, user: User): CampaignGenerationResult =
        generateInternal(request, user, GenerationType.CAMPAIGN, oneShot = false)

    /**
     * Generates a self-contained one-shot: a small, fully connected world sized
     * for a single sitting, with an ending baked in rather than a cliffhanger.
     *
     * Reuses the same blueprint shape and persistence as a full campaign — a
     * one-shot is simply a campaign with tighter scope and a resolution.
     */
    @Transactional
    fun generateOneShot(request: CampaignGenerationRequest, user: User): CampaignGenerationResult =
        generateInternal(
            request.copy(
                locationCount = request.locationCount.coerceAtMost(3),
                factionCount = request.factionCount.coerceAtMost(1),
                npcCount = request.npcCount.coerceAtMost(4),
                questCount = request.questCount.coerceAtMost(1),
                additionalNotes = buildString {
                    append(request.additionalNotes)
                    if (request.additionalNotes.isNotBlank()) appendLine()
                    append(
                        "This is a ONE-SHOT: everything must be resolvable in a single sitting. " +
                            "The first (only) session must end with a genuine resolution, not a " +
                            "cliffhanger — put the ending in the cliffhanger field.",
                    )
                },
            ),
            user,
            GenerationType.ONE_SHOT,
            oneShot = true,
        )

    private fun generateInternal(
        request: CampaignGenerationRequest,
        user: User,
        generationType: GenerationType,
        oneShot: Boolean,
    ): CampaignGenerationResult {
        validatePartySettings(request.playerCount, request.startingLevel)
        val startedAt = System.currentTimeMillis()

        val instruction = buildString {
            appendLine(if (oneShot) "Design a self-contained one-shot adventure." else "Design a complete campaign.")
            appendLine()
            appendLine("## The Dungeon Master's idea")
            appendLine(request.idea.ifBlank { "(none given — invent something memorable)" })
            appendLine()
            appendLine("## Preferences")
            appendLine("Game system: ${request.system}")
            if (request.tone.isNotBlank()) appendLine("Tone: ${request.tone}")
            if (request.themes.isNotBlank()) appendLine("Themes: ${request.themes}")
            request.playerCount?.let { appendLine("Players: $it") }
            request.startingLevel?.let { appendLine("Starting level: $it") }
            if (request.expectedLength.isNotBlank()) appendLine("Expected length: ${request.expectedLength}")

            if (request.answers.isNotEmpty()) {
                appendLine()
                appendLine("## Interview answers (these are decisions, not suggestions — honour them)")
                request.answers
                    .filter { it.answer.isNotBlank() }
                    .forEach { answer ->
                        appendLine("- ${answer.question.ifBlank { answer.questionId }}")
                        appendLine("  Answer: ${answer.answer}")
                    }
            }

            if (request.additionalNotes.isNotBlank()) {
                appendLine()
                appendLine("## Additional notes from the DM")
                appendLine(request.additionalNotes)
            }
        }

        val response = aiService.generate(
            campaign = null,
            request = GenerationRequest(
                type = generationType,
                instruction = instruction,
                temperature = request.temperature,
                mock = request.mock,
            ),
            jsonSchema = blueprintSchema(request),
            requiredFields = listOf("name", "worldLore"),
            systemGuidance = if (oneShot) "$blueprintGuidance\n\n$oneShotGuidance" else blueprintGuidance,
            user = user,
        )

        val blueprint = objectMapper.treeToValue(response.content, CampaignBlueprint::class.java)
        val result = persist(blueprint, request, user, if (oneShot) CampaignKind.ONE_SHOT else CampaignKind.CAMPAIGN)

        return CampaignGenerationResult(
            campaign = result.campaign,
            created = result.stats,
            firstSession = result.session,
            firstSessionPlan = blueprint.firstSession,
            usage = response.usage,
            model = response.model,
            mocked = response.mocked,
            durationMillis = System.currentTimeMillis() - startedAt,
            generatedAt = response.createdAt,
        )
    }

    // -----------------------------------------------------------------------
    // Persistence — turning the blueprint into structured Campaign Memory
    // -----------------------------------------------------------------------

    private data class PersistResult(
        val campaign: com.dndsaas.dto.CampaignResponse,
        val stats: CampaignMemoryStats,
        val session: com.dndsaas.dto.SessionResponse,
    )

    private fun persist(
        blueprint: CampaignBlueprint,
        request: CampaignGenerationRequest,
        owner: User,
        kind: CampaignKind,
    ): PersistResult {
        // --- The campaign itself ---
        val campaignResponse = campaignService.create(
            CampaignRequest(
                name = blueprint.name.ifBlank { "Untitled Campaign" },
                description = blueprint.premise,
                system = request.system,
                worldLore = blueprint.worldLore,
                kind = kind,
            ),
            owner,
        )
        val campaign: Campaign = campaignService.findEntity(campaignResponse.id)

        // --- Locations, parents resolved after every location exists ---
        val locationIdsByName = mutableMapOf<String, Long>()
        blueprint.locations.filter { it.name.isNotBlank() }.forEach { generated ->
            val saved = locationService.create(
                campaign,
                LocationRequest(
                    name = generated.name,
                    type = parseLocationType(generated.type),
                    description = generated.description,
                    atmosphere = generated.atmosphere,
                    discovered = false,
                ),
            )
            locationIdsByName[generated.name.lowercase()] = saved.id
        }
        blueprint.locations
            .filter { it.parentName != null && it.name.isNotBlank() }
            .forEach { generated ->
                val id = locationIdsByName[generated.name.lowercase()] ?: return@forEach
                val parentId = locationIdsByName[generated.parentName!!.lowercase()]
                if (parentId == null || parentId == id) return@forEach
                val current = locationService.findEntity(id)
                locationService.update(
                    id,
                    LocationRequest(
                        name = current.name,
                        type = current.type,
                        description = current.description,
                        atmosphere = current.atmosphere,
                        parentId = parentId,
                        notes = current.notes,
                        discovered = current.discovered,
                    ),
                )
            }

        // --- Factions ---
        val factionIdsByName = mutableMapOf<String, Long>()
        blueprint.factions.filter { it.name.isNotBlank() }.forEach { generated ->
            val saved = factionService.create(
                campaign,
                FactionRequest(
                    name = generated.name,
                    type = generated.type,
                    description = generated.description,
                    goals = generated.goals,
                    ideology = generated.ideology,
                    headquartersId = generated.headquartersName?.let { locationIdsByName[it.lowercase()] },
                ),
            )
            factionIdsByName[generated.name.lowercase()] = saved.id
        }

        // --- NPCs ---
        val npcIdsByName = mutableMapOf<String, Long>()
        blueprint.npcs.filter { it.name.isNotBlank() }.forEach { generated ->
            val saved = npcService.create(
                campaign,
                NpcRequest(
                    name = generated.name,
                    race = generated.race,
                    occupation = generated.occupation,
                    role = generated.role,
                    description = generated.description,
                    appearance = generated.appearance,
                    personality = generated.personality,
                    motivation = generated.motivation,
                    secret = generated.secret,
                    voice = generated.voice,
                    locationId = generated.locationName?.let { locationIdsByName[it.lowercase()] },
                    factionId = generated.factionName?.let { factionIdsByName[it.lowercase()] },
                ),
                aiGenerated = true,
            )
            npcIdsByName[generated.name.lowercase()] = saved.id
        }

        // --- Quests ---
        blueprint.quests.filter { it.title.isNotBlank() }.forEach { generated ->
            questService.create(
                campaign,
                QuestRequest(
                    title = generated.title,
                    description = generated.description,
                    hook = generated.hook,
                    objective = generated.objective,
                    status = QuestStatus.AVAILABLE,
                    questGiverId = generated.questGiverName?.let { npcIdsByName[it.lowercase()] },
                    locationId = generated.locationName?.let { locationIdsByName[it.lowercase()] },
                    involvedNpcIds = generated.involvedNpcNames.mapNotNull { npcIdsByName[it.lowercase()] },
                    reward = generated.reward,
                ),
                aiGenerated = true,
            )
        }

        // --- Session 1, stored as a real session with the plan in its notes ---
        val plan = blueprint.firstSession
        val session = sessionService.create(
            campaignResponse.id,
            SessionRequest(
                title = plan.title.ifBlank { "Session 1" },
                sessionNumber = 1,
                notes = SessionMarkdown.plan(plan),
                // The summary records what happened; it is filled in by the debrief after playing.
                summary = "",
            ),
        )



        log.info(
            "Generated campaign '{}' (id={}) with {} locations, {} factions, {} NPCs, {} quests",
            campaignResponse.name, campaignResponse.id,
            locationIdsByName.size, factionIdsByName.size, npcIdsByName.size, blueprint.quests.size,
        )

        val stats = CampaignMemoryStats(
            locations = locationIdsByName.size.toLong(),
            factions = factionIdsByName.size.toLong(),
            npcs = npcIdsByName.size.toLong(),
            quests = blueprint.quests.count { it.title.isNotBlank() }.toLong(),
            items = 0,
            worldEvents = 0,
            relationships = 0,
            sessions = 1,
            characters = 0,
        )

        return PersistResult(
            campaign = campaignService.get(campaignResponse.id),
            stats = stats,
            session = session,
        )
    }

    /** The same limits the frontend offers: up to 10 players, levels 1 to 20. */
    private fun validatePartySettings(playerCount: Int?, startingLevel: Int?) {
        playerCount?.let { require(it in 1..MAX_PLAYERS) { "Player count must be between 1 and $MAX_PLAYERS" } }
        startingLevel?.let { require(it in 1..MAX_LEVEL) { "Starting level must be between 1 and $MAX_LEVEL" } }
    }

    private companion object {
        const val MAX_PLAYERS = 10
        const val MAX_LEVEL = 20
        const val MAX_ONE_SHOT_HOURS = 20
        const val MAX_CAMPAIGN_SESSIONS = 100
    }

    /** Maps the model's free-text type onto the enum, falling back safely. */
    private fun parseLocationType(raw: String): LocationType =
        runCatching { LocationType.valueOf(raw.trim().uppercase().replace(' ', '_')) }
            .getOrDefault(LocationType.OTHER)
}


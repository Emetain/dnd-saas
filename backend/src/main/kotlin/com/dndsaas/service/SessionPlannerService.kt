package com.dndsaas.service

import com.dndsaas.domain.CampaignKind
import com.dndsaas.domain.GenerationType
import com.dndsaas.domain.Session
import com.dndsaas.dto.GeneratedFirstSession
import com.dndsaas.dto.GenerationRequest
import com.dndsaas.dto.NextSessionRequest
import com.dndsaas.dto.NextSessionResult
import com.dndsaas.repository.SessionRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Plans the next session of a campaign.
 *
 * Most of the world already exists, so this is cheaper than generating a
 * campaign: it builds on Campaign Memory (collected automatically by the AI
 * pipeline), the campaign's original idea, and — most importantly — the DM's
 * debrief of what actually happened in the last session.
 */
@Service
class SessionPlannerService(
    private val aiService: AiService,
    private val campaignService: CampaignService,
    private val sessionRepository: SessionRepository,
    private val sessionService: SessionService,
    private val objectMapper: ObjectMapper,
) {

    @Transactional
    fun planNext(campaignId: Long, request: NextSessionRequest): NextSessionResult {
        val campaign = campaignService.findEntity(campaignId)
        require(campaign.kind == CampaignKind.CAMPAIGN) {
            "Next sessions are for campaigns — a one-shot is played in a single session"
        }

        val sessions = sessionRepository.findByCampaignIdOrderBySessionNumberAsc(campaignId)
        val last = sessions.lastOrNull()
        require(last == null || !last.debrief.isNullOrBlank()) {
            "Add notes about what happened in Session ${last!!.sessionNumber} first — the next session builds on them"
        }

        val instruction = buildString {
            appendLine("Plan the next session of this campaign: Session ${(last?.sessionNumber ?: 0) + 1}.")
            appendLine()
            appendLine("## The campaign's idea")
            appendLine(campaign.description.ifBlank { campaign.name })
            if (last != null) {
                appendLine()
                appendLine("## What happened in the last session (Session ${last.sessionNumber}: ${last.title})")
                appendLine(last.summary)
                if (last.notes.isNotBlank()) {
                    appendLine()
                    appendLine("## What had been planned for that session")
                    appendLine(last.notes.take(PLAN_CONTEXT_CHARACTERS))
                }
            }
        }

        val response = aiService.generate(
            campaign = campaign,
            request = GenerationRequest(
                type = GenerationType.NEXT_SESSION,
                instruction = instruction,
                temperature = 0.8,
                mock = request.mock,
            ),
            jsonSchema = schema,
            requiredFields = listOf("title", "openingScene"),
            systemGuidance = guidance,
        )

        val plan = objectMapper.treeToValue(response.content, GeneratedFirstSession::class.java)
        val nextNumber = (last?.sessionNumber ?: 0) + 1
        val session = sessionRepository.save(
            Session(
                campaign = campaign,
                title = plan.title.ifBlank { "Session $nextNumber" },
                sessionNumber = nextNumber,
                notes = SessionMarkdown.plan(plan),
                // Left empty on purpose: the summary is what *happened*, filled in by the debrief.
                summary = "",
            ),
        )

        return NextSessionResult(
            session = sessionService.toResponse(session),
            plan = plan,
            usage = response.usage,
            model = response.model,
            mocked = response.mocked,
        )
    }

    private val schema = """
        {
          "title": "an evocative title for this session",
          "previouslyOn": "a short 'Previously on…' recap to read aloud to the players, in second person",
          "summary": "what this session is about, for the DM",
          "openingScene": "read-aloud text that picks up exactly where the last session ended",
          "beats": ["the scenes to hit, in order"],
          "encounters": ["combat, social or exploration encounters"],
          "cliffhanger": "how to end so players want more",
          "dmTips": ["practical advice for running this session"]
        }
    """.trimIndent()

    private val guidance = """
        You are planning the next session of a campaign that is already being played.

        Requirements:
        - Continue from where the last session ended; never ignore or undo what the players did.
        - Pay off the players' decisions, react to how they treated NPCs, and follow up loose threads.
        - Honour the DM's wishes for this session.
        - Reuse existing NPCs, locations, factions and quests by their exact names before inventing new ones.
        - Keep the campaign's original idea and central conflict moving forward.
        - The session must be runnable: an opening scene, several beats and at least one encounter.
    """.trimIndent()

    private companion object {
        /** Enough of the previous plan to show what was skipped, without drowning the prompt. */
        const val PLAN_CONTEXT_CHARACTERS = 2_000
    }
}

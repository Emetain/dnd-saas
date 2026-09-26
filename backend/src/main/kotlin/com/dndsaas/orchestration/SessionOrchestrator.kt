package com.dndsaas.orchestration

import com.dndsaas.dto.NextSessionRequest
import com.dndsaas.dto.NextSessionResult
import com.dndsaas.dto.SessionDebrief
import com.dndsaas.dto.SessionRequest
import com.dndsaas.dto.SessionResponse
import com.dndsaas.dto.SessionRewriteRequest
import com.dndsaas.service.SessionPlannerService
import com.dndsaas.service.SessionService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for sessions.
 */
@Component
class SessionOrchestrator(
    private val sessionService: SessionService,
    private val sessionPlannerService: SessionPlannerService,
) {
    fun createSession(campaignId: Long, request: SessionRequest): SessionResponse =
        sessionService.create(campaignId, request)

    fun listSessions(campaignId: Long): List<SessionResponse> =
        sessionService.listForCampaign(campaignId)

    fun getSession(id: Long): SessionResponse = sessionService.get(id)

    fun updateSession(id: Long, request: SessionRequest): SessionResponse =
        sessionService.update(id, request)

    fun deleteSession(id: Long) = sessionService.delete(id)

    /** Task: record what happened in a session after it was played. */
    fun saveDebrief(id: Long, debrief: SessionDebrief): SessionResponse = sessionService.saveDebrief(id, debrief)

    fun setCompleted(id: Long, completed: Boolean): SessionResponse = sessionService.setCompleted(id, completed)

    /** Task: re-plan a session that has not been played yet. */
    fun rewriteSession(id: Long, request: SessionRewriteRequest): NextSessionResult =
        sessionPlannerService.rewrite(id, request)

    /** Task: plan the next session from the campaign and the last debrief. */
    fun planNextSession(campaignId: Long, request: NextSessionRequest): NextSessionResult =
        sessionPlannerService.planNext(campaignId, request)
}


package com.dndsaas.orchestration

import com.dndsaas.dto.SessionRequest
import com.dndsaas.dto.SessionResponse
import com.dndsaas.service.SessionService
import org.springframework.stereotype.Component

/**
 * Orchestration (middle) layer for sessions.
 */
@Component
class SessionOrchestrator(
    private val sessionService: SessionService,
) {
    fun createSession(campaignId: Long, request: SessionRequest): SessionResponse =
        sessionService.create(campaignId, request)

    fun listSessions(campaignId: Long): List<SessionResponse> =
        sessionService.listForCampaign(campaignId)

    fun getSession(id: Long): SessionResponse = sessionService.get(id)

    fun updateSession(id: Long, request: SessionRequest): SessionResponse =
        sessionService.update(id, request)

    fun deleteSession(id: Long) = sessionService.delete(id)
}


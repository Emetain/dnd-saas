package com.dndsaas.service

import com.dndsaas.domain.Session
import com.dndsaas.dto.SessionDebrief
import com.dndsaas.dto.SessionRequest
import com.dndsaas.dto.SessionResponse
import com.dndsaas.repository.SessionRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Business logic for sessions within a campaign.
 */
@Service
@Transactional
class SessionService(
    private val memoryReferences: MemoryReferences,
    private val sessionRepository: SessionRepository,
    private val campaignService: CampaignService,
    private val objectMapper: ObjectMapper,
) {

    fun create(campaignId: Long, request: SessionRequest): SessionResponse {
        val campaign = campaignService.findEntity(campaignId)
        val session = Session(
            campaign = campaign,
            title = request.title,
            sessionNumber = request.sessionNumber,
            date = request.date,
            notes = request.notes,
            summary = request.summary,
        )
        return toResponse(sessionRepository.save(session))
    }

    @Transactional(readOnly = true)
    fun listForCampaign(campaignId: Long): List<SessionResponse> =
        sessionRepository.findByCampaignIdOrderBySessionNumberAsc(campaignId).map(::toResponse)

    @Transactional(readOnly = true)
    fun get(id: Long): SessionResponse = toResponse(findEntity(id))

    fun update(id: Long, request: SessionRequest): SessionResponse {
        val session = findEntity(id)
        session.title = request.title
        session.sessionNumber = request.sessionNumber
        session.date = request.date
        session.notes = request.notes
        session.summary = request.summary
        return toResponse(sessionRepository.save(session))
    }

    /**
     * Records what happened in a session. The answers are kept for editing, and
     * rendered into [Session.summary], which the AI reads as campaign history.
     */
    fun saveDebrief(id: Long, debrief: SessionDebrief): SessionResponse {
        require(debrief.whatHappened.isNotBlank()) { "Describe what happened in the session" }
        val session = findEntity(id)
        session.debrief = objectMapper.writeValueAsString(debrief)
        session.summary = SessionMarkdown.debrief(debrief)
        // Notes on what happened mean it was played.
        session.completed = true
        return toResponse(sessionRepository.save(session))
    }

    fun setCompleted(id: Long, completed: Boolean): SessionResponse {
        val session = findEntity(id)
        session.completed = completed
        return toResponse(sessionRepository.save(session))
    }

    /** Deletes a session and moves the ones after it up, so the numbers stay 1, 2, 3… */
    fun delete(id: Long) {
        val session = findEntity(id)
        val campaignId = session.campaign!!.id!!
        val number = session.sessionNumber
        memoryReferences.detachSession(id)
        sessionRepository.delete(session)
        sessionRepository.flush()
        sessionRepository.findByCampaignIdOrderBySessionNumberAsc(campaignId)
            .filter { it.sessionNumber > number }
            .forEach { it.sessionNumber-- }
    }

    fun findEntity(id: Long): Session =
        sessionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Session $id not found") }

    fun toResponse(session: Session): SessionResponse =
        SessionResponse(
            id = session.id!!,
            campaignId = session.campaign!!.id!!,
            title = session.title,
            sessionNumber = session.sessionNumber,
            date = session.date,
            notes = session.notes,
            summary = session.summary,
            debrief = session.debrief?.let { objectMapper.readValue(it, SessionDebrief::class.java) },
            completed = session.isPlayed,
        )
}


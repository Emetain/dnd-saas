package com.dndsaas.service

import com.dndsaas.domain.Session
import com.dndsaas.dto.SessionRequest
import com.dndsaas.dto.SessionResponse
import com.dndsaas.repository.SessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Business logic for sessions within a campaign.
 */
@Service
@Transactional
class SessionService(
    private val sessionRepository: SessionRepository,
    private val campaignService: CampaignService,
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

    fun delete(id: Long) = sessionRepository.deleteById(id)

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
        )
}


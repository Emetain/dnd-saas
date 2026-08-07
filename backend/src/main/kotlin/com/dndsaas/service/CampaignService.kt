package com.dndsaas.service

import com.dndsaas.domain.Campaign
import com.dndsaas.dto.CampaignRequest
import com.dndsaas.dto.CampaignResponse
import com.dndsaas.repository.CampaignRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Business logic for campaigns. Handles persistence and mapping to DTOs.
 */
@Service
@Transactional
class CampaignService(
    private val campaignRepository: CampaignRepository,
) {

    fun create(request: CampaignRequest): CampaignResponse {
        val campaign = Campaign(
            name = request.name,
            description = request.description,
            system = request.system,
            worldLore = request.worldLore,
        )
        return toResponse(campaignRepository.save(campaign))
    }

    fun update(id: Long, request: CampaignRequest): CampaignResponse {
        val campaign = findEntity(id)
        campaign.name = request.name
        campaign.description = request.description
        campaign.system = request.system
        campaign.worldLore = request.worldLore
        return toResponse(campaignRepository.save(campaign))
    }

    @Transactional(readOnly = true)
    fun get(id: Long): CampaignResponse = toResponse(findEntity(id))

    @Transactional(readOnly = true)
    fun list(): List<CampaignResponse> = campaignRepository.findAll().map(::toResponse)

    fun delete(id: Long) = campaignRepository.deleteById(id)

    /** Shared lookup used by this and other services. */
    fun findEntity(id: Long): Campaign =
        campaignRepository.findById(id)
            .orElseThrow { NoSuchElementException("Campaign $id not found") }

    fun toResponse(campaign: Campaign): CampaignResponse =
        CampaignResponse(
            id = campaign.id!!,
            name = campaign.name,
            description = campaign.description,
            system = campaign.system,
            worldLore = campaign.worldLore,
            sessionCount = campaign.sessions.size,
            characterCount = campaign.characters.size,
            createdAt = campaign.createdAt,
            updatedAt = campaign.updatedAt,
        )
}


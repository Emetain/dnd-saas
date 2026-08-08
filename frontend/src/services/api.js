/**
 * Frontend API service layer.
 * Centralizes all HTTP calls to the backend so components stay clean.
 * The `/api` prefix is proxied to the Spring Boot server by Vite (see vite.config.js).
 */

const BASE_URL = '/api/v1'

async function request(path, options = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!response.ok) {
    let message = `Request failed: ${response.status}`
    try {
      const body = await response.json()
      message = body.error || body.message || message
    } catch {
      // Response was not JSON; keep the status-based message.
    }
    throw new Error(message)
  }
  // 204 No Content guard
  if (response.status === 204) return null
  return response.json()
}

export const campaignApi = {
  list() {
    return request('/campaigns')
  },
  get(id) {
    return request(`/campaigns/${id}`)
  },
  create(campaign) {
    return request('/campaigns', {
      method: 'POST',
      body: JSON.stringify(campaign),
    })
  },
}

/**
 * Characters belong to a campaign, so creating and listing are campaign-scoped.
 */
export const characterApi = {
  listForCampaign(campaignId) {
    return request(`/campaigns/${campaignId}/characters`)
  },
  get(id) {
    return request(`/characters/${id}`)
  },
  create(campaignId, character) {
    return request(`/campaigns/${campaignId}/characters`, {
      method: 'POST',
      body: JSON.stringify(character),
    })
  },
  remove(id) {
    return request(`/characters/${id}`, { method: 'DELETE' })
  },
}

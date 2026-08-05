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
    const message = await response.text()
    throw new Error(message || `Request failed: ${response.status}`)
  }
  // 204 No Content guard
  if (response.status === 204) return null
  return response.json()
}

export const characterApi = {
  list() {
    return request('/characters')
  },
  get(id) {
    return request(`/characters/${id}`)
  },
  create(character) {
    return request('/characters', {
      method: 'POST',
      body: JSON.stringify(character),
    })
  },
}


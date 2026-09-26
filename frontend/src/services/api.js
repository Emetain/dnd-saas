/**
 * Frontend API service layer.
 * Centralizes all HTTP calls to the backend so components stay clean.
 * The `/api` prefix is proxied to the Spring Boot server by Vite (see vite.config.js).
 */

const BASE_URL = '/api/v1'

/**
 * Who is signed in travels in an HttpOnly session cookie that the browser sends
 * by itself. When the backend answers 401 (signed out or expired), this handler
 * — set by the session store — takes the user back to sign-in.
 */
let onUnauthorized = () => {}
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

/** An API failure that keeps the HTTP status, so views can react to 402/403. */
export class ApiError extends Error {
  constructor(message, status, body) {
    super(message)
    this.status = status
    this.body = body
  }
}

async function request(path, options = {}) {
  // expectSignedOut: a 401 is a normal answer here (sign-in form, first visit), not a lost session.
  const { expectSignedOut, ...fetchOptions } = options
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }

  const response = await fetch(`${BASE_URL}${path}`, { ...fetchOptions, headers, credentials: 'same-origin' })
  if (response.status === 401 && !expectSignedOut) onUnauthorized()
  if (!response.ok) {
    let message = `Request failed: ${response.status}`
    let body = null
    try {
      body = await response.json()
      message = body.error || body.message || message
    } catch {
      // Response was not JSON; keep the status-based message.
    }
    throw new ApiError(message, response.status, body)
  }
  // 204 No Content guard
  if (response.status === 204) return null
  const text = await response.text()
  return text ? JSON.parse(text) : null
}

const post = (path, body) => request(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })
const put = (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) })

export const authApi = {
  /** Creates the account and signs in. */
  register: (form) => post('/auth/register', form),
  login: (email, password) => request('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }), expectSignedOut: true }),
  logout: () => post('/auth/logout'),
  /** The signed-in user; 401 when signed out, which is normal on a first visit. */
  me: () => request('/auth/me', { expectSignedOut: true }),
  updateProfile: (displayName) => put('/auth/me', { displayName }),
  changePassword: (currentPassword, newPassword) => put('/auth/password', { currentPassword, newPassword }),
  verifyEmail: (token) => post('/auth/verify-email', { token }),
  resendVerification: () => post('/auth/resend-verification'),
  /** Always succeeds, whether or not the address has an account. */
  forgotPassword: (email) => post('/auth/forgot-password', { email }),
  resetPassword: (token, newPassword) => post('/auth/reset-password', { token, newPassword }),
}

export const userApi = {
  get: (id) => request(`/users/${id}`),
}

export const tokenApi = {
  balance: (userId) => request(`/users/${userId}/tokens`),
  history: (userId, limit = 50) => request(`/users/${userId}/tokens/history?limit=${limit}`),
  purchase: (userId, pack) => post(`/users/${userId}/tokens/purchase`, { pack }),
}

/** Real payments through Stripe (when the backend has a Stripe key). */
export const paymentApi = {
  config: () => request('/billing/config'),
  /** Returns { url } — send the browser there. Pass { tier } or { pack }. */
  checkout: (userId, body) => post(`/users/${userId}/billing/checkout`, body),
  portal: (userId) => post(`/users/${userId}/billing/portal`),
}

export const subscriptionApi = {
  get: (userId) => request(`/users/${userId}/subscription`),
  change: (userId, tier) => post(`/users/${userId}/subscription/upgrade`, { tier }),
  downgradeToFree: (userId) => post(`/users/${userId}/subscription/downgrade-free`),
}

export const earnApi = {
  opportunities: (userId) => request(`/users/${userId}/earn`),
  /** Rewarded ads: get a one-time ticket, then redeem it once the ad was watched. */
  startAd: (userId, provider) => post(`/users/${userId}/earn/ads/start`, { provider }),
  completeAd: (userId, ticket) => post(`/users/${userId}/earn/ads/complete`, { ticket }),
  recordLogin: (userId) => post(`/users/${userId}/earn/login`),
  referral: (userId) => request(`/users/${userId}/earn/referral`),
  applyReferral: (userId, referralCode) => post(`/users/${userId}/earn/referral/apply`, { referralCode }),
}

export const campaignApi = {
  list: () => request('/campaigns'),
  get: (id) => request(`/campaigns/${id}`),
  create: (campaign) => post('/campaigns', campaign),
  update: (id, campaign) => put(`/campaigns/${id}`, campaign),
  remove: (id) => request(`/campaigns/${id}`, { method: 'DELETE' }),
  memory: (id) => request(`/campaigns/${id}/memory`),
}

/**
 * Characters belong to a campaign, so creating and listing are campaign-scoped.
 */
export const characterApi = {
  listForCampaign: (campaignId) => request(`/campaigns/${campaignId}/characters`),
  create: (campaignId, character) => post(`/campaigns/${campaignId}/characters`, character),
  remove: (id) => request(`/characters/${id}`, { method: 'DELETE' }),
}

/** Anything in Campaign Memory addressable as /{resource}/{id}: npcs, locations, quests, items, … */
export const memoryApi = {
  create: (campaignId, resource, body) => post(`/campaigns/${campaignId}/${resource}`, body),
  update: (resource, id, body) => put(`/${resource}/${id}`, body),
  remove: (resource, id) => request(`/${resource}/${id}`, { method: 'DELETE' }),
}

export const sessionApi = {
  /** Record what happened in a session (the post-session questions). */
  saveDebrief: (sessionId, debrief) => put(`/sessions/${sessionId}/debrief`, debrief),
  /** Generate the next session of a campaign (25 tokens). */
  next: (campaignId, wishes = '') => post(`/campaigns/${campaignId}/sessions/next`, { wishes }),
  /** Re-plan a session that has not been played yet (15 tokens). */
  rewrite: (sessionId, wishes = '') => post(`/sessions/${sessionId}/rewrite`, { wishes }),
  setCompleted: (sessionId, completed) => put(`/sessions/${sessionId}/completed`, { completed }),
  /** Deletes a session; the sessions after it move up one number. */
  remove: (sessionId) => request(`/sessions/${sessionId}`, { method: 'DELETE' }),
}

export const generatorApi = {
  /** Any in-campaign generator module (NPC, Quest, Loot, ...). */
  run: (campaignId, type, body) => post(`/campaigns/${campaignId}/generate/${type}`, body),
  /** Save a previewed result into Campaign Memory. */
  commit: (campaignId, type, logId) => post(`/campaigns/${campaignId}/generate/${type}/commit/${logId}`),
  /** Free-form question answered with full campaign context. */
  ask: (campaignId, body) => post(`/campaigns/${campaignId}/ai/generate`, { type: 'FREEFORM', ...body }),
}

export const campaignGeneratorApi = {
  /** Generates an idea for the "Your idea" box (5 tokens) and saves it to the account. */
  idea: (body) => post('/ai/campaign-generator/idea', body),
  savedIdeas: (kind) => request(`/ai/campaign-generator/ideas${kind ? `?kind=${kind}` : ''}`),
  deleteIdea: (id) => request(`/ai/campaign-generator/ideas/${id}`, { method: 'DELETE' }),
  interview: (body) => post('/ai/campaign-generator/interview', body),
  generate: (body) => post('/ai/campaign-generator/generate', body),
  oneShot: (body) => post('/ai/campaign-generator/one-shot', body),
  /** Start a campaign with all its sessions (Pro+); runs in the background, poll with job(). */
  full: (body) => post('/ai/campaign-generator/full', body),
  job: (id) => request(`/ai/campaign-generator/jobs/${id}`),
}

export const aiApi = {
  status: () => request('/ai/status'),
}

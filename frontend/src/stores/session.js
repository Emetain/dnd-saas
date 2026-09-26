import { reactive, computed } from 'vue'
import { ApiError, authApi, setUnauthorizedHandler, tokenApi, userApi } from '../services/api'

/**
 * Who is signed in, plus their live token balance.
 *
 * Signing in sets an HttpOnly session cookie (JavaScript can't read it), so on
 * page load the store simply asks the backend who the cookie belongs to.
 */
const state = reactive({
  user: null,
  balance: null,
  loading: true,
  /** Set when a session ran out mid-use, so the sign-in page can say why. */
  expired: false,
})

async function setUser(user) {
  state.user = user
  state.expired = false
  state.balance = await tokenApi.balance(user.id)
  return user
}

async function login(email, password) {
  return setUser(await authApi.login(email, password))
}

async function register(form) {
  return setUser(await authApi.register(form))
}

function clear() {
  state.user = null
  state.balance = null
}

async function signOut() {
  try {
    await authApi.logout()
  } finally {
    clear()
  }
}

/** Re-reads the user and balance, e.g. after a generation or purchase. */
async function refreshBalance() {
  if (!state.user) return
  const [user, balance] = await Promise.all([userApi.get(state.user.id), tokenApi.balance(state.user.id)])
  state.user = user
  state.balance = balance
}

async function restore() {
  try {
    await setUser(await authApi.me())
  } catch (e) {
    if (!(e instanceof ApiError && e.status === 401)) console.error('Could not restore the session', e)
    clear()
  }
  state.loading = false
}

// Any 401 later on means the session ended (expired, or signed out elsewhere).
setUnauthorizedHandler(() => {
  if (!state.user) return
  clear()
  state.expired = true
})

const isFree = computed(() => state.user?.subscriptionTier === 'FREE')

export function useSession() {
  return { state, isFree, login, register, signOut, refreshBalance, restore }
}

export const tierLabels = {
  FREE: 'Free',
  PRO: 'Pro',
  PRO_PLUS: 'Pro+',
  ULTIMATE_DM: 'Ultimate DM',
}

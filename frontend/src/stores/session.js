import { reactive, computed } from 'vue'
import { ApiError, setApiUser, tokenApi, userApi } from '../services/api'

/**
 * Who is using the app, plus their live token balance.
 *
 * There is no login yet: the chosen user id is remembered in this browser and
 * sent as the X-User-Id header. When authentication arrives, only this store
 * needs to change.
 */
const STORAGE_KEY = 'narrion.userId'
/** Key used before the rename to Narrion — read once so nobody is signed out. */
const LEGACY_KEY = 'dndsaas.userId'

const state = reactive({
  user: null,
  balance: null,
  loading: true,
})

function readStoredId() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY) ?? localStorage.getItem(LEGACY_KEY)
    localStorage.removeItem(LEGACY_KEY)
    return raw ? Number(raw) : null
  } catch {
    return null
  }
}

function storeId(id) {
  try {
    if (id == null) localStorage.removeItem(STORAGE_KEY)
    else localStorage.setItem(STORAGE_KEY, String(id))
  } catch {
    // Storage unavailable (private mode); the session just won't survive a reload.
  }
}

async function signIn(id) {
  const user = await userApi.get(id)
  state.user = user
  setApiUser(user.id)
  storeId(user.id)
  await refreshBalance()
  return user
}

async function register(form) {
  const user = await userApi.register(form)
  return signIn(user.id)
}

function signOut() {
  state.user = null
  state.balance = null
  setApiUser(null)
  storeId(null)
}

/** Re-reads the user and balance, e.g. after a generation or purchase. */
async function refreshBalance() {
  if (!state.user) return
  const [user, balance] = await Promise.all([userApi.get(state.user.id), tokenApi.balance(state.user.id)])
  state.user = user
  state.balance = balance
}

async function restore() {
  const id = readStoredId()
  if (id != null) {
    try {
      await signIn(id)
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) signOut()
    }
  }
  state.loading = false
}

const isFree = computed(() => state.user?.subscriptionTier === 'FREE')

export function useSession() {
  return { state, isFree, signIn, register, signOut, refreshBalance, restore }
}

export const tierLabels = {
  FREE: 'Free',
  PRO: 'Pro',
  PRO_PLUS: 'Pro+',
  ULTIMATE_DM: 'Ultimate DM',
}

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Check, LogOut } from 'lucide-vue-next'
import PasswordStrength from '../components/PasswordStrength.vue'
import { authApi } from '../services/api'
import { isStrongPassword } from '../utils/password'
import { tierLabels, useSession } from '../stores/session'

/** Account settings: display name, password and signing out. */
const { state, signOut } = useSession()
const router = useRouter()

const profile = reactive({ displayName: state.user.displayName })
const profileState = reactive({ busy: false, error: '', saved: false })

async function saveProfile() {
  Object.assign(profileState, { busy: true, error: '', saved: false })
  try {
    state.user = await authApi.updateProfile(profile.displayName)
    profileState.saved = true
  } catch (e) {
    profileState.error = e.message
  } finally {
    profileState.busy = false
  }
}

const password = reactive({ current: '', next: '', repeat: '' })
const passwordState = reactive({ busy: false, error: '', saved: false })
const showPasswords = ref(false)

async function changePassword() {
  Object.assign(passwordState, { error: '', saved: false })
  if (!isStrongPassword(password.next)) {
    passwordState.error = "Your new password doesn't meet all the rules yet"
    return
  }
  if (password.next !== password.repeat) {
    passwordState.error = "The new passwords don't match"
    return
  }
  passwordState.busy = true
  try {
    await authApi.changePassword(password.current, password.next)
    Object.assign(password, { current: '', next: '', repeat: '' })
    passwordState.saved = true
  } catch (e) {
    passwordState.error = e.message
  } finally {
    passwordState.busy = false
  }
}

async function logout() {
  await signOut()
  router.push('/')
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Account settings</h1>
        <p>Your profile, password and sign-in.</p>
      </div>
    </div>

    <div class="settings">
      <section class="card stack">
        <h2>Profile</h2>
        <form class="stack" @submit.prevent="saveProfile">
          <div class="field">
            <label for="a-name">Display name</label>
            <input id="a-name" v-model="profile.displayName" class="input" required maxlength="50" autocomplete="nickname" />
          </div>
          <div class="field">
            <label for="a-email">Email</label>
            <input id="a-email" :value="state.user.email" class="input" disabled />
            <span class="hint">Your email is how you sign in.</span>
          </div>
          <div class="field">
            <span class="label">Plan</span>
            <span>
              <span class="badge badge-blue">{{ tierLabels[state.user.subscriptionTier] }}</span>
              <RouterLink to="/billing#plans" class="small plan-link">Change plan</RouterLink>
            </span>
          </div>
          <div v-if="profileState.error" class="alert alert-error" role="alert">{{ profileState.error }}</div>
          <div class="row">
            <button class="btn btn-primary" :disabled="profileState.busy || profile.displayName.trim() === state.user.displayName">
              <span v-if="profileState.busy" class="spinner" /> Save
            </button>
            <span v-if="profileState.saved" class="saved"><Check :size="15" /> Saved</span>
          </div>
        </form>
      </section>

      <section class="card stack">
        <h2>Password</h2>
        <form class="stack" @submit.prevent="changePassword">
          <!-- Lets password managers link the change to this account. -->
          <input :value="state.user.email" type="email" autocomplete="username" hidden />
          <div class="field">
            <label for="a-current">Current password</label>
            <input id="a-current" v-model="password.current" :type="showPasswords ? 'text' : 'password'" class="input" required autocomplete="current-password" />
          </div>
          <div class="field">
            <label for="a-new">New password</label>
            <input id="a-new" v-model="password.next" :type="showPasswords ? 'text' : 'password'" class="input" required autocomplete="new-password" aria-describedby="a-strength" />
            <PasswordStrength id="a-strength" :password="password.next" />
            <span class="hint">Changing it signs you out on your other devices.</span>
          </div>
          <div class="field">
            <label for="a-repeat">Repeat new password</label>
            <input id="a-repeat" v-model="password.repeat" :type="showPasswords ? 'text' : 'password'" class="input" required autocomplete="new-password" />
          </div>
          <label class="check"><input v-model="showPasswords" type="checkbox" /> Show passwords</label>
          <div v-if="passwordState.error" class="alert alert-error" role="alert">{{ passwordState.error }}</div>
          <div class="row">
            <button class="btn btn-primary" :disabled="passwordState.busy || !password.current || !isStrongPassword(password.next) || password.next !== password.repeat">
              <span v-if="passwordState.busy" class="spinner" /> Change password
            </button>
            <span v-if="passwordState.saved" class="saved"><Check :size="15" /> Password changed</span>
          </div>
        </form>
      </section>

      <section class="card stack">
        <h2>Sign out</h2>
        <p class="muted">Sign out of Narrion in this browser.</p>
        <div><button class="btn btn-secondary" @click="logout"><LogOut :size="16" /> Sign out</button></div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.settings { display: grid; gap: 20px; max-width: 640px; }
.settings h2 { margin: 0; }
.label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.plan-link { margin-left: 10px; }
.saved { display: inline-flex; align-items: center; gap: 6px; color: var(--success-text); font-weight: 560; }
.check { display: flex; gap: 8px; align-items: center; cursor: pointer; font-size: 14px; }
</style>

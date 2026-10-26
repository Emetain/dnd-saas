<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthCard from '../components/AuthCard.vue'
import PasswordStrength from '../components/PasswordStrength.vue'
import { authApi } from '../services/api'
import { useSession } from '../stores/session'
import { isStrongPassword } from '../utils/password'

/** Chooses a new password from the link in the reset email (?token=…). */
const route = useRoute()
const router = useRouter()
const { state } = useSession()

const token = computed(() => (typeof route.query.token === 'string' ? route.query.token : ''))
const password = ref('')
const repeat = ref('')
const show = ref(false)
const busy = ref(false)
const error = ref('')

const canSubmit = computed(() => token.value && isStrongPassword(password.value) && password.value === repeat.value)

async function submit() {
  if (!canSubmit.value) return
  busy.value = true
  error.value = ''
  try {
    await authApi.resetPassword(token.value, password.value)
    // Resetting signs the account out everywhere, this browser included.
    state.user = null
    router.replace({ name: 'login', query: { reset: '1' } })
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <AuthCard title="Choose a new password" subtitle="You'll be signed out on all your devices afterwards.">
    <div v-if="!token" class="stack">
      <div class="alert alert-error">This link is incomplete. Open the link from the email again, or ask for a new one.</div>
      <RouterLink to="/forgot-password" class="btn btn-secondary btn-block">Send a new link</RouterLink>
    </div>
    <form v-else class="stack" novalidate @submit.prevent="submit">
      <!-- Lets password managers store the new password. -->
      <input type="text" autocomplete="username" hidden />
      <div class="field">
        <label for="r-password">New password</label>
        <input
          id="r-password" v-model="password" class="input" :type="show ? 'text' : 'password'" required
          autocomplete="new-password" aria-describedby="r-strength"
        />
        <PasswordStrength id="r-strength" :password="password" />
      </div>
      <div class="field">
        <label for="r-repeat">Repeat new password</label>
        <input id="r-repeat" v-model="repeat" class="input" :type="show ? 'text' : 'password'" required autocomplete="new-password" />
        <span v-if="repeat && repeat !== password" class="hint warn">The passwords don't match yet.</span>
      </div>
      <label class="check"><input v-model="show" type="checkbox" /> Show passwords</label>
      <div v-if="error" class="alert alert-error" role="alert">
        {{ error }} <RouterLink v-if="error.includes('link')" to="/forgot-password">Send a new link</RouterLink>
      </div>
      <button class="btn btn-primary btn-block" :disabled="busy || !canSubmit">
        <span v-if="busy" class="spinner" /> Save new password
      </button>
    </form>
  </AuthCard>
</template>

<style scoped>
.check { display: flex; gap: 8px; align-items: center; cursor: pointer; font-size: 14px; }
.warn { color: var(--accent-text); }
</style>

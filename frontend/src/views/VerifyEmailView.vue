<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { CircleCheck, CircleX } from 'lucide-vue-next'
import AuthCard from '../components/AuthCard.vue'
import { authApi } from '../services/api'
import { useSession } from '../stores/session'

/** Opened from the link in the verification email (?token=…). */
const route = useRoute()
const { state, refreshBalance } = useSession()

const status = ref('checking') // checking | done | failed
const error = ref('')

onMounted(async () => {
  const token = typeof route.query.token === 'string' ? route.query.token : ''
  if (!token) {
    status.value = 'failed'
    error.value = 'This link is incomplete. Open the link from the email again.'
    return
  }
  try {
    await authApi.verifyEmail(token)
    status.value = 'done'
    await refreshBalance()
  } catch (e) {
    status.value = 'failed'
    error.value = e.message
  }
})
</script>

<template>
  <AuthCard :title="status === 'done' ? 'Email confirmed' : status === 'failed' ? 'That didn\'t work' : 'Confirming your email…'">
    <div v-if="status === 'checking'" class="center"><span class="spinner" /></div>
    <div v-else-if="status === 'done'" class="stack center">
      <span class="icon ok"><CircleCheck :size="28" /></span>
      <p>Thanks! Your email address is confirmed and every generator is ready for you.</p>
      <RouterLink :to="state.user ? '/generators' : '/login'" class="btn btn-primary btn-block">
        {{ state.user ? 'Start generating' : 'Sign in' }}
      </RouterLink>
    </div>
    <div v-else class="stack center">
      <span class="icon bad"><CircleX :size="28" /></span>
      <p>{{ error }}</p>
      <p class="muted small">You can ask for a new link on your dashboard after signing in.</p>
      <RouterLink :to="state.user ? '/' : '/login'" class="btn btn-secondary btn-block">
        {{ state.user ? 'Go to dashboard' : 'Sign in' }}
      </RouterLink>
    </div>
  </AuthCard>
</template>

<style scoped>
.center { text-align: center; align-items: center; display: flex; flex-direction: column; }
.center p { margin: 0; }
.icon { width: 56px; height: 56px; border-radius: 50%; display: grid; place-items: center; }
.icon.ok { background: var(--success-soft); color: var(--success-text); }
.icon.bad { background: var(--accent-soft); color: var(--accent-text); }
</style>

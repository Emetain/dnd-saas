<script setup>
import { computed, ref } from 'vue'
import { MailCheck } from 'lucide-vue-next'
import AuthCard from '../components/AuthCard.vue'
import { authApi } from '../services/api'

/** Asks for a password reset email. Always says "sent", so it can't reveal who has an account. */
const email = ref('')
const sent = ref(false)
const busy = ref(false)
const error = ref('')
const emailValid = computed(() => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.value.trim()))

async function submit() {
  if (!emailValid.value) return
  busy.value = true
  error.value = ''
  try {
    await authApi.forgotPassword(email.value.trim())
    sent.value = true
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <AuthCard title="Forgot your password?" :subtitle="sent ? '' : 'Enter your email and we\'ll send you a link to choose a new one.'">
    <div v-if="sent" class="stack sent">
      <span class="icon"><MailCheck :size="26" /></span>
      <p>If <strong>{{ email }}</strong> has a Narrion account, a reset link is on its way. It works for 1 hour.</p>
      <p class="muted small">No email? Check your spam folder, or try again in a minute.</p>
      <RouterLink to="/login" class="btn btn-secondary btn-block">Back to sign in</RouterLink>
    </div>
    <form v-else class="stack" novalidate @submit.prevent="submit">
      <div class="field">
        <label for="f-email">Email</label>
        <input id="f-email" v-model="email" class="input" type="email" required autocomplete="email" placeholder="you@example.com" />
      </div>
      <div v-if="error" class="alert alert-error" role="alert">{{ error }}</div>
      <button class="btn btn-primary btn-block" :disabled="busy || !emailValid">
        <span v-if="busy" class="spinner" /> Send reset link
      </button>
      <p class="muted small back"><RouterLink to="/login">Back to sign in</RouterLink></p>
    </form>
  </AuthCard>
</template>

<style scoped>
.sent { text-align: center; align-items: center; }
.sent p { margin: 0; }
.icon {
  width: 52px; height: 52px; border-radius: 50%; display: grid; place-items: center;
  background: var(--success-soft); color: var(--success-text);
}
.back { text-align: center; margin: 0; }
</style>

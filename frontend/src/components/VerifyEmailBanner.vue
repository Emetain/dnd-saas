<script setup>
import { ref } from 'vue'
import { MailWarning } from 'lucide-vue-next'
import { authApi } from '../services/api'
import { useSession } from '../stores/session'

/** Shown until the signed-in user confirms their email; generating is blocked until then. */
const { state, refreshBalance } = useSession()
const status = ref('') // '' | sending | sent
const error = ref('')

async function resend() {
  status.value = 'sending'
  error.value = ''
  try {
    await authApi.resendVerification()
    status.value = 'sent'
  } catch (e) {
    status.value = ''
    error.value = e.message
  }
}
</script>

<template>
  <div class="verify-banner" role="status">
    <div class="inner">
      <MailWarning :size="18" />
      <span>
        <strong>Confirm your email to start generating.</strong>
        We sent a link to {{ state.user.email }}.
        <template v-if="error"> {{ error }}.</template>
      </span>
      <span class="actions">
        <span v-if="status === 'sent'" class="sent">New link sent</span>
        <button v-else class="btn btn-sm btn-secondary" :disabled="status === 'sending'" @click="resend">
          <span v-if="status === 'sending'" class="spinner" /> Resend email
        </button>
        <button class="btn btn-sm btn-ghost" title="Already clicked the link in another tab?" @click="refreshBalance">I've confirmed it</button>
      </span>
    </div>
  </div>
</template>

<style scoped>
.verify-banner { background: var(--primary-soft); border-bottom: 1px solid var(--primary-soft-strong); color: var(--primary-text); }
.inner { max-width: 1180px; margin: 0 auto; padding: 10px 24px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; font-size: 14px; }
.inner > svg { flex-shrink: 0; }
.actions { margin-left: auto; display: flex; gap: 6px; align-items: center; }
.sent { font-weight: 600; }
@media (max-width: 640px) { .inner { padding: 10px 16px; } .actions { margin-left: 0; } }
</style>

<script setup>
import { computed, watchEffect } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AuthCard from '../components/AuthCard.vue'
import AuthForm from '../components/AuthForm.vue'
import { useSession } from '../stores/session'

/** The /login and /register pages. Afterwards you land where you were heading. */
const route = useRoute()
const router = useRouter()
const { state } = useSession()

const mode = computed({
  get: () => (route.name === 'register' ? 'register' : 'login'),
  set: (value) => router.replace({ name: value, query: route.query }),
})

function destination() {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/'
}

// Already signed in (or just signed in): go on.
watchEffect(() => {
  if (state.user) router.replace(destination())
})
</script>

<template>
  <AuthCard
    :title="mode === 'register' ? 'Create your account' : 'Welcome back'"
    :subtitle="mode === 'register' ? 'Free to start — 50 tokens to try every generator.' : 'Sign in to continue to your campaigns.'"
  >
    <div v-if="state.expired && mode === 'login'" class="alert alert-info">Your session ended — please sign in again.</div>
    <div v-if="route.query.reset && mode === 'login'" class="alert alert-success">Your password is changed — sign in with your new password.</div>
    <AuthForm v-model:mode="mode" />
  </AuthCard>
</template>

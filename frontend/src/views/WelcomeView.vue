<script setup>
import { reactive, ref } from 'vue'
import { useSession } from '../stores/session'
import { useWelcome } from '../stores/welcome'

/**
 * Shown below the banner to visitors who are not signed in: what Narrion does,
 * and the account form ("Register now" and "Sign in" scroll to it).
 * Continuing as a user id is temporary until real sign-in exists.
 */
const { register, signIn } = useSession()
/** Shared with the banner and navbar, which switch it when they open the form. */
const { mode } = useWelcome()
const form = reactive({ displayName: '', email: '', password: '' })
const existingId = ref('')
const error = ref('')
const busy = ref(false)

async function submit() {
  error.value = ''
  busy.value = true
  try {
    if (mode.value === 'register') await register({ ...form })
    else await signIn(Number(existingId.value))
  } catch (e) {
    error.value = e.status === 404 ? 'No account with that id.' : e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="welcome">
    <section class="hero">
      <h2>Everything your campaign needs, in one place</h2>
      <p>
        Manage your world, sessions and characters — with an AI assistant that already
        knows every NPC, place and plot thread you have created.
      </p>
      <ul>
        <li>Generators that fit your existing world</li>
        <li>Structured campaign memory, not walls of text</li>
        <li>Free to start — 50 tokens on sign-up</li>
      </ul>
    </section>

    <section id="account-form" class="panel card">
      <div class="segmented">
        <button :class="{ active: mode === 'register' }" @click="mode = 'register'">Create account</button>
        <button :class="{ active: mode === 'existing' }" @click="mode = 'existing'">I have an account</button>
      </div>

      <form class="stack" @submit.prevent="submit">
        <template v-if="mode === 'register'">
          <div class="field">
            <label for="name">Display name</label>
            <input id="name" v-model="form.displayName" class="input" required placeholder="Dungeon Master" />
          </div>
          <div class="field">
            <label for="email">Email</label>
            <input id="email" v-model="form.email" class="input" type="email" required placeholder="you@example.com" />
          </div>
          <div class="field">
            <label for="password">Password</label>
            <input id="password" v-model="form.password" class="input" type="password" required minlength="6" />
          </div>
        </template>
        <div v-else class="field">
          <label for="uid">User id</label>
          <input id="uid" v-model="existingId" class="input" type="number" min="1" required placeholder="1" />
          <span class="hint">Temporary until login is added — your id is shown in your account menu.</span>
        </div>

        <div v-if="error" class="alert alert-error">{{ error }}</div>

        <button class="btn btn-primary btn-block" :disabled="busy">
          <span v-if="busy" class="spinner" />
          {{ mode === 'register' ? 'Create free account' : 'Continue' }}
        </button>
      </form>
    </section>
  </div>
</template>

<style scoped>
.welcome {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 48px;
  align-items: center;
  max-width: 1180px;
  margin: 0 auto;
  padding: 48px 24px;
}
.hero h2 { font-size: 30px; font-weight: 700; margin: 0 0 12px; letter-spacing: -0.02em; }
.hero p { color: var(--text-secondary); font-size: 16px; max-width: 480px; }
.hero ul { margin: 24px 0 0; padding: 0; list-style: none; display: grid; gap: 10px; }
.hero li { display: flex; gap: 10px; align-items: center; color: var(--text-strong); font-weight: 520; }
.hero li::before { content: ''; width: 8px; height: 8px; border-radius: 50%; background: var(--accent); }
.panel { scroll-margin-top: calc(var(--nav-height) + 24px); display: flex; flex-direction: column; gap: 20px; padding: 28px; box-shadow: var(--shadow); }
.panel .segmented { align-self: flex-start; }
@media (max-width: 820px) {
  .welcome { grid-template-columns: 1fr; gap: 24px; }
  .hero h2 { font-size: 24px; }
}
</style>

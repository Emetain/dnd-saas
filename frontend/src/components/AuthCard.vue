<script setup>
import NarrionLogo from './NarrionLogo.vue'

/** The centred card used by the sign-in, password and email-confirmation pages. */
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
})
</script>

<template>
  <div class="auth-page">
    <div class="card auth-card">
      <div class="head">
        <div class="logo"><NarrionLogo :size="44" :wordmark="false" /></div>
        <h1>{{ title }}</h1>
        <p v-if="subtitle" class="muted">{{ subtitle }}</p>
      </div>
      <slot />
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: calc(100vh - var(--nav-height));
  display: grid; place-items: center;
  padding: 40px 16px;
}
.auth-card { width: 100%; max-width: 440px; padding: 32px; display: flex; flex-direction: column; gap: 20px; box-shadow: var(--shadow); }
.head { text-align: center; }
.logo { display: flex; justify-content: center; margin-bottom: 12px; }
.head h1 { font-size: 24px; margin: 0 0 6px; letter-spacing: -0.01em; }
.head p { margin: 0; }
@media (max-width: 480px) {
  .auth-card { padding: 24px 18px; }
}
</style>

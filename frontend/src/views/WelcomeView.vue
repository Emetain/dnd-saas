<script setup>
import { UserPlus } from 'lucide-vue-next'

/**
 * Shown below the banner to visitors who are not signed in: what Narrion does,
 * with links to the /register and /login pages (the forms live only there).
 */
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

    <section class="panel card">
      <h2>Start your first campaign</h2>
      <p class="muted">Create a free account and get 50 tokens to try every generator.</p>
      <RouterLink to="/register" class="btn btn-primary btn-block"><UserPlus :size="16" /> Create free account</RouterLink>
      <p class="muted small signin">Already have an account? <RouterLink to="/login">Sign in</RouterLink></p>
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
.panel { display: flex; flex-direction: column; gap: 14px; padding: 28px; box-shadow: var(--shadow); }
.panel h2 { margin: 0; font-size: 20px; }
.panel p { margin: 0; }
.signin { text-align: center; }
@media (max-width: 820px) {
  .welcome { grid-template-columns: 1fr; gap: 24px; }
  .hero h2 { font-size: 24px; }
}
</style>

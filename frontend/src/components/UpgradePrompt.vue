<script setup>
import { Castle, Flag, MessageCircleQuestion, Sparkles } from 'lucide-vue-next'

defineProps({ feature: { type: String, required: true } })
</script>

<template>
  <div class="card upgrade">
    <span class="badge badge-red">Pro feature</span>
    <h2>{{ feature }} is part of Pro</h2>
    <p class="muted">
      On Free you can generate one-shots and run every adventure generator inside them.
      Pro unlocks full campaigns and the tools that make a large world hang together.
    </p>
    <ul>
      <li><Castle :size="16" /> The campaign generator — lore, locations, factions, NPCs, quests and Session 1</li>
      <li><Flag :size="16" /> Factions to connect your world</li>
      <li><MessageCircleQuestion :size="16" /> Free-form questions about your campaign</li>
      <li><Sparkles :size="16" /> 1,000 tokens every month, rolling over up to 2,000</li>
    </ul>
    <div class="row">
      <RouterLink to="/billing#plans" class="btn btn-accent">See plans</RouterLink>
      <RouterLink to="/generators/one-shot" class="btn btn-secondary">Make a one-shot instead</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.upgrade { display: flex; flex-direction: column; gap: 12px; align-items: flex-start; border-top: 3px solid var(--accent); }
.upgrade h2 { margin: 0; }
ul { list-style: none; padding: 0; margin: 4px 0 8px; display: grid; gap: 8px; }
li { display: flex; gap: 10px; align-items: center; color: var(--text-strong); }
li svg { color: var(--primary); flex-shrink: 0; }
</style>

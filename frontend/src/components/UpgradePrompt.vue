<script setup>
import { computed } from 'vue'
import { Check } from 'lucide-vue-next'
import { findPlan } from '../data/plans'
import { tierLabels } from '../stores/session'

/** Shown in place of a generator the user's plan doesn't include (e.g. when opened by URL). */
const props = defineProps({
  feature: { type: String, required: true },
  /** The lowest plan that includes the feature. */
  tier: { type: String, default: 'PRO' },
})

const plan = computed(() => findPlan(props.tier))
const planName = computed(() => tierLabels[props.tier])
</script>

<template>
  <div class="card upgrade">
    <span class="badge badge-red">{{ planName }} feature</span>
    <h2>{{ feature }} is part of {{ planName }}</h2>
    <p class="muted">Upgrade to {{ planName }} ({{ plan.price }} / month) to use it. You also get:</p>
    <ul>
      <li v-for="item in plan.features" :key="item"><Check :size="16" /> {{ item }}</li>
    </ul>
    <div class="row">
      <RouterLink to="/billing#plans" class="btn btn-accent">Upgrade to {{ planName }}</RouterLink>
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

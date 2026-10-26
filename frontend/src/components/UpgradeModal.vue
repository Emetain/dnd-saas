<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Check, Lock } from 'lucide-vue-next'
import AppModal from './AppModal.vue'
import { useAccess } from '../stores/access'
import { tierLabels, useSession } from '../stores/session'
import { findPlan } from '../data/plans'

/** The "this is part of a higher plan" pop-up; opened from anywhere through useAccess(). */
const { upgrade, closeUpgrade } = useAccess()
const { state } = useSession()
const router = useRouter()

const plan = computed(() => findPlan(upgrade.tier))
const planName = computed(() => tierLabels[upgrade.tier])
const currentName = computed(() => tierLabels[state.user?.subscriptionTier ?? 'FREE'])

function seePlans() {
  closeUpgrade()
  router.push('/billing#plans')
}
</script>

<template>
  <AppModal v-if="upgrade.open && plan" :title="`Unlock ${upgrade.feature}`" @close="closeUpgrade">
    <div class="stack upgrade">
      <div class="row intro">
        <span class="lock-icon"><Lock :size="20" /></span>
        <p>
          <strong>{{ upgrade.feature }}</strong> isn't included in your <strong>{{ currentName }}</strong> plan.
          Upgrade to <strong>{{ planName }}</strong> to use it.
        </p>
      </div>

      <div class="card plan">
        <div class="row-between">
          <h3>{{ planName }}</h3>
          <span class="price">{{ plan.price }}<span class="muted small"> / month</span></span>
        </div>
        <p class="muted small">{{ plan.tokens }} tokens</p>
        <ul>
          <li v-for="feature in plan.features" :key="feature"><Check :size="15" /> {{ feature }}</li>
        </ul>
      </div>

      <div class="row actions">
        <button class="btn btn-ghost" @click="closeUpgrade">Not now</button>
        <button class="btn btn-accent" @click="seePlans">Upgrade to {{ planName }}</button>
      </div>
    </div>
  </AppModal>
</template>

<style scoped>
.intro { align-items: flex-start; gap: 14px; }
.intro p { margin: 0; line-height: 1.55; }
.lock-icon {
  width: 40px; height: 40px; border-radius: 50%; display: grid; place-items: center; flex-shrink: 0;
  background: var(--accent-soft); color: var(--accent-text);
}
.plan { border-top: 3px solid var(--accent); }
.plan h3 { margin: 0; }
.price { font-size: 20px; font-weight: 700; }
ul { list-style: none; padding: 0; margin: 10px 0 0; display: grid; gap: 7px; }
li { display: flex; gap: 8px; align-items: flex-start; }
li svg { color: var(--primary); flex-shrink: 0; margin-top: 3px; }
.actions { justify-content: flex-end; flex-wrap: wrap; }
</style>

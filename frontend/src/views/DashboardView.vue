<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, Castle, Coins, Plus, ScrollText } from 'lucide-vue-next'
import { campaignApi } from '../services/api'
import { allGenerators } from '../data/generators'
import { tierLabels, useSession } from '../stores/session'
import CampaignCard from '../components/CampaignCard.vue'

const { state, isFree } = useSession()
const campaigns = ref([])
const loading = ref(true)

const quickGenerators = computed(() =>
  ['npc', 'encounter', 'loot', 'quest'].map((slug) => allGenerators.find((g) => g.slug === slug)),
)
const firstName = computed(() => state.user.displayName.split(' ')[0])

onMounted(async () => {
  try {
    campaigns.value = await campaignApi.list()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Welcome back, {{ firstName }}</h1>
        <p>Pick up where you left off, or start something new.</p>
      </div>
      <div class="row">
        <RouterLink to="/generators/one-shot" class="btn btn-secondary"><ScrollText :size="16" /> New one-shot</RouterLink>
        <RouterLink to="/generators/campaign" class="btn btn-primary"><Castle :size="16" /> New campaign</RouterLink>
      </div>
    </div>

    <div class="grid grid-3 stats">
      <div class="card">
        <div class="stat-label">Tokens</div>
        <div class="stat-value">{{ state.balance?.currentTokens ?? '—' }}</div>
        <p class="muted small" v-if="state.balance">
          {{ state.balance.allowanceTokens }} allowance · {{ state.balance.purchasedTokens }} purchased
        </p>
      </div>
      <div class="card">
        <div class="stat-label">Plan</div>
        <div class="stat-value">{{ tierLabels[state.user.subscriptionTier] }}</div>
        <RouterLink v-if="isFree" to="/billing#plans" class="small">Upgrade for full campaigns →</RouterLink>
        <p v-else class="muted small">{{ state.balance?.monthlyAllowance }} tokens per month</p>
      </div>
      <div class="card">
        <div class="stat-label">Campaigns</div>
        <div class="stat-value">{{ campaigns.length }}</div>
        <p class="muted small">{{ campaigns.filter((c) => c.kind === 'ONE_SHOT').length }} one-shots</p>
      </div>
    </div>

    <div v-if="isFree && state.balance && state.balance.currentTokens < 10" class="alert alert-info low">
      <Coins :size="16" />
      <span>
        Running low on tokens.
        <RouterLink to="/earn">Earn some free</RouterLink>, <RouterLink to="/billing#packs">buy a pack</RouterLink>
        or <RouterLink to="/billing#plans">upgrade to Pro</RouterLink> for 1,000 a month.
      </span>
    </div>

    <section class="section">
      <div class="row-between section-head">
        <h2>Your campaigns</h2>
        <RouterLink to="/campaigns" class="small">View all <ArrowRight :size="12" /></RouterLink>
      </div>
      <div v-if="loading" class="muted">Loading…</div>
      <div v-else-if="!campaigns.length" class="empty">
        <h3>No campaigns yet</h3>
        <p>Generate a one-shot to see how the AI builds a connected world.</p>
        <RouterLink to="/generators/one-shot" class="btn btn-primary" style="margin-top: 16px"><Plus :size="16" /> Create a one-shot</RouterLink>
      </div>
      <div v-else class="grid grid-3">
        <CampaignCard v-for="campaign in campaigns.slice(0, 6)" :key="campaign.id" :campaign="campaign" />
      </div>
    </section>

    <section class="section">
      <div class="row-between section-head">
        <h2>Quick generators</h2>
        <RouterLink to="/generators" class="small">All generators <ArrowRight :size="12" /></RouterLink>
      </div>
      <div class="grid grid-4">
        <RouterLink v-for="gen in quickGenerators" :key="gen.slug" :to="`/generators/${gen.slug}`" class="card card-link quick">
          <span class="icon"><component :is="gen.icon" :size="18" /></span>
          <h3>{{ gen.label }}</h3>
          <p class="muted small">{{ gen.description }}</p>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.stats { margin-bottom: 20px; }
.low { margin-bottom: 20px; }
.section { margin-top: 32px; }
.section-head { margin-bottom: 12px; }
.section-head a { display: inline-flex; align-items: center; gap: 4px; }
.quick .icon {
  width: 34px; height: 34px; border-radius: 8px; display: grid; place-items: center;
  background: var(--primary-soft); color: var(--primary-text); margin-bottom: 12px;
}
</style>

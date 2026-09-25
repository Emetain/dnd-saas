<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Check, Coins, Info } from 'lucide-vue-next'
import { paymentApi, subscriptionApi, tokenApi } from '../services/api'
import { tierLabels, useSession } from '../stores/session'

const { state, refreshBalance } = useSession()
const route = useRoute()
const router = useRouter()

/** With payments on, money goes through Stripe; without, plan changes are free for testing. */
const payments = ref({ paymentsEnabled: false, testMode: false })

const subscription = ref(null)
const history = ref([])
const busy = ref('')
const message = ref('')
const error = ref('')

const tierOrder = ['FREE', 'PRO', 'PRO_PLUS', 'ULTIMATE_DM']
/** Highlighted on the plans page. */
const MOST_POPULAR = 'PRO_PLUS'
const plans = [
  {
    tier: 'FREE', price: '€0', tokens: '50 to start',
    features: ['One-shots', 'NPC, Boss, Backstory, Quest, Encounter, Loot, Shop and Puzzle generators', 'Earn tokens with ads and referrals', 'Buy token packs'],
  },
  {
    tier: 'PRO', price: '€10', tokens: '1,000 / month',
    features: ['Everything in Free', 'Full campaign generator', 'Factions and full Campaign Memory', 'Free-form questions', 'Unused tokens roll over up to 2,000'],
  },
  {
    tier: 'PRO_PLUS', price: '€25', tokens: '3,000 / month',
    features: ['Everything in Pro', 'Rolls over up to 6,000'],
  },
  {
    tier: 'ULTIMATE_DM', price: '€50', tokens: '5,000 / month',
    features: ['Everything in Pro+', 'Image and map generation (coming soon)', 'Rolls over up to 10,000'],
  },
]
/** Mirrors TokenPack on the backend. */
const packs = [
  { pack: 'SMALL', tokens: 250, price: '€5' },
  { pack: 'MEDIUM', tokens: 1000, price: '€15', badge: 'Popular' },
  { pack: 'LARGE', tokens: 2500, price: '€25', badge: 'Best value' },
]

const currentTier = computed(() => state.user.subscriptionTier)
const pendingTier = computed(() => subscription.value?.pendingTier)

function planAction(tier) {
  const diff = tierOrder.indexOf(tier) - tierOrder.indexOf(currentTier.value)
  if (diff === 0) return pendingTier.value ? 'Keep this plan' : null
  return diff > 0 ? 'Upgrade' : 'Downgrade'
}

function formatDate(iso) {
  return iso ? new Date(iso).toLocaleDateString(undefined, { day: 'numeric', month: 'long', year: 'numeric' }) : '—'
}
function formatDateTime(iso) {
  return new Date(iso).toLocaleString(undefined, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })
}
function humanType(type) {
  return type.replaceAll('_', ' ').toLowerCase().replace(/^./, (c) => c.toUpperCase())
}

async function load() {
  const [sub, hist] = await Promise.all([subscriptionApi.get(state.user.id), tokenApi.history(state.user.id, 30)])
  subscription.value = sub
  history.value = hist
}

async function act(key, fn, success) {
  busy.value = key
  error.value = ''
  message.value = ''
  try {
    await fn()
    await Promise.all([refreshBalance(), load()])
    message.value = success
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = ''
  }
}

/** Sends the browser to a Stripe page (Checkout or the Customer Portal). */
async function goToStripe(key, fn) {
  busy.value = key
  error.value = ''
  try {
    const { url } = await fn()
    window.location.href = url
  } catch (e) {
    error.value = e.message
    busy.value = ''
  }
}

/** Opens the Stripe Customer Portal: change or cancel the plan, card, invoices. */
function manageSubscription() {
  goToStripe('portal', () => paymentApi.portal(state.user.id))
}

function changePlan(tier) {
  if (payments.value.paymentsEnabled) {
    // A Stripe subscription is changed in the portal; a first plan is bought through checkout.
    if (subscription.value?.managedByStripe) manageSubscription()
    else goToStripe(tier, () => paymentApi.checkout(state.user.id, { tier }))
    return
  }
  const action = planAction(tier)
  const text = action === 'Downgrade'
    ? `Your plan changes to ${tierLabels[tier]} at the end of this billing cycle.`
    : `You're now on ${tierLabels[tier]}.`
  act(tier, () => subscriptionApi.change(state.user.id, tier), text)
}

function buy(pack) {
  if (payments.value.paymentsEnabled) {
    goToStripe(pack.pack, () => paymentApi.checkout(state.user.id, { pack: pack.pack }))
    return
  }
  act(pack.pack, () => tokenApi.purchase(state.user.id, pack.pack), `${pack.tokens} tokens added — they never expire.`)
}

/**
 * Back from Stripe Checkout. The tokens are granted by Stripe's webhook, which can
 * take a few seconds — so check a few times until the balance changes.
 */
async function handleCheckoutReturn() {
  const outcome = route.query.checkout
  if (!outcome) return
  router.replace({ query: {} })
  if (outcome === 'cancelled') {
    error.value = 'Payment cancelled — nothing was charged.'
    return
  }
  message.value = 'Payment received! Your tokens are being added…'
  const before = state.balance?.currentTokens
  for (let attempt = 0; attempt < 8; attempt += 1) {
    await new Promise((resolve) => setTimeout(resolve, 1500))
    await Promise.all([refreshBalance(), load()])
    if (state.balance?.currentTokens !== before) break
  }
  message.value = 'Payment received — thank you! Your tokens have been added.'
}

onMounted(async () => {
  payments.value = await paymentApi.config().catch(() => payments.value)
  await load()
  await handleCheckoutReturn()
})
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Tokens &amp; billing</h1>
        <p>Every generation costs tokens. Your plan refills them monthly; bought tokens never expire.</p>
      </div>
    </div>

    <div v-if="message" class="alert alert-success banner"><Check :size="16" /> {{ message }}</div>
    <div v-if="error" class="alert alert-error banner">{{ error }}</div>

    <div v-if="state.balance" class="grid grid-4">
      <div class="card">
        <div class="stat-label">Total</div>
        <div class="stat-value">{{ state.balance.currentTokens }}</div>
        <p class="muted small">{{ state.balance.tokensUsedThisMonth }} used this month</p>
      </div>
      <div class="card">
        <div class="stat-label">Monthly allowance</div>
        <div class="stat-value">{{ state.balance.allowanceTokens }}</div>
        <p class="muted small">
          {{ currentTier === 'FREE' ? 'Starting and earned tokens' : `Rolls over up to ${state.balance.allowanceCap}` }}
        </p>
      </div>
      <div class="card">
        <div class="stat-label">Purchased</div>
        <div class="stat-value">{{ state.balance.purchasedTokens }}</div>
        <p class="muted small">Never expire · spent last</p>
      </div>
      <div class="card">
        <div class="stat-label">Next refill</div>
        <div class="stat-value date">{{ subscription?.nextBillingDate ? formatDate(subscription.nextBillingDate) : '—' }}</div>
        <p class="muted small">{{ currentTier === 'FREE' ? 'Free has no monthly refill' : `+${state.balance.monthlyAllowance} tokens` }}</p>
      </div>
    </div>

    <div v-if="pendingTier" class="alert alert-info banner">
      <Info :size="16" />
      <span>
        Your plan changes to <strong>{{ tierLabels[pendingTier] }}</strong> on {{ formatDate(subscription.billingCycleEnd) }}.
        Until then you keep everything in {{ tierLabels[currentTier] }}.
      </span>
    </div>

    <section id="plans" class="section">
      <h2>Plans</h2>
      <div class="row-between section-sub">
        <p class="muted">
          Monthly prices, cancel any time.
          <template v-if="!payments.paymentsEnabled">Payments aren't connected yet — changing plan is free while we test.</template>
          <template v-else-if="payments.testMode">Stripe test mode: use card 4242 4242 4242 4242.</template>
        </p>
        <button v-if="payments.paymentsEnabled && subscription?.managedByStripe" class="btn btn-secondary btn-sm" :disabled="busy !== ''" @click="manageSubscription">
          <span v-if="busy === 'portal'" class="spinner" /> Manage subscription
        </button>
      </div>
      <div class="grid grid-4">
        <div v-for="plan in plans" :key="plan.tier" class="card plan" :class="{ current: plan.tier === currentTier, featured: plan.tier === MOST_POPULAR }">
          <div class="row-between">
            <h3>{{ tierLabels[plan.tier] }}</h3>
            <span v-if="plan.tier === currentTier" class="badge badge-navy">Current</span>
            <span v-else-if="plan.tier === MOST_POPULAR" class="badge badge-red">Most popular</span>
          </div>
          <div class="price">{{ plan.price }}<span v-if="plan.tier !== 'FREE'" class="per">/ month</span></div>
          <div class="tokens"><Coins :size="14" /> {{ plan.tokens }}</div>
          <ul>
            <li v-for="f in plan.features" :key="f"><Check :size="14" /> {{ f }}</li>
          </ul>
          <button
            v-if="planAction(plan.tier)"
            class="btn btn-block"
            :class="planAction(plan.tier) === 'Upgrade' ? 'btn-accent' : 'btn-secondary'"
            :disabled="busy !== '' || pendingTier === plan.tier"
            @click="changePlan(plan.tier)"
          >
            {{ pendingTier === plan.tier ? 'Scheduled' : planAction(plan.tier) }}
          </button>
        </div>
      </div>
    </section>

    <section id="packs" class="section">
      <h2>Buy tokens</h2>
      <p class="muted section-sub">Top up any plan. Bought tokens never expire and are only used once your monthly allowance runs out.</p>
      <div class="grid grid-3">
        <div v-for="pack in packs" :key="pack.pack" class="card pack">
          <span v-if="pack.badge" class="badge badge-red pack-badge">{{ pack.badge }}</span>
          <div class="pack-tokens">{{ pack.tokens.toLocaleString() }}</div>
          <div class="muted">tokens</div>
          <div class="pack-price">{{ pack.price }}</div>
          <button class="btn btn-primary btn-block" :disabled="busy !== ''" @click="buy(pack)">
            <span v-if="busy === pack.pack" class="spinner" /> Buy for {{ pack.price }}
          </button>
        </div>
      </div>
      <p v-if="!payments.paymentsEnabled" class="muted small note">Payments are not connected yet — packs are added for free while testing.</p>
      <p v-else class="muted small note">Secure payment through Stripe — iDEAL, cards and more.</p>
    </section>

    <section class="section">
      <h2>History</h2>
      <div class="card table-card">
        <table v-if="history.length" class="table">
          <thead><tr><th>When</th><th>What</th><th>Type</th><th style="text-align: right">Tokens</th></tr></thead>
          <tbody>
            <tr v-for="t in history" :key="t.id">
              <td class="muted">{{ formatDateTime(t.createdAt) }}</td>
              <td>{{ t.description }}</td>
              <td><span class="badge">{{ humanType(t.type) }}</span></td>
              <td class="amount" :class="t.amount < 0 ? 'neg' : 'pos'">{{ t.amount > 0 ? '+' : '' }}{{ t.amount }}</td>
            </tr>
          </tbody>
        </table>
        <p v-else class="muted">No transactions yet.</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.banner { margin-bottom: 16px; }
.section { margin-top: 36px; scroll-margin-top: calc(var(--nav-height) + 16px); }
.section-sub { margin: 4px 0 16px; }
.date { font-size: 18px; margin-top: 10px; }
.plan { display: flex; flex-direction: column; gap: 10px; }
.plan h3 { margin: 0; }
.plan.current { border-color: var(--text-strong); box-shadow: 0 0 0 1px var(--text-strong); }
/* An inset line instead of a border, so the card's content stays level with the others. */
.plan.featured:not(.current) { box-shadow: inset 0 3px 0 var(--accent), var(--shadow-sm); }
.price { font-size: 26px; font-weight: 700; color: var(--text-strong); }
.per { font-size: 14px; font-weight: 500; color: var(--text-muted); margin-left: 4px; }
.tokens { display: inline-flex; align-items: center; gap: 6px; font-weight: 600; color: var(--primary-text); }
.plan ul { list-style: none; padding: 0; margin: 0 0 8px; display: grid; gap: 6px; flex: 1; align-content: start; }
.plan li { display: flex; gap: 8px; font-size: 13px; color: var(--text-secondary); }
.plan li svg { color: var(--success-text); flex-shrink: 0; margin-top: 3px; }
.pack { text-align: center; display: flex; flex-direction: column; gap: 4px; align-items: center; }
.pack { position: relative; }
.pack-badge { position: absolute; top: 12px; right: 12px; }
.pack-tokens { font-size: 30px; font-weight: 700; color: var(--text-strong); }
.pack-price { font-size: 20px; font-weight: 650; color: var(--text-strong); margin-top: 6px; }
.pack .btn { margin-top: 12px; }
.note { margin-top: 10px; }
.table-card { padding: 8px 12px; overflow-x: auto; }
.amount { text-align: right; font-weight: 620; font-variant-numeric: tabular-nums; }
.amount.pos { color: var(--success-text); }
.amount.neg { color: var(--accent-text); }
</style>

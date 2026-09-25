<script setup>
import { computed, onMounted, ref } from 'vue'
import { CalendarCheck, Copy, Gift, PlayCircle, Share2, Star, Users } from 'lucide-vue-next'
import { earnApi } from '../services/api'
import { adProvider, showGoogleRewardedAd } from '../services/ads'
import { useSession } from '../stores/session'
import SimulatedAd from '../components/SimulatedAd.vue'

const { state, isFree, refreshBalance } = useSession()

const opportunities = ref([])
const loading = ref(true)
const referralInput = ref('')
/** True while an ad is being fetched, played or rewarded. */
const adBusy = ref(false)
/** Seconds for the simulated ad while it is on screen; null when none is showing. */
const simulatedAdSeconds = ref(null)
let simulatedAdDone = null
const message = ref('')
const error = ref('')
const copied = ref(false)

const byType = computed(() => Object.fromEntries(opportunities.value.map((o) => [o.type, o])))
const ad = computed(() => byType.value.ad)
const referral = computed(() => byType.value.referral)
const streak = computed(() => byType.value.login_streak)

async function load() {
  try {
    const data = await earnApi.opportunities(state.user.id)
    opportunities.value = data.opportunities
  } finally {
    loading.value = false
  }
}

async function after(promise, success) {
  error.value = ''
  message.value = ''
  try {
    const res = await promise
    message.value = typeof success === 'function' ? success(res) : success
    await Promise.all([load(), refreshBalance()])
  } catch (e) {
    error.value = e.message
  }
}

/** Plays the development stand-in ad and resolves like a real one: 'granted' or 'dismissed'. */
function playSimulatedAd(seconds) {
  simulatedAdSeconds.value = seconds
  return new Promise((resolve) => { simulatedAdDone = resolve })
}
function endSimulatedAd(result) {
  simulatedAdSeconds.value = null
  simulatedAdDone?.(result)
}

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

/**
 * Watch a rewarded ad: get a ticket (which checks the limits), play the ad,
 * and redeem the ticket only if the ad was watched to the end.
 */
async function watchAd() {
  error.value = ''
  message.value = ''
  adBusy.value = true
  try {
    const ticket = await earnApi.startAd(state.user.id, adProvider)
    const startedAt = Date.now()
    const result = adProvider === 'google'
      ? await showGoogleRewardedAd()
      : await playSimulatedAd(ticket.minWatchSeconds)

    if (result !== 'granted') {
      error.value = {
        dismissed: 'The ad was closed early, so no tokens this time.',
        'no-fill': 'No ad available right now — try again in a little while.',
        unsupported: 'Rewarded ads are not supported on this device.',
      }[result]
      return
    }
    // The server only pays out once the ad could have finished; wait out any difference.
    const remaining = ticket.minWatchSeconds * 1000 - (Date.now() - startedAt)
    if (remaining > 0) await wait(remaining + 250)
    const reward = await earnApi.completeAd(state.user.id, ticket.ticket)
    message.value = `+${reward.tokensEarned} tokens. Thanks for watching!`
    await Promise.all([load(), refreshBalance()])
  } catch (e) {
    error.value = e.message
  } finally {
    adBusy.value = false
  }
}

function claimLogin() {
  after(earnApi.recordLogin(state.user.id), (r) =>
    r.tokensEarned > 0 ? `+${r.tokensEarned} tokens for your streak!` : 'Already claimed today — come back tomorrow.',
  )
}

function applyReferral() {
  after(earnApi.applyReferral(state.user.id, referralInput.value.trim()), 'Referral code applied.')
  referralInput.value = ''
}

async function copyCode() {
  try {
    await navigator.clipboard.writeText(referral.value.metadata.referral_code)
    copied.value = true
    setTimeout(() => { copied.value = false }, 1500)
  } catch {
    // Clipboard unavailable — the code is still visible to copy by hand.
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Earn tokens</h1>
        <p v-if="isFree">Keep generating on Free — earn tokens by watching ads, inviting friends and visiting daily.</p>
        <p v-else>Your plan refills monthly, but you can always earn a few extra.</p>
      </div>
      <RouterLink to="/billing#packs" class="btn btn-secondary">Or buy a pack</RouterLink>
    </div>

    <div v-if="message" class="alert alert-success banner"><Gift :size="16" /> {{ message }}</div>
    <div v-if="error" class="alert alert-error banner">{{ error }}</div>

    <div v-if="loading" class="muted">Loading…</div>
    <div v-else class="grid grid-2">
      <div v-if="ad" class="card earn">
        <div class="row"><span class="icon"><PlayCircle :size="18" /></span><h3>Watch a short ad</h3></div>
        <p class="muted">+{{ ad.tokensReward }} tokens per ad · up to {{ ad.metadata.max_per_day }} per day</p>
        <div class="meter">
          <div class="meter-fill" :style="{ width: `${100 - (ad.metadata.remaining_this_month / ad.metadata.monthly_cap) * 100}%` }" />
        </div>
        <p class="muted small">
          {{ ad.metadata.remaining_today }} left today ·
          {{ ad.metadata.monthly_cap - ad.metadata.remaining_this_month }} / {{ ad.metadata.monthly_cap }} earned this month from ads and streaks
        </p>
        <button v-if="!adProvider" class="btn btn-secondary" disabled>Ads coming soon</button>
        <button v-else class="btn btn-primary" :disabled="!ad.isAvailable || adBusy" @click="watchAd">
          <template v-if="adBusy"><span class="spinner" /> Playing ad…</template>
          <template v-else-if="!ad.isAvailable">{{ ad.reason }}</template>
          <template v-else>Watch ad</template>
        </button>
      </div>

      <div v-if="streak" class="card earn">
        <div class="row"><span class="icon"><CalendarCheck :size="18" /></span><h3>Daily streak</h3></div>
        <p class="muted">+1 token a day, and a bonus of 25 every 7 days in a row.</p>
        <div class="streak">
          <div><div class="stat-value">{{ streak.metadata.current_streak }}</div><span class="muted small">day streak</span></div>
          <div><div class="stat-value">{{ streak.metadata.longest_streak }}</div><span class="muted small">longest</span></div>
        </div>
        <button class="btn btn-primary" @click="claimLogin">Claim today's token</button>
      </div>

      <div v-if="referral" class="card earn">
        <div class="row"><span class="icon"><Users :size="18" /></span><h3>Invite friends</h3></div>
        <p class="muted">You get +{{ referral.tokensReward }} tokens and your friend gets +25 when they join with your code.</p>
        <div class="code-row">
          <code class="code">{{ referral.metadata.referral_code }}</code>
          <button class="btn btn-secondary btn-sm" @click="copyCode"><Copy :size="14" /> {{ copied ? 'Copied' : 'Copy' }}</button>
        </div>
        <p class="muted small">{{ referral.metadata.referrals_completed }} friends joined · {{ referral.metadata.total_earned }} tokens earned</p>
        <form class="row apply" @submit.prevent="applyReferral">
          <input v-model="referralInput" class="input" placeholder="Got a code from a friend?" required />
          <button class="btn btn-secondary">Apply</button>
        </form>
      </div>

      <div class="card earn soon">
        <div class="row"><span class="icon"><Star :size="18" /></span><h3>Review the app</h3></div>
        <p class="muted">+25 tokens per app store, once the mobile apps are live.</p>
        <div class="row"><span class="icon"><Share2 :size="18" /></span><h3>Share with your group</h3></div>
        <p class="muted">Coming soon.</p>
      </div>
    </div>
    <SimulatedAd
      v-if="simulatedAdSeconds"
      :seconds="simulatedAdSeconds"
      @granted="endSimulatedAd('granted')"
      @dismissed="endSimulatedAd('dismissed')"
    />
  </div>
</template>

<style scoped>
.banner { margin-bottom: 16px; }
.earn { display: flex; flex-direction: column; gap: 10px; align-items: flex-start; }
.earn h3 { margin: 0; }
.icon {
  width: 32px; height: 32px; border-radius: 8px; display: grid; place-items: center;
  background: var(--accent-soft); color: var(--accent-text);
}
.meter { width: 100%; height: 6px; background: var(--surface-muted); border-radius: 999px; overflow: hidden; }
.meter-fill { height: 100%; background: var(--primary); border-radius: 999px; }
.streak { display: flex; gap: 32px; }
.code-row { display: flex; gap: 8px; align-items: center; }
.code {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 15px; font-weight: 600;
  background: var(--primary-soft); color: var(--primary-text); padding: 6px 12px; border-radius: var(--radius-sm);
}
.apply { width: 100%; }
.soon { opacity: 0.8; }
</style>

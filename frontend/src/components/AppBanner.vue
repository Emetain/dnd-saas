<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowRight, Castle, Lock, PlayCircle, ScrollText, Sparkles } from 'lucide-vue-next'
import { campaignApi } from '../services/api'
import { useSession } from '../stores/session'
import { useWelcome } from '../stores/welcome'
import { bannerSlides } from '../data/bannerSlides'

/**
 * The banner below the navigation bar on the home page (and for visitors who
 * are not signed in): a slideshow filling 70% of the screen, with the tagline
 * and the main actions. It scrolls away while the navigation bar stays at the top.
 */
const { state, isFree } = useSession()
const { openAccountForm } = useWelcome()

// --- Slideshow ---
const current = ref(0)
const paused = ref(false)
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
let timer = null

function show(index) {
  current.value = (index + bannerSlides.length) % bannerSlides.length
}
function start() {
  stop()
  timer = setInterval(() => { if (!paused.value) show(current.value + 1) }, reducedMotion ? 9000 : 6500)
}
function stop() {
  if (timer) clearInterval(timer)
}
onMounted(start)
onBeforeUnmount(stop)

// --- "Continue" goes to the campaign the user worked on last ---
const lastCampaign = ref(null)
async function loadLastCampaign() {
  lastCampaign.value = null
  if (!state.user) return
  try {
    const campaigns = await campaignApi.list()
    lastCampaign.value = campaigns[0] ?? null
  } catch {
    // Not important enough to show an error — the button just stays hidden.
  }
}
// The banner is rebuilt each time the home page opens, so a new campaign shows up.
watch(() => state.user?.id, loadLastCampaign, { immediate: true })

const continueLink = computed(() => {
  const c = lastCampaign.value
  if (!c) return null
  return c.kind === 'CAMPAIGN' ? `/campaigns/${c.id}?tab=sessions` : `/campaigns/${c.id}`
})
</script>

<template>
  <section
    class="banner"
    aria-roledescription="carousel" aria-label="Narrion"
    @mouseenter="paused = true" @mouseleave="paused = false"
  >
    <div class="slides" aria-hidden="true">
      <div
        v-for="(slide, i) in bannerSlides" :key="slide.image"
        class="slide" :class="{ active: i === current, still: reducedMotion }"
        :style="{ backgroundImage: `url(${slide.image})` }"
      />
    </div>
    <div class="shade" />

    <div class="inner">
      <div class="copy">
        <h1 class="title">Your campaign, remembered.</h1>
        <p class="sub">
          Build your world, run your sessions and let an AI that already knows every NPC, place and plot thread help you prepare.
        </p>

        <div class="actions">
          <!-- Visitors get one clear action; members get the three ways in. -->
          <button v-if="!state.user" class="btn btn-accent btn-lg" @click="openAccountForm('register')">
            <Sparkles :size="18" /> Register now — it's free
          </button>
          <template v-else>
            <RouterLink to="/generators/campaign" class="btn btn-accent btn-lg">
              <Castle :size="18" /> Create a campaign
              <Lock v-if="isFree" :size="14" />
            </RouterLink>
            <RouterLink to="/generators/one-shot" class="btn btn-glass btn-lg">
              <ScrollText :size="18" /> Create a one-shot
            </RouterLink>
            <RouterLink v-if="continueLink" :to="continueLink" class="btn btn-glass btn-lg">
              <PlayCircle :size="18" /> <span class="truncate">Continue {{ lastCampaign.name }}</span> <ArrowRight :size="16" />
            </RouterLink>
          </template>
        </div>
      </div>
    </div>

    <div class="indicator">
      <span class="label">{{ bannerSlides[current].label }}</span>
      <div class="dots" role="tablist" aria-label="Choose a slide">
        <button
          v-for="(slide, i) in bannerSlides" :key="slide.label"
          role="tab" :aria-selected="i === current" :aria-label="slide.label"
          :class="{ on: i === current }" @click="show(i); start()"
        />
      </div>
    </div>
  </section>
</template>

<style scoped>
/* Always in brand colours, whichever theme is active. */
.banner {
  position: relative; overflow: hidden; color: #ffffff; background: #0b1630;
  height: 70vh; min-height: 460px; max-height: 860px;
}

.slides, .slide, .shade { position: absolute; inset: 0; }
.slide {
  background-size: cover; background-position: center;
  opacity: 0; transition: opacity 1.4s ease;
  transform: scale(1.02);
}
.slide.active { opacity: 1; animation: drift 9s ease-out forwards; }
.slide.still.active { animation: none; }
@keyframes drift { from { transform: scale(1.02); } to { transform: scale(1.1); } }

/* Darkens the left for readable text, and the bottom edge towards the page. */
.shade {
  background:
    linear-gradient(90deg, rgba(8, 15, 34, 0.92) 0%, rgba(8, 15, 34, 0.7) 38%, rgba(8, 15, 34, 0.15) 75%),
    linear-gradient(0deg, rgba(8, 15, 34, 0.75) 0%, rgba(8, 15, 34, 0) 35%);
}

/* Same column as the page content, so the text lines up with it. */
.inner {
  position: relative; height: 100%;
  max-width: 1180px; margin: 0 auto; padding: 0 24px;
  display: flex; align-items: center;
}
.copy { max-width: 620px; }
.title { color: #ffffff; font-size: clamp(36px, 5vw, 60px); font-weight: 750; letter-spacing: -0.02em; line-height: 1.1; }
.sub { font-size: 18px; color: rgba(255, 255, 255, 0.78); margin-top: 16px; max-width: 540px; }

.actions { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 32px; }
.btn-lg { height: 46px; padding: 0 20px; font-size: 15px; border-radius: var(--radius); }
.btn-glass {
  background: rgba(255, 255, 255, 0.12); color: #ffffff;
  border-color: rgba(255, 255, 255, 0.28); backdrop-filter: blur(6px);
}
.truncate { max-width: 260px; overflow: hidden; text-overflow: ellipsis; }
.btn-glass:hover { background: rgba(255, 255, 255, 0.2); color: #ffffff; }

.indicator {
  position: absolute; right: 24px; bottom: 18px;
  display: flex; align-items: center; gap: 12px;
}
.label { font-size: 12px; font-weight: 600; letter-spacing: 0.04em; color: rgba(255, 255, 255, 0.8); }
.dots { display: flex; gap: 6px; }
.dots button {
  width: 8px; height: 8px; padding: 0; border-radius: 999px; border: none; cursor: pointer;
  background: rgba(255, 255, 255, 0.4); transition: width 0.3s, background 0.3s;
}
.dots button.on { width: 24px; background: #ffffff; }

@media (max-width: 640px) {
  .banner { min-height: 420px; }
  .sub { font-size: 15px; }
  .label { display: none; }
}
</style>

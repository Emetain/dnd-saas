<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowRight, Castle, Lock, PlayCircle, ScrollText, Sparkles } from 'lucide-vue-next'
import { campaignApi } from '../services/api'
import { useSession } from '../stores/session'
import { useWelcome } from '../stores/welcome'
import { bannerSlides } from '../data/bannerSlides'

/**
 * The banner below the navigation bar on the home page (and for visitors who
 * are not signed in): a slideshow in a bordered panel, with the tagline
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
  <div class="banner-wrap">
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
          <p class="eyebrow">Campaign prep for Dungeon Masters</p>
          <h1 class="title">Your campaign, remembered.</h1>
          <p class="sub">
            Build your world, run your sessions and let an AI that already knows every NPC, place and plot thread help you prepare.
          </p>

          <div class="actions">
            <!-- Visitors get one clear action; members get one primary, one secondary and a text link. -->
            <button v-if="!state.user" class="btn btn-primary" @click="openAccountForm('register')">
              <Sparkles :size="18" /> Register now — it's free
            </button>
            <template v-else>
              <RouterLink to="/generators/campaign" class="btn btn-primary">
                <Castle :size="18" /> Create a campaign
                <Lock v-if="isFree" :size="16" />
              </RouterLink>
              <RouterLink to="/generators/one-shot" class="btn btn-on-image">
                <ScrollText :size="18" /> Create a one-shot
              </RouterLink>
              <RouterLink v-if="continueLink" :to="continueLink" class="continue">
                <PlayCircle :size="16" /> <span class="truncate">Continue {{ lastCampaign.name }}</span> <ArrowRight :size="16" />
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
  </div>
</template>

<style scoped>
/* A bordered panel inside the page gutter, instead of an edge-to-edge photo with glows. */
.banner-wrap { padding: 16px 16px 0; }
.banner {
  position: relative; overflow: hidden;
  border: 1px solid var(--border); border-radius: var(--radius-lg);
  background: var(--bg); color: var(--on-image);
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

/* One flat scrim — no gradients — keeps the text readable on every photo. */
.shade { background: var(--scrim); }

/* Same column as the page content, so the text lines up with it. */
.inner {
  position: relative;
  max-width: 1180px; margin: 0 auto; padding: 112px 24px 88px;
}
.copy { max-width: 900px; }
.eyebrow { color: var(--on-image-accent); }
.title {
  margin-top: 20px; color: var(--on-image);
  font-weight: 600;
  font-size: clamp(40px, 5.6vw, 72px); line-height: 1.05; letter-spacing: -0.03em;
}
.sub { margin-top: 24px; max-width: 560px; font-size: 19px; line-height: 1.55; color: var(--on-image-muted); }

.actions { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-top: 40px; }
/* Secondary button on the photo: outlined, light text in both themes. */
.btn-on-image { background: transparent; color: var(--on-image); border-color: var(--on-image-border); font-weight: 500; }
.btn-on-image:hover { color: var(--on-image); border-color: var(--on-image); }
.continue {
  display: inline-flex; align-items: center; gap: 8px; min-height: 44px; margin-left: 12px;
  font-size: 15px; font-weight: 500; color: var(--on-image-accent);
}
.continue:hover { color: var(--on-image); }
.truncate { max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.indicator {
  position: absolute; right: 16px; bottom: 12px;
  display: flex; align-items: center; gap: 8px;
}
.label {
  font-size: 12px; font-weight: 500; text-transform: uppercase;
  letter-spacing: 0.06em; color: var(--on-image-muted);
}
.dots { display: flex; }
/* 44px tall hit area; the visible dot is drawn inside it. */
.dots button {
  width: 22px; height: 44px; padding: 0; border: none; background: none; cursor: pointer;
  display: grid; place-items: center;
}
.dots button::before {
  content: ''; width: 8px; height: 8px; border-radius: 999px;
  background: var(--on-image-border); transition: background 0.3s;
}
.dots button.on::before { background: var(--on-image-accent); }

@media (max-width: 640px) {
  .banner-wrap { padding: 12px 12px 0; }
  .inner { padding: 64px 20px 72px; }
  .sub { font-size: 17px; }
  .actions { margin-top: 32px; }
  .continue { margin-left: 0; }
  .label { display: none; }
}
</style>

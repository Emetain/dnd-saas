<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { linesFor, pickTheme } from '../data/loadingLines'

/**
 * Full-screen "the AI is working" overlay: a big spinner, an estimated percentage
 * and a status line that fits the tone of what is being generated.
 *
 * For a single request the percentage is an estimate based on `expectedSeconds`:
 * it eases towards 95% and only jumps to 100% when the request is done.
 * For a job with real steps (`stepsTotal` > 0), it follows the finished steps and
 * eases within the current one, using `expectedSeconds` per step.
 */
const props = defineProps({
  active: { type: Boolean, default: false },
  /** Set when the request failed, so the overlay closes without showing 100%. */
  failed: { type: Boolean, default: false },
  expectedSeconds: { type: Number, default: 40 },
  title: { type: String, default: 'Generating…' },
  tone: { type: String, default: '' },
  themes: { type: String, default: '' },
  idea: { type: String, default: '' },
  /** Real progress, when known: finished and total steps, plus a line like "Session 3 of 10". */
  stepsDone: { type: Number, default: 0 },
  stepsTotal: { type: Number, default: 0 },
  detail: { type: String, default: '' },
})

const LINE_MS = 7000

const visible = ref(false)
const progress = ref(0)
const lineTick = ref(0)
const done = ref(false)
let phases = linesFor('default')
let startedAt = 0
let stepStartedAt = 0
let timer = null
let hideTimer = null

function start() {
  clearTimeout(hideTimer)
  phases = linesFor(pickTheme(props))
  startedAt = performance.now()
  stepStartedAt = startedAt
  progress.value = 0
  lineTick.value = 0
  done.value = false
  visible.value = true
  clearInterval(timer)
  timer = setInterval(() => {
    const now = performance.now()
    const elapsed = now - startedAt
    if (props.stepsTotal > 0) {
      // Finished steps count fully; the current one eases towards 90% of its share.
      const within = 0.9 * ease(now - stepStartedAt)
      const estimate = (100 * Math.min(props.stepsDone + within, props.stepsTotal)) / props.stepsTotal
      progress.value = Math.max(progress.value, Math.min(estimate, 99))
    } else {
      // ~84% at the expected time, ~93% at twice that, never past 95% until the request is done.
      progress.value = 95 * ease(elapsed)
    }
    lineTick.value = Math.floor(elapsed / LINE_MS)
  }, 100)
}

/** 0 → 1 over time: ~0.89 at `expectedSeconds`. */
function ease(ms) {
  return 1 - Math.exp((-2.2 * ms) / (props.expectedSeconds * 1000))
}

watch(() => props.stepsDone, () => { stepStartedAt = performance.now() })

function finish() {
  clearInterval(timer)
  if (props.failed) {
    visible.value = false
    return
  }
  progress.value = 100
  done.value = true
  hideTimer = setTimeout(() => { visible.value = false }, 700)
}

watch(() => props.active, (active) => (active ? start() : visible.value && finish()), { immediate: true })

onBeforeUnmount(() => {
  clearInterval(timer)
  clearTimeout(hideTimer)
})

const percent = computed(() => Math.floor(progress.value))

const status = computed(() => {
  if (done.value) return 'Done! Rolling out the red carpet…'
  const phase = phases[Math.min(phases.length - 1, Math.floor(progress.value / 25))]
  return phase[lineTick.value % phase.length]
})
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="overlay" role="dialog" aria-modal="true" :aria-label="title">
        <div class="content">
          <span class="big-spinner" aria-hidden="true" />
          <div
            class="percent" role="progressbar" aria-valuemin="0" aria-valuemax="100"
            :aria-valuenow="percent" :aria-label="title"
          >{{ percent }}%</div>
          <div class="bar"><div class="bar-fill" :style="{ width: `${progress}%` }" /></div>
          <p class="title">{{ title }}</p>
          <p v-if="detail" class="detail">{{ detail }}</p>
          <Transition name="line" mode="out-in">
            <p :key="status" class="status" aria-live="polite">{{ status }}</p>
          </Transition>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.overlay {
  position: fixed; inset: 0; z-index: 200;
  display: grid; place-items: center; padding: 16px;
  background: rgba(8, 14, 30, 0.72); backdrop-filter: blur(3px);
  color: #fff; cursor: progress;
}
.content {
  display: flex; flex-direction: column; align-items: center; gap: 14px;
  width: min(440px, 100%); text-align: center;
}
.big-spinner {
  width: 112px; height: 112px; border-radius: 50%;
  border: 8px solid rgba(255, 255, 255, 0.18); border-top-color: #fff;
  animation: spin 0.9s linear infinite;
  margin-bottom: 8px;
}
.percent { font-size: 40px; font-weight: 700; font-variant-numeric: tabular-nums; line-height: 1; }
.bar { width: 100%; height: 6px; border-radius: 999px; background: rgba(255, 255, 255, 0.18); overflow: hidden; }
.bar-fill {
  height: 100%; border-radius: inherit;
  background: linear-gradient(90deg, var(--primary), var(--accent));
  transition: width 0.3s ease-out;
}
.title { margin: 6px 0 0; font-weight: 600; font-size: 17px; }
.detail { margin: -6px 0 0; color: rgba(255, 255, 255, 0.9); font-variant-numeric: tabular-nums; }
.status { margin: 0; min-height: 3em; color: rgba(255, 255, 255, 0.8); font-style: italic; line-height: 1.5; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
.line-enter-active, .line-leave-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.line-enter-from { opacity: 0; transform: translateY(6px); }
.line-leave-to { opacity: 0; transform: translateY(-6px); }

@media (prefers-reduced-motion: reduce) {
  .big-spinner { animation-duration: 2.4s; }
  .line-enter-active, .line-leave-active, .bar-fill { transition: none; }
}
</style>

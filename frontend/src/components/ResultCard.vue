<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { Check, Copy } from 'lucide-vue-next'

/**
 * The card every generator result is shown in: a small label and the name in
 * the header with the actions on the right, the result's fields in the body,
 * and the chosen options plus the roll number in the footer strip.
 *
 * Each generator fills the body with its own fields (usually <ResultFields>)
 * and adds its own actions (Reroll, Save, Open …) in the `actions` slot.
 */
const props = defineProps({
  /** Shown small and gold above the name, e.g. "Half-elf · Harbormaster". */
  label: { type: String, default: '' },
  title: { type: String, required: true },
  /** The options this result was made with, e.g. "Level 5–10 · Coastal city · Grim". */
  options: { type: String, default: '' },
  /** How many times the generator ran on this screen; hidden when 0. */
  roll: { type: Number, default: 0 },
  /** Plain text for the Copy button; no Copy button when empty. */
  copyText: { type: String, default: '' },
  /** Changing it fades the name and fields in again (the action buttons keep focus). */
  contentKey: { type: String, default: null },
})

const copied = ref(false)
let timer = null

async function copy() {
  try {
    await navigator.clipboard.writeText(props.copyText)
    copied.value = true
    clearTimeout(timer)
    timer = setTimeout(() => { copied.value = false }, 2000)
  } catch {
    // Clipboard blocked (e.g. not a secure context) — nothing else to do.
  }
}
onBeforeUnmount(() => clearTimeout(timer))
</script>

<template>
  <article class="result-card">
    <header class="head">
      <div :key="contentKey ?? 'heading'" class="heading" :class="{ fade: contentKey }">
        <p v-if="label" class="result-label">{{ label }}</p>
        <h2 class="name">{{ title }}</h2>
      </div>
      <div class="actions">
        <slot name="actions" />
        <button v-if="copyText" type="button" class="btn btn-secondary btn-sm" @click="copy">
          <Check v-if="copied" :size="16" /><Copy v-else :size="16" />
          {{ copied ? 'Copied' : 'Copy' }}
        </button>
      </div>
    </header>

    <div :key="contentKey ?? 'body'" class="body" :class="{ fade: contentKey }">
      <slot />
    </div>

    <footer v-if="options || roll || $slots['footer-end']" class="foot">
      <span>{{ options }}</span>
      <slot name="footer-end"><span v-if="roll">Roll #{{ roll }}</span></slot>
    </footer>
  </article>
</template>

<style scoped>
.result-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.head {
  padding: 32px;
  border-bottom: 1px solid var(--border);
  display: flex; flex-wrap: wrap; align-items: flex-start; justify-content: space-between; gap: 16px 24px;
}
.heading { min-width: 0; flex: 1 1 320px; }
.result-label {
  font-size: 12px; font-weight: 500; text-transform: uppercase;
  letter-spacing: 0.06em; color: var(--accent-text);
}
.name {
  margin-top: 8px;
  font-weight: 600; letter-spacing: -0.02em;
  font-size: 34px; line-height: 1.1; color: var(--text);
  overflow-wrap: anywhere;
}
.actions { display: flex; flex-wrap: wrap; gap: 8px; }

.body { padding: 32px; }

.foot {
  padding: 14px 32px;
  border-top: 1px solid var(--border);
  display: flex; flex-wrap: wrap; justify-content: space-between; gap: 8px;
  font-size: 13px; color: var(--text-subtle); font-variant-numeric: tabular-nums;
}

/* A soft fade when the content changes; none for people who prefer less motion. */
.fade { animation: fade-in 180ms ease-out; }
@keyframes fade-in { from { opacity: 0; } to { opacity: 1; } }
@media (prefers-reduced-motion: reduce) { .fade { animation: none; } }

@media (max-width: 640px) {
  .head, .body { padding: 20px; }
  .foot { padding: 12px 20px; }
  .name { font-size: 28px; }
}
</style>

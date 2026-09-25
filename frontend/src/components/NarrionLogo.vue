<script setup>
/**
 * The Narrion logo: an "N" in a navy tile whose diagonal is a red quill
 * stroke ending in a pen nib — the N of the name, the quill of "narrate".
 *
 * The wordmark takes the surrounding text colour, so it works on the navy
 * navigation bar and on light or dark pages alike.
 */
defineProps({
  size: { type: Number, default: 28 },
  wordmark: { type: Boolean, default: true },
})
</script>

<template>
  <span class="narrion-logo" :style="{ '--logo-size': `${size}px` }">
    <svg class="mark" viewBox="0 0 32 32" role="img" aria-label="Narrion">
      <defs>
        <linearGradient id="narrion-tile" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stop-color="#2549a8"/>
          <stop offset="1" stop-color="#0d1a3d"/>
        </linearGradient>
      </defs>
      <!-- Navy tile with a faint edge so it still reads on the navy navigation bar -->
      <rect width="32" height="32" rx="8" fill="url(#narrion-tile)"/>
      <rect x="0.5" y="0.5" width="31" height="31" rx="7.5" fill="none" stroke="#ffffff" stroke-opacity="0.1"/>
      <!-- The uprights of the N -->
      <rect x="8" y="8" width="3.6" height="16" rx="1.2" fill="#ffffff"/>
      <rect x="20.4" y="8" width="3.6" height="16" rx="1.2" fill="#ffffff"/>
      <!-- The diagonal: a quill stroke ending in a pen nib, with its slit and breather hole -->
      <path d="M8.3 8H11.8L20.43 17.18L21.65 17.68L23.25 22.21L18.83 20.33L18.41 19.08Z" fill="#e5484d"/>
      <path d="M20.44 19.22L22.63 21.56" stroke="#7a1d22" stroke-width="0.7" stroke-linecap="round"/>
      <circle cx="20.44" cy="19.22" r="0.62" fill="#7a1d22"/>
    </svg>
    <span v-if="wordmark" class="word">Narrion</span>
  </span>
</template>

<style scoped>
.narrion-logo { display: inline-flex; align-items: center; gap: calc(var(--logo-size) * 0.34); }
.mark { width: var(--logo-size); height: var(--logo-size); flex-shrink: 0; display: block; }
.word {
  font-size: calc(var(--logo-size) * 0.62);
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1;
  color: currentColor;
}
</style>

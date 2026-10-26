<script setup>
/**
 * The Narrion logo: the app icon (public/favicon.svg — the same file the
 * browser tab shows) next to the wordmark.
 *
 * The wordmark takes the surrounding text colour, so it works in the light
 * and the dark theme.
 */
defineProps({
  size: { type: Number, default: 28 },
  wordmark: { type: Boolean, default: true },
})
</script>

<template>
  <span class="narrion-logo" :style="{ '--logo-size': `${size}px` }">
    <img class="mark" src="/favicon.svg" :alt="wordmark ? '' : 'Narrion'" :width="size" :height="size" />
    <span v-if="wordmark" class="word">Narrion</span>
  </span>
</template>

<style scoped>
.narrion-logo { display: inline-flex; align-items: center; gap: calc(var(--logo-size) * 0.3); }
/* The icon's tile is the dark page colour, so a hairline keeps its edge visible in the dark theme. */
.mark {
  width: var(--logo-size); height: var(--logo-size); flex-shrink: 0; display: block;
  border-radius: 21.875%; /* the icon's own corner: 112 / 512 */
  box-shadow: 0 0 0 1px var(--border);
}
.word {
  font-size: calc(var(--logo-size) * 0.5);
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1;
  color: currentColor;
}
</style>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronDown } from 'lucide-vue-next'

/**
 * A navbar item that opens a panel on click (and closes on outside click,
 * Escape or navigation). The panel content comes from the default slot;
 * the `trigger` slot replaces the plain text label (e.g. with an avatar).
 */
defineProps({
  label: { type: String, required: true },
  active: { type: Boolean, default: false },
  align: { type: String, default: 'left' },
  wide: { type: Boolean, default: false },
})

const open = ref(false)
const root = ref(null)
const route = useRoute()

function onDocumentClick(event) {
  if (root.value && !root.value.contains(event.target)) open.value = false
}
function onKey(event) {
  if (event.key === 'Escape') open.value = false
}

watch(open, (isOpen) => {
  if (isOpen) {
    document.addEventListener('click', onDocumentClick)
    document.addEventListener('keydown', onKey)
  } else {
    document.removeEventListener('click', onDocumentClick)
    document.removeEventListener('keydown', onKey)
  }
})
watch(() => route.fullPath, () => { open.value = false })
onBeforeUnmount(() => { open.value = false })
</script>

<template>
  <div ref="root" class="dropdown">
    <button class="trigger" :class="{ active, open }" :aria-expanded="open" @click="open = !open">
      <slot name="trigger">{{ label }}</slot>
      <ChevronDown :size="14" class="chevron" />
    </button>
    <div v-if="open" class="panel" :class="[`align-${align}`, { wide }]" role="menu">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.dropdown { position: relative; }
.trigger {
  display: inline-flex; align-items: center; gap: 4px;
  min-height: 44px; padding: 0 12px; border: none; border-radius: var(--radius-sm);
  background: transparent; color: var(--text-muted);
  font: inherit; font-size: 14px; font-weight: 500; cursor: pointer;
}
.trigger:hover, .trigger.open { background: var(--raised); color: var(--text); }
.trigger.active { color: var(--text); }
.chevron { opacity: 0.7; transition: transform 0.15s; }
.trigger.open .chevron { transform: rotate(180deg); }

.panel {
  position: absolute; top: calc(100% + 8px);
  min-width: 240px; padding: 8px;
  background: var(--surface); border: 1px solid var(--border);
  border-radius: var(--radius); box-shadow: var(--float-shadow);
  z-index: 50;
}
.panel.align-left { left: 0; }
.panel.align-right { right: 0; }
.panel.wide { min-width: 640px; }
@media (max-width: 760px) { .panel.wide { min-width: 300px; } }
</style>

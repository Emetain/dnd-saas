<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { X } from 'lucide-vue-next'

/** A pop-up dialog. Closes on the × button, Escape or a click on the backdrop. */
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  wide: { type: Boolean, default: false },
})
const emit = defineEmits(['close'])
const dialog = ref(null)

function onKey(event) {
  if (event.key === 'Escape') emit('close')
}

onMounted(() => {
  document.addEventListener('keydown', onKey)
  document.body.style.overflow = 'hidden'
  dialog.value?.focus()
})
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKey)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <div class="backdrop" @click.self="emit('close')">
      <div ref="dialog" class="dialog" :class="{ wide }" role="dialog" aria-modal="true" :aria-label="title" tabindex="-1">
        <header class="head">
          <div>
            <h2>{{ title }}</h2>
            <p v-if="subtitle" class="muted">{{ subtitle }}</p>
          </div>
          <button class="btn btn-ghost btn-sm" aria-label="Close" @click="emit('close')"><X :size="18" /></button>
        </header>
        <div class="body"><slot /></div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.backdrop {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(8, 14, 30, 0.55); backdrop-filter: blur(2px);
  display: flex; align-items: flex-start; justify-content: center;
  padding: 6vh 16px; overflow-y: auto;
}
.dialog {
  width: 100%; max-width: 640px; outline: none;
  background: var(--bg); border: 1px solid var(--border); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}
.dialog.wide { max-width: 820px; }
.head {
  display: flex; justify-content: space-between; align-items: flex-start; gap: 12px;
  padding: 18px 20px 14px; border-bottom: 1px solid var(--border);
}
.head h2 { margin: 0; }
.head p { margin-top: 2px; }
.body { padding: 18px 20px 20px; }
</style>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { X } from 'lucide-vue-next'

/**
 * Stands in for a real rewarded ad during local development: a full-screen
 * "ad" with a countdown. Emits 'granted' when it finishes, 'dismissed' when closed early.
 */
const props = defineProps({ seconds: { type: Number, required: true } })
const emit = defineEmits(['granted', 'dismissed'])

const left = ref(props.seconds)
const progress = computed(() => 100 - (left.value / props.seconds) * 100)
let timer = null

onMounted(() => {
  document.body.style.overflow = 'hidden'
  timer = setInterval(() => {
    left.value -= 1
    if (left.value <= 0) {
      clearInterval(timer)
      emit('granted')
    }
  }, 1000)
})
onBeforeUnmount(() => {
  clearInterval(timer)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <div class="ad-backdrop" role="dialog" aria-modal="true" aria-label="Advertisement">
      <div class="ad">
        <div class="ad-top">
          <span class="tag">Advertisement · test mode</span>
          <button class="close" :aria-label="left > 0 ? 'Close ad without reward' : 'Close'" @click="emit('dismissed')">
            <span v-if="left > 0">Reward in {{ left }}s</span>
            <X :size="16" />
          </button>
        </div>
        <div class="ad-body">
          <p class="big">Your ad here</p>
          <p class="muted">A real rewarded video plays here once an ad network is connected.</p>
        </div>
        <div class="bar"><div class="fill" :style="{ width: `${progress}%` }" /></div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.ad-backdrop { position: fixed; inset: 0; z-index: 200; background: rgba(0, 0, 0, 0.85); display: grid; place-items: center; padding: 16px; }
.ad { width: 100%; max-width: 720px; aspect-ratio: 16 / 9; background: #0b1630; color: #fff; border-radius: 14px; overflow: hidden; display: flex; flex-direction: column; }
.ad-top { display: flex; justify-content: space-between; align-items: center; padding: 12px 14px; }
.tag { font-size: 12px; color: rgba(255, 255, 255, 0.7); }
.close { display: inline-flex; align-items: center; gap: 6px; border: none; border-radius: 999px; padding: 6px 12px; background: rgba(255, 255, 255, 0.14); color: #fff; font: inherit; font-size: 13px; cursor: pointer; }
.ad-body { flex: 1; display: grid; place-content: center; text-align: center; gap: 8px; padding: 0 24px; }
.big { font-size: 34px; font-weight: 750; }
.muted { color: rgba(255, 255, 255, 0.65); }
.bar { height: 5px; background: rgba(255, 255, 255, 0.15); }
.fill { height: 100%; background: #e5484d; transition: width 1s linear; }
</style>

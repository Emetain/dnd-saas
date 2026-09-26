<script setup>
import { watch } from 'vue'
import { Coins, Lock } from 'lucide-vue-next'
import { useAccess } from '../stores/access'

/**
 * Explains a failed generation, with a way forward for 402 (tokens) and 403 (tier).
 * A 403 also opens the upgrade pop-up for the plan the backend says is needed.
 */
const props = defineProps({ error: { type: Object, required: true } })
const { showUpgrade } = useAccess()

watch(() => props.error, (error) => {
  if (error?.status === 403 && error.body?.requiredTier) showUpgrade('This feature', error.body.requiredTier)
}, { immediate: true })
</script>

<template>
  <div v-if="error.status === 402" class="alert alert-error">
    <Coins :size="16" />
    <span>
      {{ error.message }}.
      <RouterLink to="/earn">Earn tokens</RouterLink>, <RouterLink to="/billing#packs">buy a pack</RouterLink>
      or <RouterLink to="/billing#plans">upgrade</RouterLink>.
    </span>
  </div>
  <div v-else-if="error.status === 403" class="alert alert-error">
    <Lock :size="16" />
    <span>{{ error.message }}. <RouterLink to="/billing#plans">Compare plans</RouterLink></span>
  </div>
  <div v-else class="alert alert-error">{{ error.message }}</div>
</template>

<script setup>
import { Coins, Lock } from 'lucide-vue-next'

/** Explains a failed generation, with a way forward for 402 (tokens) and 403 (tier). */
defineProps({ error: { type: Object, required: true } })
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

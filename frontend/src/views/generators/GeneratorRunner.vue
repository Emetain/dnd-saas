<script setup>
import { computed } from 'vue'
import { findGenerator } from '../../data/generators'
import { useAccess } from '../../stores/access'
import WorldGenerator from './WorldGenerator.vue'
import ModuleGenerator from './ModuleGenerator.vue'
import UpgradePrompt from '../../components/UpgradePrompt.vue'

/** Picks the right screen for the generator in the URL. */
const props = defineProps({ slug: { type: String, required: true } })
const { isLocked } = useAccess()

const generator = computed(() => findGenerator(props.slug))
</script>

<template>
  <div v-if="!generator" class="empty"><h3>Unknown generator</h3></div>
  <div v-else :key="generator.slug">
    <div class="page-header">
      <div class="row head">
        <span class="icon"><component :is="generator.icon" :size="20" /></span>
        <div>
          <h1>{{ generator.label }}</h1>
          <p>{{ generator.description }}</p>
        </div>
      </div>
      <span class="badge">{{ generator.cost }} tokens</span>
    </div>

    <UpgradePrompt v-if="isLocked(generator)" :feature="generator.label" :tier="generator.minTier" />
    <WorldGenerator v-else-if="generator.kind === 'world'" :generator="generator" />
    <ModuleGenerator v-else :generator="generator" />
  </div>
</template>

<style scoped>
.head { align-items: flex-start; gap: 14px; }
.icon {
  width: 44px; height: 44px; flex-shrink: 0; border-radius: var(--radius-btn); display: grid; place-items: center;
  background: var(--surface); border: 1px solid var(--border); color: var(--accent-text);
}
</style>

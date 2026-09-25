<script setup>
import { computed } from 'vue'
import { findGenerator } from '../../data/generators'
import { useSession } from '../../stores/session'
import WorldGenerator from './WorldGenerator.vue'
import ModuleGenerator from './ModuleGenerator.vue'
import UpgradePrompt from '../../components/UpgradePrompt.vue'

/** Picks the right screen for the generator in the URL. */
const props = defineProps({ slug: { type: String, required: true } })
const { isFree } = useSession()

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
      <span class="badge badge-navy">{{ generator.cost }} tokens</span>
    </div>

    <UpgradePrompt v-if="isFree && !generator.free" :feature="generator.label" />
    <WorldGenerator v-else-if="generator.kind === 'world'" :generator="generator" />
    <ModuleGenerator v-else :generator="generator" />
  </div>
</template>

<style scoped>
.head { align-items: flex-start; gap: 14px; }
.icon {
  width: 42px; height: 42px; flex-shrink: 0; border-radius: 10px; display: grid; place-items: center;
  background: var(--strong-bg); color: var(--on-color);
}
</style>

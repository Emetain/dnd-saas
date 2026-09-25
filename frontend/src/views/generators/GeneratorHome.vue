<script setup>
import { Lock } from 'lucide-vue-next'
import { generatorGroups } from '../../data/generators'
import { useSession } from '../../stores/session'

const { isFree } = useSession()
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1>Generators</h1>
        <p>Every generator already knows your campaign — its places, people and history.</p>
      </div>
    </div>

    <section v-for="group in generatorGroups" :key="group.id" class="group">
      <h2>{{ group.label }}</h2>
      <div class="grid grid-2">
        <RouterLink v-for="gen in group.generators" :key="gen.slug" :to="`/generators/${gen.slug}`" class="card card-link gen">
          <span class="icon"><component :is="gen.icon" :size="18" /></span>
          <div class="text">
            <div class="row-between">
              <h3>{{ gen.label }}</h3>
              <span v-if="isFree && !gen.free" class="badge badge-red"><Lock :size="11" /> Pro</span>
              <span v-else class="badge">{{ gen.cost }} tokens</span>
            </div>
            <p class="muted">{{ gen.description }}</p>
          </div>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.group { margin-bottom: 28px; }
.group h2 { margin-bottom: 12px; }
.gen { display: flex; gap: 14px; align-items: flex-start; }
.gen h3 { margin: 0; }
.text { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.icon {
  width: 36px; height: 36px; flex-shrink: 0; border-radius: 8px; display: grid; place-items: center;
  background: var(--primary-soft); color: var(--primary-text);
}
</style>

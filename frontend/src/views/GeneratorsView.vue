<script setup>
import { Lock } from 'lucide-vue-next'
import { generatorGroups } from '../data/generators'
import { useSession } from '../stores/session'

const { isFree } = useSession()
</script>

<template>
  <div class="page layout">
    <aside class="sidebar" aria-label="Generators">
      <RouterLink to="/generators" class="sidebar-home" exact-active-class="active">All generators</RouterLink>
      <div v-for="group in generatorGroups" :key="group.id" class="group">
        <div class="group-title">{{ group.label }}</div>
        <RouterLink
          v-for="gen in group.generators"
          :key="gen.slug"
          :to="{ path: `/generators/${gen.slug}`, query: $route.query.campaign ? { campaign: $route.query.campaign } : {} }"
          class="item"
          active-class="active"
        >
          <component :is="gen.icon" :size="16" />
          <span class="name">{{ gen.label }}</span>
          <Lock v-if="isFree && !gen.free" :size="12" class="lock" />
          <span v-else class="cost">{{ gen.cost }}</span>
        </RouterLink>
      </div>
    </aside>
    <section class="content">
      <RouterView />
    </section>
  </div>
</template>

<style scoped>
.layout { display: grid; grid-template-columns: 240px 1fr; gap: 28px; align-items: start; }
.sidebar {
  position: sticky; top: calc(var(--nav-height) + 24px);
  background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius);
  padding: 10px; display: flex; flex-direction: column; gap: 4px;
}
.sidebar-home { padding: 8px 10px; border-radius: var(--radius-sm); font-weight: 600; color: var(--text-strong); }
.sidebar-home:hover, .sidebar-home.active { background: var(--primary-soft); color: var(--primary-text); }
.group { display: flex; flex-direction: column; margin-top: 6px; }
.group-title {
  font-size: 11px; font-weight: 650; text-transform: uppercase; letter-spacing: 0.06em;
  color: var(--text-muted); padding: 8px 10px 4px;
}
.item {
  display: flex; align-items: center; gap: 10px; padding: 7px 10px; border-radius: var(--radius-sm);
  color: var(--text-secondary); font-weight: 520;
}
.item:hover { background: var(--surface-muted); color: var(--text-strong); }
.item.active { background: var(--primary-soft); color: var(--primary-text); box-shadow: inset 3px 0 0 var(--primary); }
.item .name { flex: 1; }
.cost { font-size: 11px; color: var(--text-muted); font-weight: 600; }
.lock { color: var(--accent); }
.content { min-width: 0; }
@media (max-width: 860px) {
  .layout { grid-template-columns: 1fr; }
  .sidebar { position: static; }
}
</style>

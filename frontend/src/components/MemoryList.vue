<script setup>
/**
 * Renders one kind of Campaign Memory (NPCs, locations, ...) as cards.
 * `fields` lists the extra properties worth showing, as [key, label] pairs.
 */
const props = defineProps({
  items: { type: Array, required: true },
  titleKey: { type: String, default: 'name' },
  tagKeys: { type: Array, default: () => [] },
  bodyKey: { type: String, default: 'description' },
  fields: { type: Array, default: () => [] },
  emptyText: { type: String, default: 'Nothing here yet.' },
})

function display(value) {
  if (value == null || value === '') return null
  if (Array.isArray(value)) return value.map(display).filter(Boolean).join(', ') || null
  if (typeof value === 'object') return value.name ?? value.title ?? null
  return String(value).replaceAll('_', ' ')
}

function tags(item) {
  return props.tagKeys.map((key) => display(item[key])).filter(Boolean)
}
</script>

<template>
  <div v-if="!items.length" class="empty">
    <p>{{ emptyText }}</p>
  </div>
  <div v-else class="grid grid-2">
    <article v-for="item in items" :key="item.id" class="card entity">
      <div class="row-between">
        <h3>{{ item[titleKey] }}</h3>
        <span v-if="item.aiGenerated" class="badge badge-blue">AI</span>
      </div>
      <div v-if="tags(item).length" class="row tags">
        <span v-for="tag in tags(item)" :key="tag" class="badge">{{ tag }}</span>
      </div>
      <p v-if="item[bodyKey]" class="body">{{ item[bodyKey] }}</p>
      <dl v-if="fields.length">
        <template v-for="[key, label] in fields" :key="key">
          <template v-if="display(item[key])">
            <dt>{{ label }}</dt>
            <dd>{{ display(item[key]) }}</dd>
          </template>
        </template>
      </dl>
    </article>
  </div>
</template>

<style scoped>
.entity { display: flex; flex-direction: column; gap: 8px; }
.entity h3 { margin: 0; }
.tags { flex-wrap: wrap; gap: 6px; }
.body { color: var(--text-secondary); white-space: pre-line; }
dl { display: grid; grid-template-columns: max-content 1fr; gap: 4px 12px; margin: 4px 0 0; font-size: 13px; }
dt { color: var(--text-muted); font-weight: 560; }
dd { margin: 0; color: var(--text-secondary); white-space: pre-line; }
</style>

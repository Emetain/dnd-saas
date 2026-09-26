<script setup>
import { ref } from 'vue'
import { Pencil, Trash2 } from 'lucide-vue-next'
import { memoryApi } from '../services/api'

/**
 * Renders one kind of Campaign Memory (NPCs, locations, ...) as cards.
 * `fields` lists the extra properties worth showing, as [key, label] pairs.
 * With `resource` (e.g. "npcs"), each card can be deleted; `changed` is emitted afterwards.
 * With `editable`, each card also gets an edit button that emits `edit`.
 */
const emit = defineEmits(['changed', 'edit'])
const props = defineProps({
  resource: { type: String, default: '' },
  editable: { type: Boolean, default: false },
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

const deleting = ref(null)
const error = ref('')

async function remove(item) {
  const name = item[props.titleKey] || 'this'
  if (!window.confirm(`Delete "${name}"? The AI will no longer know about it. This cannot be undone.`)) return
  deleting.value = item.id
  error.value = ''
  try {
    await memoryApi.remove(props.resource, item.id)
    emit('changed')
  } catch (e) {
    error.value = e.message
  } finally {
    deleting.value = null
  }
}
</script>

<template>
  <div v-if="error" class="alert alert-error list-error">{{ error }}</div>
  <div v-if="!items.length" class="empty">
    <p>{{ emptyText }}</p>
  </div>
  <div v-else class="grid grid-2">
    <article v-for="item in items" :key="item.id" class="card entity">
      <div class="row-between">
        <h3>{{ item[titleKey] }}</h3>
        <div class="row actions">
          <span v-if="item.aiGenerated" class="badge badge-blue">AI</span>
          <button
            v-if="editable" class="btn btn-ghost btn-sm delete"
            :title="`Edit ${item[titleKey]}`" :aria-label="`Edit ${item[titleKey]}`" @click="emit('edit', item)"
          >
            <Pencil :size="14" />
          </button>
          <button
            v-if="resource" class="btn btn-ghost btn-sm delete" :disabled="deleting === item.id"
            :title="`Delete ${item[titleKey]}`" :aria-label="`Delete ${item[titleKey]}`" @click="remove(item)"
          >
            <Trash2 :size="14" />
          </button>
        </div>
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
.actions { gap: 4px; flex-shrink: 0; }
.delete { color: var(--text-muted); }
.delete:hover:not(:disabled) { color: var(--accent-text); }
.list-error { margin-bottom: 12px; }
.tags { flex-wrap: wrap; gap: 6px; }
.body { color: var(--text-secondary); white-space: pre-line; }
dl { display: grid; grid-template-columns: max-content 1fr; gap: 4px 12px; margin: 4px 0 0; font-size: 13px; }
dt { color: var(--text-muted); font-weight: 560; }
dd { margin: 0; color: var(--text-secondary); white-space: pre-line; }
</style>

<script setup>
import { humanize } from '../utils/text'

/**
 * Renders a generator's structured JSON result as readable content:
 * objects become labelled sections, lists of objects become cards, and
 * lists of strings become bullet lists. Works for every generator, so new
 * generators need no new result screens.
 */
defineOptions({ name: 'JsonView' })
const props = defineProps({
  value: { required: true },
  depth: { type: Number, default: 0 },
})

const isObject = (v) => v !== null && typeof v === 'object' && !Array.isArray(v)
const isEmpty = (v) => v == null || v === '' || (Array.isArray(v) && v.length === 0)
const entries = (obj) => Object.entries(obj).filter(([, v]) => !isEmpty(v))
const titleOf = (obj) => obj.name || obj.title || null
</script>

<template>
  <!-- Object: labelled fields -->
  <div v-if="isObject(props.value)" class="obj" :class="`depth-${depth}`">
    <template v-for="[key, val] in entries(props.value)" :key="key">
      <div v-if="!(depth > 0 && (key === 'name' || key === 'title'))" class="field-row">
        <div class="key">{{ humanize(key) }}</div>
        <div class="val"><JsonView :value="val" :depth="depth + 1" /></div>
      </div>
    </template>
  </div>

  <!-- List of objects: cards -->
  <div v-else-if="Array.isArray(props.value) && props.value.some(isObject)" class="cards">
    <div v-for="(item, i) in props.value" :key="i" class="item-card">
      <div v-if="isObject(item) && titleOf(item)" class="item-title">{{ titleOf(item) }}</div>
      <JsonView :value="item" :depth="depth + 1" />
    </div>
  </div>

  <!-- List of primitives: bullets -->
  <ul v-else-if="Array.isArray(props.value)" class="bullets">
    <li v-for="(item, i) in props.value" :key="i">{{ item }}</li>
  </ul>

  <span v-else-if="typeof props.value === 'boolean'">{{ props.value ? 'Yes' : 'No' }}</span>
  <p v-else class="text">{{ props.value }}</p>
</template>

<style scoped>
.obj { display: flex; flex-direction: column; gap: 16px; }
.obj.depth-1, .obj.depth-2, .obj.depth-3 { gap: 8px; }
.field-row { display: flex; flex-direction: column; gap: 6px; }
.depth-1 .field-row, .depth-2 .field-row { flex-direction: row; gap: 12px; }
.depth-1 .key, .depth-2 .key { min-width: 130px; }
.key {
  font-size: 12px; font-weight: 500; text-transform: uppercase;
  letter-spacing: 0.06em; color: var(--text-subtle);
}
.depth-1 .key, .depth-2 .key { font-weight: 400; text-transform: none; letter-spacing: 0; font-size: 14px; color: var(--text-muted); }
.val { flex: 1; min-width: 0; }
.text { white-space: pre-line; color: var(--text-body); line-height: 1.6; }
.cards { display: grid; gap: 12px; }
.item-card { border: 1px solid var(--border); border-radius: var(--radius); padding: 16px 18px; background: var(--inset); }
.item-title { font-size: 17px; font-weight: 600; line-height: 1.3; color: var(--text); margin-bottom: 10px; }
.bullets { margin: 0; padding-left: 18px; display: grid; gap: 4px; color: var(--text-body); }
.bullets li::marker { color: var(--accent-text); }
@media (max-width: 640px) { .depth-1 .field-row, .depth-2 .field-row { flex-direction: column; gap: 2px; } }
</style>

<script setup>
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

function humanize(key) {
  return key
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/_/g, ' ')
    .replace(/^./, (c) => c.toUpperCase())
}
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
.obj { display: flex; flex-direction: column; gap: 14px; }
.obj.depth-1, .obj.depth-2, .obj.depth-3 { gap: 8px; }
.field-row { display: flex; flex-direction: column; gap: 4px; }
.depth-1 .field-row, .depth-2 .field-row { flex-direction: row; gap: 12px; }
.depth-1 .key, .depth-2 .key { min-width: 130px; }
.key { font-size: 12px; font-weight: 620; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.04em; }
.depth-1 .key, .depth-2 .key { text-transform: none; letter-spacing: 0; font-size: 13px; }
.val { flex: 1; min-width: 0; }
.text { white-space: pre-line; color: var(--text); line-height: 1.6; }
.cards { display: grid; gap: 10px; }
.item-card { border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 12px 14px; background: var(--surface-sunken); }
.item-title { font-weight: 620; color: var(--text-strong); margin-bottom: 8px; }
.bullets { margin: 0; padding-left: 18px; display: grid; gap: 4px; }
@media (max-width: 640px) { .depth-1 .field-row, .depth-2 .field-row { flex-direction: column; gap: 2px; } }
</style>

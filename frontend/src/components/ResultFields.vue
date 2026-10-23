<script setup>
import { computed } from 'vue'
import { humanize } from '../utils/text'
import JsonView from './JsonView.vue'

/**
 * A generator result's fields in the result card body: a two-column grid of
 * small labels and values. The fields a DM reaches for first (secrets, hooks,
 * twists …) sit in highlight boxes; lists and long text take the full width.
 */
const props = defineProps({
  /** A generator's JSON result; each top-level key becomes a field. */
  value: { type: Object, default: null },
  /** Or ready-made fields: { label, value, highlight? } (the showcase examples). */
  fields: { type: Array, default: null },
  /** Fields that are shown elsewhere (the name in the card header, for example). */
  hide: { type: Array, default: () => ['name', 'title'] },
  highlight: {
    type: Array,
    default: () => ['secret', 'hook', 'storyHook', 'openingHook', 'twist', 'complication', 'flaw', 'quirk', 'weakness', 'cliffhanger'],
  },
})

const isEmpty = (v) => v == null || v === '' || (Array.isArray(v) && v.length === 0)

const fromList = (list) =>
  list.map((f) => ({ key: f.label, label: f.label, val: f.value, text: true, wide: String(f.value).length > 180, highlight: !!f.highlight }))

const items = computed(() =>
  props.fields ? fromList(props.fields) : Object.entries(props.value || {})
    .filter(([key, val]) => !props.hide.includes(key) && !isEmpty(val))
    .map(([key, val]) => ({
      key,
      label: humanize(key),
      val,
      text: typeof val === 'string' || typeof val === 'number',
      wide: typeof val === 'object' || String(val).length > 180,
      highlight: props.highlight.includes(key),
    })),
)
</script>

<template>
  <dl class="fields">
    <div v-for="f in items" :key="f.key" class="field-item" :class="{ wide: f.wide, highlight: f.highlight }">
      <dt class="mono-label">{{ f.label }}</dt>
      <dd>
        <p v-if="f.text" class="value">{{ f.val }}</p>
        <JsonView v-else :value="f.val" :depth="1" />
      </dd>
    </div>
  </dl>
</template>

<style scoped>
.fields {
  margin: 0;
  display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px 32px;
}
.field-item { min-width: 0; }
.field-item.wide { grid-column: 1 / -1; }
.field-item.highlight {
  padding: 18px 20px;
  background: var(--inset); border: 1px solid var(--border); border-radius: var(--radius);
}
dt { margin-bottom: 6px; }
dd { margin: 0; }
.value { font-size: 16px; line-height: 1.6; color: var(--text-body); white-space: pre-line; overflow-wrap: anywhere; }

@media (max-width: 640px) {
  .fields { grid-template-columns: 1fr; }
}
</style>

<script setup>
import { computed, reactive, ref } from 'vue'
import AppModal from './AppModal.vue'
import { memoryApi } from '../services/api'
import { memoryForms, optionLabel } from '../data/memoryForms'

/**
 * Add or edit an NPC, location, quest or item by hand. The form comes from
 * data/memoryForms.js; links (lives in, quest giver…) are picked from the
 * campaign's own memory.
 */
const props = defineProps({
  resource: { type: String, required: true },
  /** The entry being edited, or null to add a new one. */
  item: { type: Object, default: null },
  memory: { type: Object, required: true },
})
const emit = defineEmits(['close', 'saved'])

const form = computed(() => memoryForms[props.resource])
const isNew = computed(() => !props.item)
const nameField = computed(() => form.value.fields[0])

/** Starts from the entry's current values, or each field's default. */
function initialValues() {
  const values = {}
  for (const field of form.value.fields) {
    const current = props.item
    if (field.kind === 'ref') values[field.key] = current?.[field.from]?.id ?? null
    else if (field.kind === 'refs') values[field.key] = (current?.[field.from] ?? []).map((ref) => ref.id)
    else if (current && current[field.key] != null) values[field.key] = current[field.key]
    else values[field.key] = field.default ?? (field.kind === 'checkbox' ? false : '')
  }
  return values
}

const values = reactive(initialValues())
const saving = ref(false)
const error = ref('')

function choices(field) {
  const list = props.memory[field.source] ?? []
  return list
    .filter((entry) => !(field.excludeSelf && entry.id === props.item?.id))
    .map((entry) => ({ id: entry.id, name: entry.name ?? entry.title }))
    .sort((a, b) => a.name.localeCompare(b.name))
}

const canSave = computed(() => String(values[nameField.value.key] ?? '').trim().length > 0)

async function save() {
  if (!canSave.value) return
  saving.value = true
  error.value = ''
  try {
    const body = { ...values }
    const saved = isNew.value
      ? await memoryApi.create(props.memory.campaign.id, props.resource, body)
      : await memoryApi.update(props.resource, props.item.id, body)
    emit('saved', saved)
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <AppModal
    :title="isNew ? `Add ${form.singular}` : `Edit ${item[nameField.key]}`"
    subtitle="Everything you write here becomes part of Campaign Memory — the AI reads it for every generation."
    wide
    @close="emit('close')"
  >
    <form class="stack" novalidate @submit.prevent="save">
      <div class="grid grid-2 fields">
        <div v-for="field in form.fields" :key="field.key" class="field" :class="{ wide: field.wide }">
          <label v-if="field.kind === 'checkbox'" class="check">
            <input v-model="values[field.key]" type="checkbox" /> {{ field.label }}
          </label>
          <template v-else>
            <label :for="`m-${field.key}`">{{ field.label }} <span v-if="field.required" class="muted">(required)</span></label>
            <input
              v-if="field.kind === 'text'" :id="`m-${field.key}`" v-model="values[field.key]" class="input"
              :required="field.required" :placeholder="field.placeholder"
            />
            <textarea v-else-if="field.kind === 'textarea'" :id="`m-${field.key}`" v-model="values[field.key]" class="textarea short" />
            <select v-else-if="field.kind === 'select'" :id="`m-${field.key}`" v-model="values[field.key]" class="select">
              <option v-for="option in field.options" :key="option" :value="option">{{ optionLabel(option) }}</option>
            </select>
            <select v-else-if="field.kind === 'ref'" :id="`m-${field.key}`" v-model="values[field.key]" class="select">
              <option :value="null">— None —</option>
              <option v-for="choice in choices(field)" :key="choice.id" :value="choice.id">{{ choice.name }}</option>
            </select>
            <div v-else-if="field.kind === 'refs'" :id="`m-${field.key}`" class="chips">
              <label v-for="choice in choices(field)" :key="choice.id" class="chip" :class="{ active: values[field.key].includes(choice.id) }">
                <input v-model="values[field.key]" type="checkbox" :value="choice.id" /> {{ choice.name }}
              </label>
              <span v-if="!choices(field).length" class="muted small">Nothing to pick yet.</span>
            </div>
          </template>
        </div>
      </div>

      <div v-if="error" class="alert alert-error" role="alert">{{ error }}</div>
      <div class="row actions">
        <button type="button" class="btn btn-ghost" @click="emit('close')">Cancel</button>
        <button class="btn btn-primary" :disabled="saving || !canSave">
          <span v-if="saving" class="spinner" /> {{ isNew ? `Add ${form.singular}` : 'Save changes' }}
        </button>
      </div>
    </form>
  </AppModal>
</template>

<style scoped>
.fields { gap: 14px 16px; }
.wide { grid-column: 1 / -1; }
.textarea.short { min-height: 72px; }
.check { display: flex; gap: 8px; align-items: center; cursor: pointer; margin-top: 26px; }
.chips { display: flex; flex-wrap: wrap; gap: 6px; }
.chip {
  display: inline-flex; align-items: center; gap: 6px; cursor: pointer;
  border: 1px solid var(--border-strong); border-radius: 999px; padding: 4px 12px; font-size: 13px; color: var(--text-secondary);
}
.chip input { position: absolute; opacity: 0; pointer-events: none; }
.chip:focus-within { outline: 2px solid var(--primary); outline-offset: 2px; }
.chip.active { background: var(--primary); border-color: var(--primary); color: var(--on-color); }
.actions { justify-content: flex-end; }
@media (max-width: 640px) { .check { margin-top: 0; } }
</style>

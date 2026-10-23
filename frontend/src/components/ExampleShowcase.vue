<script setup>
import { computed, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Dices, Lock, Sparkles } from 'lucide-vue-next'
import ResultCard from './ResultCard.vue'
import ResultFields from './ResultFields.vue'
import { exampleTypes } from '../data/examples'
import { findGenerator } from '../data/generators'
import { useSession } from '../stores/session'
import { useWelcome } from '../stores/welcome'

/**
 * Example results below the banner: pick a type, flip through its examples
 * one at a time, then open one in its generator ("Use this example") or open
 * the generator empty ("Try this generator"). Never rotates by itself.
 *
 * The examples live in data/examples.js.
 */
const { state, isFree } = useSession()
const { openAccountForm } = useWelcome()
const router = useRouter()

const typeIndex = ref(0)
const exampleIndex = ref(0)
const tabs = ref([])

const type = computed(() => exampleTypes[typeIndex.value])
const example = computed(() => type.value.examples[exampleIndex.value])
const generator = computed(() => findGenerator(type.value.generator))
/** Same rule as the navigation: Free (and new) accounts see a lock on Pro generators. */
const locked = computed(() => (!state.user || isFree.value) && !generator.value.free)

function selectType(index) {
  typeIndex.value = index
  exampleIndex.value = 0
}

/** Arrow keys, Home and End move between the type tabs (and select them). */
function onTabKey(event, index) {
  const last = exampleTypes.length - 1
  const next = {
    ArrowRight: index === last ? 0 : index + 1,
    ArrowLeft: index === 0 ? last : index - 1,
    Home: 0,
    End: last,
  }[event.key]
  if (next === undefined) return
  event.preventDefault()
  selectType(next)
  nextTick(() => tabs.value[next]?.focus())
}

function showAnother() {
  exampleIndex.value = (exampleIndex.value + 1) % type.value.examples.length
}

/**
 * Opens the generator, with this example filled in or empty. Visitors are
 * taken there too, and asked to create an account first; once they have
 * one, the generator is what they see.
 */
async function open(withExample) {
  const to = { path: `/generators/${generator.value.slug}`, query: withExample ? { example: example.value.id } : {} }
  await router.push(to)
  if (!state.user) setTimeout(() => openAccountForm('register'))
}
</script>

<template>
  <section class="showcase" aria-labelledby="showcase-title">
    <div class="inner">
      <div class="intro">
        <p class="eyebrow">Example results</p>
        <h2 id="showcase-title">See what the generators make</h2>
        <p class="sub">Browse a few results, then open one in its generator to reroll it, copy it or make it your own.</p>
      </div>

      <div class="types" role="tablist" aria-label="Example type">
        <button
          v-for="(t, i) in exampleTypes" :id="`example-tab-${t.id}`" :key="t.id"
          ref="tabs"
          type="button" role="tab" class="type"
          :aria-selected="i === typeIndex" aria-controls="example-panel" :tabindex="i === typeIndex ? 0 : -1"
          @click="selectType(i)" @keydown="onTabKey($event, i)"
        >
          {{ t.label }}
        </button>
      </div>

      <div id="example-panel" role="tabpanel" :aria-labelledby="`example-tab-${type.id}`">
        <ResultCard :label="example.label" :title="example.name" :options="example.settings" :content-key="example.id">
          <template #actions>
            <div class="another">
              <button type="button" class="btn btn-secondary btn-sm" :disabled="type.examples.length < 2" @click="showAnother">
                <Dices :size="16" /> Show another
              </button>
              <span class="counter" aria-hidden="true">{{ exampleIndex + 1 }} / {{ type.examples.length }}</span>
            </div>
          </template>

          <ResultFields :fields="example.fields" />

          <div class="use">
            <button type="button" class="btn btn-primary" @click="open(true)">
              <Sparkles :size="18" /> Use this example
              <template v-if="locked"><Lock :size="16" aria-hidden="true" /><span class="sr-only">(Pro plan)</span></template>
            </button>
            <button type="button" class="btn btn-secondary" @click="open(false)">
              Try this generator
              <template v-if="locked"><Lock :size="16" aria-hidden="true" /><span class="sr-only">(Pro plan)</span></template>
            </button>
          </div>

          <template #footer-end>
            <span>{{ generator.label }} generator · {{ generator.cost }} tokens</span>
          </template>
        </ResultCard>
        <p class="sr-only" aria-live="polite">
          {{ type.label }} example {{ exampleIndex + 1 }} of {{ type.examples.length }}: {{ example.name }}
        </p>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* Same column as the page content; a divider line separates it from what follows. */
.showcase { padding: 0 24px; }
.inner {
  max-width: 1132px; margin: 0 auto; padding: 72px 0;
  display: flex; flex-direction: column; gap: 28px;
  border-bottom: 1px solid var(--divider);
}
.intro { max-width: 640px; }
.intro h2 { margin-top: 10px; font-size: clamp(28px, 3vw, 36px); line-height: 1.15; letter-spacing: -0.02em; }
.sub { margin-top: 12px; font-size: 17px; line-height: 1.55; color: var(--text-muted); }

/* Type pills: tabs that wrap on small screens. */
.types { display: flex; flex-wrap: wrap; gap: 8px; }
.type {
  min-height: 44px; padding: 0 18px;
  border: 1px solid var(--border-strong); border-radius: 999px; background: transparent;
  font: inherit; font-size: 14px; font-weight: 500; color: var(--text-muted); cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}
.type:hover { color: var(--text); border-color: var(--border-hover); }
.type[aria-selected='true'] { border-color: var(--accent-text); color: var(--accent-text); background: var(--surface); }

.another { display: flex; align-items: center; gap: 12px; }
.counter { font-size: 13px; color: var(--text-subtle); font-variant-numeric: tabular-nums; }

.use {
  display: flex; flex-wrap: wrap; gap: 12px;
  margin-top: 32px; padding-top: 28px; border-top: 1px solid var(--divider);
}

@media (max-width: 640px) {
  .showcase { padding: 0 16px; }
  .inner { padding: 48px 0; gap: 24px; }
  .sub { font-size: 16px; }
  .use .btn { flex: 1 1 100%; }
}
</style>

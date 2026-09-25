<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Bookmark, Check, Dices, Lock, MessageSquare, Sparkles, X } from 'lucide-vue-next'
import { campaignGeneratorApi } from '../../services/api'
import { useSession } from '../../stores/session'
import JsonView from '../../components/JsonView.vue'
import GenerationError from '../../components/GenerationError.vue'
import { gameSystems, limits, randomIdea, range } from '../../data/campaignOptions'

/**
 * The campaign generator. Step 1 (optional, free): the AI interviews the DM
 * about their idea. Step 2: the idea plus the answers become a campaign or
 * one-shot, saved as structured Campaign Memory.
 */
const props = defineProps({ generator: { type: Object, required: true } })
const { isFree, refreshBalance } = useSession()
const router = useRouter()

const oneShot = computed(() => props.generator.slug === 'one-shot')
const step = ref('idea') // idea -> questions -> result
const busy = ref(false)
const error = ref(null)
const interview = ref(null)
const answers = reactive({})
const result = ref(null)

/** A fresh example idea every time the page opens (the page is rebuilt per mode). */
const ideaPlaceholder = randomIdea(oneShot.value)
const lengthOptions = range(oneShot.value ? limits.oneShotHours : limits.campaignSessions)

const form = reactive({
  idea: '',
  system: gameSystems[0],
  tone: '',
  themes: '',
  playerCount: 4,
  startingLevel: oneShot.value ? 3 : 1,
  /** Hours for a one-shot, sessions for a campaign. */
  length: oneShot.value ? 4 : 12,
  additionalNotes: '',
  locationCount: 5,
  factionCount: 3,
  npcCount: 6,
  questCount: 4,
})

const generatingIdea = ref(false)
/** Every idea this user generated before (campaign and one-shot) — they cost tokens, so they are kept. */
const savedIdeas = ref([])
const showSaved = ref(false)
const IDEA_COST = 5

async function loadSavedIdeas() {
  try {
    savedIdeas.value = await campaignGeneratorApi.savedIdeas()
  } catch {
    savedIdeas.value = []
  }
}

/**
 * Fills the form from a generated or saved idea: the idea plus its tone, themes and length.
 * The length is only copied for the same kind — a campaign's sessions are not a one-shot's hours.
 */
function applyIdea(idea) {
  form.idea = idea.idea
  if (idea.tone) form.tone = idea.tone
  if (idea.themes) form.themes = idea.themes
  const sameKind = idea.kind === (oneShot.value ? 'ONE_SHOT' : 'CAMPAIGN')
  if (sameKind && idea.length && lengthOptions.includes(idea.length)) form.length = idea.length
}

function useSavedIdea(saved) {
  applyIdea(saved)
  showSaved.value = false
}

async function removeSavedIdea(saved) {
  await campaignGeneratorApi.deleteIdea(saved.id)
  savedIdeas.value = savedIdeas.value.filter((i) => i.id !== saved.id)
}

onMounted(loadSavedIdeas)

/** Drops one of the built-in example ideas into the box (no AI, instant). */
function useExampleIdea() {
  let next = randomIdea(oneShot.value)
  while (next === form.idea) next = randomIdea(oneShot.value)
  form.idea = next
}

/** Asks the AI for a fresh idea that fits the choices already made, and keeps it on the account. */
async function generateIdea() {
  error.value = null
  generatingIdea.value = true
  try {
    // The length is left to the AI so it can fit the idea; tone and themes are kept if already chosen.
    const { expectedLength, ...preferences } = requestBody()
    const saved = await campaignGeneratorApi.idea({ ...preferences, oneShot: oneShot.value })
    applyIdea(saved)
    savedIdeas.value = [saved, ...savedIdeas.value]
  } catch (e) {
    error.value = e
  } finally {
    generatingIdea.value = false
    refreshBalance()
  }
}

/** What the backend receives: the form plus the length written out, e.g. "4 hours". */
function requestBody() {
  const { length, ...rest } = form
  const unit = oneShot.value ? 'hour' : 'session'
  return { ...rest, expectedLength: `${length} ${unit}${length === 1 ? '' : 's'}` }
}

function switchMode(slug) {
  if (slug !== props.generator.slug) router.push(`/generators/${slug}`)
}

async function askQuestions() {
  error.value = null
  busy.value = true
  try {
    interview.value = await campaignGeneratorApi.interview({ ...requestBody(), oneShot: oneShot.value, questionCount: oneShot.value ? 4 : 6 })
    interview.value.questions.forEach((q) => { answers[q.id] = '' })
    step.value = 'questions'
  } catch (e) {
    error.value = e
  } finally {
    busy.value = false
  }
}

async function generate() {
  error.value = null
  busy.value = true
  try {
    const body = {
      ...requestBody(),
      answers: (interview.value?.questions || [])
        .filter((q) => answers[q.id]?.trim())
        .map((q) => ({ questionId: q.id, question: q.question, answer: answers[q.id] })),
    }
    result.value = oneShot.value ? await campaignGeneratorApi.oneShot(body) : await campaignGeneratorApi.generate(body)
    step.value = 'result'
  } catch (e) {
    error.value = e
  } finally {
    busy.value = false
    refreshBalance()
  }
}

const createdSummary = computed(() => {
  const c = result.value?.created
  if (!c) return []
  return [
    ['Locations', c.locations], ['Factions', c.factions], ['NPCs', c.npcs], ['Quests', c.quests], ['Sessions', c.sessions],
  ].filter(([, n]) => n > 0)
})
</script>

<template>
  <div class="stack">
    <div class="segmented mode">
      <button :class="{ active: !oneShot }" :disabled="isFree" @click="switchMode('campaign')">
        <Lock v-if="isFree" :size="12" /> Full campaign
      </button>
      <button :class="{ active: oneShot }" @click="switchMode('one-shot')">One-shot</button>
    </div>

    <ol class="steps">
      <li :class="{ active: step === 'idea', done: step !== 'idea' }">1. Your idea</li>
      <li :class="{ active: step === 'questions', done: step === 'result' }">2. Questions <span class="muted">(optional)</span></li>
      <li :class="{ active: step === 'result' }">3. Your {{ oneShot ? 'one-shot' : 'campaign' }}</li>
    </ol>

    <!-- Step 1: the idea -->
    <form v-if="step === 'idea'" class="card stack" @submit.prevent="askQuestions">
      <div class="field">
        <label for="w-idea">Your idea</label>
        <textarea
          id="w-idea" v-model="form.idea" class="textarea"
          :placeholder="`e.g. ${ideaPlaceholder}`"
        />
        <span class="hint">A sentence is enough. It can be vague — the questions in the next step fill in the rest.</span>
        <!-- Always the same buttons in the same place, whichever mode and however many saved ideas. -->
        <div class="idea-actions">
            <button type="button" class="btn btn-ghost btn-sm" :disabled="generatingIdea" @click="useExampleIdea">
              <Dices :size="14" /> Use example
            </button>
            <button type="button" class="btn btn-ghost btn-sm" :disabled="!savedIdeas.length" @click="showSaved = !showSaved">
              <Bookmark :size="14" /> Saved ideas ({{ savedIdeas.length }})
            </button>
            <button type="button" class="btn btn-ai btn-sm" :disabled="generatingIdea" @click="generateIdea">
              <span v-if="generatingIdea" class="spinner" /><Sparkles v-else :size="14" />
              {{ generatingIdea ? 'Thinking…' : `Generate idea · ${IDEA_COST} tokens` }}
            </button>
        </div>
        <ul v-if="showSaved && savedIdeas.length" class="saved-ideas">
          <li v-for="saved in savedIdeas" :key="saved.id">
            <button type="button" class="saved-use" :class="{ active: form.idea === saved.idea }" @click="useSavedIdea(saved)">
              <span class="saved-meta">
                <span class="badge" :class="saved.kind === 'ONE_SHOT' ? 'badge-red' : 'badge-blue'">
                  {{ saved.kind === 'ONE_SHOT' ? 'One-shot' : 'Campaign' }}
                </span>
                <span v-if="saved.tone" class="muted small">{{ saved.tone }}</span>
              </span>
              {{ saved.idea }}
            </button>
            <button type="button" class="btn btn-ghost btn-sm" title="Remove this idea" aria-label="Remove this idea" @click="removeSavedIdea(saved)">
              <X :size="14" />
            </button>
          </li>
        </ul>
      </div>
      <div class="grid grid-3">
        <div class="field"><label for="w-tone">Tone</label><input id="w-tone" v-model="form.tone" class="input" placeholder="dark, heroic, comedic…" /></div>
        <div class="field"><label for="w-themes">Themes</label><input id="w-themes" v-model="form.themes" class="input" placeholder="betrayal, forbidden magic" /></div>
        <div class="field">
          <label for="w-system">Game system</label>
          <select id="w-system" v-model="form.system" class="select">
            <option v-for="s in gameSystems" :key="s" :value="s">{{ s }}</option>
          </select>
        </div>
        <div class="field">
          <label for="w-players">Players</label>
          <select id="w-players" v-model.number="form.playerCount" class="select">
            <option v-for="n in range(limits.players)" :key="n" :value="n">{{ n }} {{ n === 1 ? 'player' : 'players' }}</option>
          </select>
        </div>
        <div class="field">
          <label for="w-level">Starting level</label>
          <select id="w-level" v-model.number="form.startingLevel" class="select">
            <option v-for="n in range(limits.startingLevel)" :key="n" :value="n">Level {{ n }}</option>
          </select>
        </div>
        <div class="field">
          <label for="w-length">{{ oneShot ? 'Play time' : 'Expected length' }}</label>
          <select id="w-length" v-model.number="form.length" class="select">
            <option v-for="n in lengthOptions" :key="n" :value="n">
              {{ n }} {{ oneShot ? (n === 1 ? 'hour' : 'hours') : (n === 1 ? 'session' : 'sessions') }}
            </option>
          </select>
        </div>
      </div>

      <details v-if="!oneShot" class="sizes">
        <summary>World size</summary>
        <div class="grid grid-4">
          <div class="field"><label>Locations</label><input v-model.number="form.locationCount" type="number" min="1" max="12" class="input" /></div>
          <div class="field"><label>Factions</label><input v-model.number="form.factionCount" type="number" min="0" max="8" class="input" /></div>
          <div class="field"><label>NPCs</label><input v-model.number="form.npcCount" type="number" min="1" max="15" class="input" /></div>
          <div class="field"><label>Quests</label><input v-model.number="form.questCount" type="number" min="1" max="10" class="input" /></div>
        </div>
      </details>

      <div class="row-between actions">
        <button type="button" class="btn btn-ghost" :disabled="busy" @click="generate">Skip questions, generate now</button>
        <button class="btn btn-primary" :disabled="busy">
          <span v-if="busy" class="spinner" /><MessageSquare v-else :size="16" />
          Ask me questions <span class="free">free</span>
        </button>
      </div>
    </form>

    <!-- Step 2: the interview -->
    <div v-else-if="step === 'questions'" class="stack">
      <div class="card understanding">
        <div class="stat-label">How the AI understood your idea</div>
        <p>{{ interview.understanding }}</p>
      </div>
      <div v-for="q in interview.questions" :key="q.id" class="card stack question">
        <div>
          <h3>{{ q.question }}</h3>
          <p class="muted small">{{ q.why }}</p>
        </div>
        <div class="row suggestions">
          <button
            v-for="s in q.suggestedAnswers" :key="s" type="button" class="chip"
            :class="{ active: answers[q.id] === s }" @click="answers[q.id] = s"
          >{{ s }}</button>
        </div>
        <input v-model="answers[q.id]" class="input" placeholder="Or write your own answer…" />
      </div>
      <div class="field">
        <label for="w-notes">Anything else?</label>
        <textarea id="w-notes" v-model="form.additionalNotes" class="textarea" placeholder="Names you want to use, content to avoid, a scene you already have in mind…" />
      </div>
      <div class="row-between">
        <button class="btn btn-ghost" @click="step = 'idea'"><ArrowLeft :size="16" /> Back</button>
        <button class="btn btn-primary" :disabled="busy" @click="generate">
          <span v-if="busy" class="spinner" /><Sparkles v-else :size="16" />
          Generate {{ oneShot ? 'one-shot' : 'campaign' }} · {{ generator.cost }} tokens
        </button>
      </div>
    </div>

    <!-- Step 3: the result -->
    <div v-else-if="step === 'result' && result" class="stack">
      <div class="card created">
        <div class="row">
          <span class="check-icon"><Check :size="18" /></span>
          <div>
            <h2>{{ result.campaign.name }}</h2>
            <p class="muted">{{ result.campaign.description }}</p>
          </div>
        </div>
        <div class="row counts">
          <span v-for="[label, n] in createdSummary" :key="label" class="badge badge-blue">{{ n }} {{ label }}</span>
        </div>
        <RouterLink :to="`/campaigns/${result.campaign.id}`" class="btn btn-primary">
          Open {{ oneShot ? 'one-shot' : 'campaign' }} <ArrowRight :size="16" />
        </RouterLink>
      </div>
      <div class="card">
        <h2>{{ oneShot ? 'Adventure plan' : 'Session 1 plan' }}</h2>
        <JsonView :value="result.firstSessionPlan" />
      </div>
    </div>

    <GenerationError v-if="error" :error="error" />
  </div>
</template>

<style scoped>
.mode { align-self: flex-start; }
.idea-actions { display: flex; justify-content: flex-end; align-items: center; gap: 8px; flex-wrap: wrap; }
.saved-meta { display: flex; gap: 8px; align-items: center; margin-bottom: 4px; }
.saved-ideas {
  list-style: none; margin: 4px 0 0; padding: 6px; display: grid; gap: 4px;
  border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--surface-sunken);
  max-height: 240px; overflow-y: auto;
}
.saved-ideas li { display: flex; gap: 6px; align-items: flex-start; }
.saved-use {
  flex: 1; text-align: left; border: 1px solid transparent; background: none; font: inherit;
  color: var(--text); padding: 7px 10px; border-radius: var(--radius-sm); cursor: pointer; line-height: 1.45;
}
.saved-use:hover { background: var(--surface); border-color: var(--border); }
.saved-use.active { background: var(--primary-soft); border-color: var(--primary-border); color: var(--primary-text); }
.btn-ai {
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: var(--on-color);
}
.btn-ai:hover:not(:disabled) { filter: brightness(1.08); color: var(--on-color); }
.steps { display: flex; gap: 8px; list-style: none; padding: 0; margin: 0; flex-wrap: wrap; }
.steps li { font-size: 13px; font-weight: 560; color: var(--text-muted); padding: 6px 12px; border-radius: 999px; background: var(--surface-muted); }
.steps li.active { background: var(--strong-bg); color: var(--on-color); }
.steps li.active .muted { color: rgba(255, 255, 255, 0.7); }
.steps li.done { background: var(--primary-soft-strong); color: var(--primary-text); }
.sizes summary { cursor: pointer; font-weight: 560; color: var(--text-secondary); margin-bottom: 12px; }
.actions { flex-wrap: wrap; }
.free { font-size: 11px; background: rgba(255, 255, 255, 0.2); padding: 1px 6px; border-radius: 999px; }
.understanding { border-left: 3px solid var(--primary); }
.understanding p { margin-top: 6px; }
.question h3 { margin: 0 0 2px; }
.suggestions { flex-wrap: wrap; }
.chip {
  border: 1px solid var(--border-strong); background: var(--surface); border-radius: 999px; padding: 5px 12px;
  font: inherit; font-size: 13px; cursor: pointer; color: var(--text-secondary);
}
.chip:hover { border-color: var(--primary-border); color: var(--primary-text); }
.chip.active { background: var(--primary); border-color: var(--primary); color: var(--on-color); }
.created { display: flex; flex-direction: column; gap: 14px; align-items: flex-start; border-top: 3px solid var(--primary); }
.created h2 { margin: 0; }
.counts { flex-wrap: wrap; }
.check-icon {
  width: 36px; height: 36px; border-radius: 50%; display: grid; place-items: center; flex-shrink: 0;
  background: var(--success-soft); color: var(--success-text);
}
</style>

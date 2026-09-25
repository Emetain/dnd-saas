<script setup>
import { computed, reactive, ref } from 'vue'
import { CalendarDays, ChevronDown, ClipboardPen, Lock, Sparkles } from 'lucide-vue-next'
import AppModal from './AppModal.vue'
import MarkdownView from './MarkdownView.vue'
import GenerationError from './GenerationError.vue'
import { sessionApi } from '../services/api'
import { useSession } from '../stores/session'

/**
 * The sessions of a campaign: each session's plan, what actually happened
 * (the debrief), and generating the next session from it.
 */
const props = defineProps({ memory: { type: Object, required: true } })
const emit = defineEmits(['changed'])
const { isFree, refreshBalance } = useSession()

const NEXT_SESSION_COST = 25

/** The set questions asked after every session. Only the first is required. */
const questions = [
  { key: 'whatHappened', label: 'What happened this session?', hint: 'The main events, in a few sentences or bullet points.', required: true },
  { key: 'questProgress', label: 'Which quests moved forward, were completed or abandoned?' },
  { key: 'npcs', label: 'Which NPCs did the party meet, and how did it go?', hint: 'Who did they help, anger, trust or kill?' },
  { key: 'decisions', label: 'What important choices did the players make?' },
  { key: 'endedAt', label: 'Where and how did the session end?', hint: 'The next session picks up exactly here.' },
  { key: 'looseThreads', label: 'Which loose threads, theories or plans are still open?' },
  { key: 'partyChanges', label: 'Did the party change?', hint: 'Level-ups, important items, deaths, new members.' },
  { key: 'nextSessionWishes', label: 'Anything you want in the next session?', hint: 'A scene, an NPC to bring back, a tone.' },
]

const isCampaign = computed(() => props.memory.campaign.kind === 'CAMPAIGN')
/** Newest first — the session you are about to run or debrief is on top. */
const sessions = computed(() => [...props.memory.sessions].sort((a, b) => b.sessionNumber - a.sessionNumber))
const latest = computed(() => sessions.value[0])
const needsDebrief = computed(() => latest.value && !latest.value.debrief)

const openPlans = ref(new Set())
const debriefing = ref(null)
const answers = reactive({})
const saving = ref(false)
const generating = ref(false)
const error = ref(null)

function isPlanOpen(session) {
  return openPlans.value.has(session.id) || (session.id === latest.value?.id && !session.debrief)
}
function togglePlan(session) {
  const next = new Set(openPlans.value)
  next.has(session.id) ? next.delete(session.id) : next.add(session.id)
  openPlans.value = next
}

function openDebrief(session) {
  questions.forEach((q) => { answers[q.key] = session.debrief?.[q.key] ?? '' })
  debriefing.value = session
}

async function saveDebrief() {
  saving.value = true
  error.value = null
  try {
    await sessionApi.saveDebrief(debriefing.value.id, { ...answers })
    debriefing.value = null
    emit('changed')
  } catch (e) {
    error.value = e
  } finally {
    saving.value = false
  }
}

async function generateNext() {
  generating.value = true
  error.value = null
  try {
    await sessionApi.next(props.memory.campaign.id)
    emit('changed')
  } catch (e) {
    error.value = e
  } finally {
    generating.value = false
    refreshBalance()
  }
}
</script>

<template>
  <div class="stack">
    <div v-if="isCampaign" class="card next">
      <div>
        <h3>Next session</h3>
        <p class="muted small">
          Builds on your campaign's idea, everything it remembers, and your notes on what happened last time.
        </p>
        <p v-if="needsDebrief" class="muted small next-hint">
          First add notes about <strong>Session {{ latest.sessionNumber }}</strong> — the next session continues from them.
        </p>
      </div>
      <button v-if="isFree" class="btn btn-secondary" disabled><Lock :size="14" /> Pro</button>
      <button v-else class="btn btn-ai" :disabled="generating || needsDebrief" @click="generateNext">
        <span v-if="generating" class="spinner" /><Sparkles v-else :size="16" />
        {{ generating ? 'Planning the next session…' : `Generate Session ${(latest?.sessionNumber ?? 0) + 1} · ${NEXT_SESSION_COST} tokens` }}
      </button>
    </div>

    <GenerationError v-if="error && !debriefing" :error="error" />

    <div v-if="!sessions.length" class="empty"><p>No sessions yet.</p></div>

    <article v-for="session in sessions" :key="session.id" class="card session">
      <div class="row-between">
        <div class="row">
          <span class="number">{{ session.sessionNumber }}</span>
          <div>
            <h3>{{ session.title }}</h3>
            <span v-if="session.date" class="muted small"><CalendarDays :size="12" /> {{ session.date }}</span>
          </div>
        </div>
        <span class="badge" :class="session.debrief ? 'badge-green' : 'badge-blue'">
          {{ session.debrief ? 'Played' : 'Planned' }}
        </span>
      </div>

      <div v-if="session.debrief" class="block happened">
        <div class="row-between block-head">
          <span class="block-title">After the session</span>
          <button class="btn btn-ghost btn-sm" @click="openDebrief(session)"><ClipboardPen :size="14" /> Edit notes</button>
        </div>
        <MarkdownView :source="session.summary" />
      </div>
      <div v-else-if="session.summary" class="block happened">
        <!-- Sessions created before debriefs existed only have a free-text summary. -->
        <span class="block-title">Summary</span>
        <MarkdownView :source="session.summary" />
      </div>
      <button v-if="!session.debrief" class="btn btn-primary add-notes" @click="openDebrief(session)">
        <ClipboardPen :size="16" /> Add notes on what happened
      </button>

      <div v-if="session.notes" class="block">
        <button class="plan-toggle" @click="togglePlan(session)">
          <ChevronDown :size="16" :class="{ turned: !isPlanOpen(session) }" />
          <span class="block-title">Session plan</span>
        </button>
        <MarkdownView v-if="isPlanOpen(session)" :source="session.notes" class="plan" />
      </div>
    </article>

    <AppModal
      v-if="debriefing"
      :title="`Session ${debriefing.sessionNumber}: what happened?`"
      subtitle="Your answers become this session's history — the AI reads them for every generation, and uses them to plan the next session."
      wide
      @close="debriefing = null"
    >
      <form class="stack" @submit.prevent="saveDebrief">
        <div v-for="q in questions" :key="q.key" class="field">
          <label :for="`d-${q.key}`">{{ q.label }} <span v-if="!q.required" class="muted">(optional)</span></label>
          <textarea :id="`d-${q.key}`" v-model="answers[q.key]" class="textarea short" :required="q.required" />
          <span v-if="q.hint" class="hint">{{ q.hint }}</span>
        </div>
        <GenerationError v-if="error" :error="error" />
        <div class="row actions">
          <button type="button" class="btn btn-ghost" @click="debriefing = null">Cancel</button>
          <button class="btn btn-primary" :disabled="saving">
            <span v-if="saving" class="spinner" /> Save notes
          </button>
        </div>
      </form>
    </AppModal>
  </div>
</template>

<style scoped>
.next { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; border-top: 3px solid var(--primary); }
.next h3 { margin: 0 0 4px; }
.next-hint { margin-top: 6px; color: var(--accent-text); }
.btn-ai { background: linear-gradient(135deg, var(--primary), var(--accent)); color: var(--on-color); }
.btn-ai:hover:not(:disabled) { filter: brightness(1.08); color: var(--on-color); }

.session { display: flex; flex-direction: column; gap: 14px; }
.session h3 { margin: 0; }
.number {
  width: 34px; height: 34px; border-radius: 50%; display: grid; place-items: center; flex-shrink: 0;
  background: var(--strong-bg); color: var(--on-color); font-weight: 700;
}
.block { border-top: 1px solid var(--border); padding-top: 12px; }
.block-head { margin-bottom: 4px; }
.block-title { font-size: 12px; font-weight: 650; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); }
.happened { background: var(--success-soft); border: none; border-radius: var(--radius-sm); padding: 12px 14px; }
.add-notes { align-self: flex-start; }
.plan-toggle { display: flex; align-items: center; gap: 6px; border: none; background: none; padding: 0; cursor: pointer; color: var(--text-muted); font: inherit; }
.plan-toggle svg { transition: transform 0.15s; }
.plan-toggle .turned { transform: rotate(-90deg); }
.plan { margin-top: 10px; }
.textarea.short { min-height: 64px; }
.actions { justify-content: flex-end; }
</style>

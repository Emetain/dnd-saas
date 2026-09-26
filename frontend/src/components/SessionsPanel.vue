<script setup>
import { computed, reactive, ref } from 'vue'
import { CalendarDays, CircleCheck, ChevronDown, ClipboardPen, Lock, RefreshCw, RotateCcw, Sparkles, Trash2 } from 'lucide-vue-next'
import AppModal from './AppModal.vue'
import MarkdownView from './MarkdownView.vue'
import GenerationError from './GenerationError.vue'
import GenerationOverlay from './GenerationOverlay.vue'
import { sessionApi } from '../services/api'
import { useSession } from '../stores/session'
import { useAccess } from '../stores/access'

/**
 * The sessions of a campaign: each session's plan, what actually happened
 * (the debrief), adding a session after the last one, and rewriting sessions
 * that have not been played yet.
 */
const props = defineProps({ memory: { type: Object, required: true } })
const emit = defineEmits(['changed'])
const { refreshBalance } = useSession()
const { hasTier, guard } = useAccess()

const NEXT_SESSION_COST = 25
const REWRITE_COST = 15

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
/** The last session is still only a plan, so a new one continues from that plan. */
const latestUnplayed = computed(() => latest.value && !latest.value.completed)

const openPlans = ref(new Set())
const debriefing = ref(null)
const answers = reactive({})
const saving = ref(false)
const generating = ref(false)
const error = ref(null)
const nextWishes = ref('')

/** The overlay while sessions are being written: finished and total AI calls. */
const progress = reactive({ done: 0, total: 0, detail: '', title: '' })

function isPlanOpen(session) {
  return openPlans.value.has(session.id) || (session.id === latest.value?.id && !session.completed)
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
  if (!guard('PRO', 'Session planning')) return
  const number = (latest.value?.sessionNumber ?? 0) + 1
  Object.assign(progress, { done: 0, total: 1, detail: '', title: `Writing Session ${number}…` })
  generating.value = true
  error.value = null
  try {
    await sessionApi.next(props.memory.campaign.id, nextWishes.value)
    nextWishes.value = ''
    emit('changed')
  } catch (e) {
    error.value = e
  } finally {
    generating.value = false
    refreshBalance()
  }
}

// --- Completing and deleting sessions ---

const changing = ref(null)

async function setCompleted(session, completed) {
  changing.value = session.id
  error.value = null
  try {
    await sessionApi.setCompleted(session.id, completed)
    emit('changed')
  } catch (e) {
    error.value = e
  } finally {
    changing.value = null
  }
}

async function removeSession(session) {
  const after = props.memory.sessions.some((s) => s.sessionNumber > session.sessionNumber)
  const note = after ? ' The sessions after it move up one number.' : ''
  if (!window.confirm(`Delete Session ${session.sessionNumber}, "${session.title}"?${note} This cannot be undone.`)) return
  changing.value = session.id
  error.value = null
  try {
    await sessionApi.remove(session.id)
    emit('changed')
  } catch (e) {
    error.value = e
  } finally {
    changing.value = null
  }
}

// --- Rewriting sessions that have not been played yet ---

const rewriting = ref(null)
const rewriteWishes = ref('')
const rewriteFollowing = ref(false)

/** Unplayed sessions after the one being rewritten, oldest first. */
const followingSessions = computed(() => {
  if (!rewriting.value) return []
  return props.memory.sessions
    .filter((s) => s.sessionNumber > rewriting.value.sessionNumber && !s.completed)
    .sort((a, b) => a.sessionNumber - b.sessionNumber)
})
const rewriteTargets = computed(() => (rewriting.value ? [rewriting.value, ...(rewriteFollowing.value ? followingSessions.value : [])] : []))

function openRewrite(session) {
  if (!guard('PRO', 'Rewriting sessions')) return
  rewriteWishes.value = ''
  rewriteFollowing.value = false
  rewriting.value = session
}

/** Rewrites the chosen sessions one after another, each building on the one before. */
async function rewrite() {
  const targets = rewriteTargets.value
  rewriting.value = null
  Object.assign(progress, { done: 0, total: targets.length, title: 'Rewriting your sessions…' })
  generating.value = true
  error.value = null
  try {
    for (const [index, session] of targets.entries()) {
      progress.detail = `Session ${session.sessionNumber} (${index + 1} of ${targets.length})`
      // Only the first session gets the DM's wishes; the ones after follow from it.
      await sessionApi.rewrite(session.id, index === 0 ? rewriteWishes.value : '')
      progress.done = index + 1
    }
  } catch (e) {
    error.value = e
  } finally {
    generating.value = false
    emit('changed')
    refreshBalance()
  }
}
</script>

<template>
  <div class="stack">
    <div v-if="isCampaign" class="card next">
      <div>
        <h3>Add a session</h3>
        <p class="muted small">
          Builds on your campaign's idea, everything it remembers, and your notes on what happened last time.
        </p>
        <p v-if="latestUnplayed" class="muted small next-hint">
          <strong>Session {{ latest.sessionNumber }}</strong> hasn't been played yet, so the new session continues from its plan —
          handy for making the campaign longer. Played it already? <a href="#" @click.prevent="openDebrief(latest)">Add your notes first</a>.
        </p>
        <input v-model="nextWishes" class="input wishes" placeholder="Anything you want in it? (optional)" />
      </div>
      <button class="btn btn-ai" :class="{ 'is-locked': !hasTier('PRO') }" :disabled="generating" @click="generateNext">
        <Lock v-if="!hasTier('PRO')" :size="14" /><Sparkles v-else :size="16" />
        Generate Session {{ (latest?.sessionNumber ?? 0) + 1 }} · {{ NEXT_SESSION_COST }} tokens
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
        <div class="row head-actions">
          <span class="badge" :class="session.completed ? 'badge-green' : 'badge-blue'">
            {{ session.completed ? 'Completed' : 'Planned' }}
          </span>
          <button
            class="btn btn-ghost btn-sm delete" :disabled="changing === session.id"
            :title="`Delete Session ${session.sessionNumber}`" :aria-label="`Delete Session ${session.sessionNumber}`"
            @click="removeSession(session)"
          >
            <Trash2 :size="14" />
          </button>
        </div>
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
      <div v-if="!session.debrief" class="row session-actions">
        <button class="btn btn-primary" @click="openDebrief(session)">
          <ClipboardPen :size="16" /> Add notes on what happened
        </button>
        <button v-if="!session.completed" class="btn btn-secondary" :disabled="changing === session.id" @click="setCompleted(session, true)">
          <CircleCheck :size="16" /> Mark as completed
        </button>
        <button v-else class="btn btn-ghost" :disabled="changing === session.id" @click="setCompleted(session, false)">
          <RotateCcw :size="16" /> Not played yet
        </button>
        <button
          v-if="isCampaign && !session.completed" class="btn btn-secondary" :class="{ 'is-locked': !hasTier('PRO') }"
          :disabled="generating" @click="openRewrite(session)"
        >
          <Lock v-if="!hasTier('PRO')" :size="14" /><RefreshCw v-else :size="16" /> Rewrite · {{ REWRITE_COST }} tokens
        </button>
      </div>

      <div v-if="session.notes" class="block">
        <button class="plan-toggle" @click="togglePlan(session)">
          <ChevronDown :size="16" :class="{ turned: !isPlanOpen(session) }" />
          <span class="block-title">Session plan</span>
        </button>
        <MarkdownView v-if="isPlanOpen(session)" :source="session.notes" class="plan" />
      </div>
    </article>

    <AppModal
      v-if="rewriting"
      :title="`Rewrite Session ${rewriting.sessionNumber}`"
      subtitle="A new plan for this session that fits everything before it. Sessions you have played are never changed."
      @close="rewriting = null"
    >
      <form class="stack" @submit.prevent="rewrite">
        <div class="field">
          <label for="r-wishes">What should change? <span class="muted">(optional)</span></label>
          <textarea
            id="r-wishes" v-model="rewriteWishes" class="textarea short"
            placeholder="The party spared the necromancer — make her an uneasy ally. Less combat, more intrigue."
          />
          <span class="hint">Tip: add notes to the sessions you played first — the rewrite builds on them.</span>
        </div>
        <label v-if="followingSessions.length" class="check">
          <input v-model="rewriteFollowing" type="checkbox" />
          Also rewrite the {{ followingSessions.length }} planned {{ followingSessions.length === 1 ? 'session' : 'sessions' }} after it,
          so the story stays connected
        </label>
        <div class="row actions">
          <button type="button" class="btn btn-ghost" @click="rewriting = null">Cancel</button>
          <button class="btn btn-ai">
            <RefreshCw :size="16" />
            Rewrite {{ rewriteTargets.length }} {{ rewriteTargets.length === 1 ? 'session' : 'sessions' }} · {{ rewriteTargets.length * REWRITE_COST }} tokens
          </button>
        </div>
      </form>
    </AppModal>

    <GenerationOverlay
      :active="generating"
      :failed="!!error"
      :expected-seconds="25"
      :title="progress.title"
      :steps-done="progress.done"
      :steps-total="progress.total"
      :detail="progress.detail"
      :idea="memory.campaign.description"
    />

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
.session-actions { flex-wrap: wrap; }
.head-actions { gap: 4px; flex-shrink: 0; }
.delete { color: var(--text-muted); }
.delete:hover:not(:disabled) { color: var(--accent-text); }
.wishes { margin-top: 10px; max-width: 460px; }
.check { display: flex; gap: 10px; align-items: flex-start; cursor: pointer; line-height: 1.45; }
.check input { margin-top: 3px; }
.plan-toggle { display: flex; align-items: center; gap: 6px; border: none; background: none; padding: 0; cursor: pointer; color: var(--text-muted); font: inherit; }
.plan-toggle svg { transition: transform 0.15s; }
.plan-toggle .turned { transform: rotate(-90deg); }
.plan { margin-top: 10px; }
.textarea.short { min-height: 64px; }
.actions { justify-content: flex-end; }
</style>

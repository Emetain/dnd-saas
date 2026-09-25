<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Check, RotateCcw, Save, Sparkles } from 'lucide-vue-next'
import { campaignApi, characterApi, generatorApi } from '../../services/api'
import { useSession } from '../../stores/session'
import JsonView from '../../components/JsonView.vue'
import GenerationError from '../../components/GenerationError.vue'

/**
 * Runs an in-campaign generator (NPC, Quest, Loot, ...) or a free-form
 * question. The campaign's memory is sent to the AI automatically.
 */
const props = defineProps({
  generator: { type: Object, required: true },
  /** Set when shown in a pop-up for one campaign: hides the picker and leaves the URL alone. */
  fixedCampaignId: { type: Number, default: null },
})
/** Fired after a generation, so a surrounding page can reload what it shows. */
const emit = defineEmits(['generated'])
const { refreshBalance } = useSession()
const route = useRoute()
const router = useRouter()

const campaigns = ref([])
const characters = ref([])
const loadingCampaigns = ref(true)
const running = ref(false)
const saving = ref(false)
const error = ref(null)
const result = ref(null)
/** Whether the shown result was generated as a preview (so it can still be saved). */
const ranAsPreview = ref(false)

const form = reactive({
  campaignId: props.fixedCampaignId ?? (route.query.campaign ? Number(route.query.campaign) : null),
  instruction: '',
  temperature: 0.8,
  difficulty: '',
  targetId: null,
  preview: false,
})

const isAsk = computed(() => props.generator.kind === 'ask')
const campaign = computed(() => campaigns.value.find((c) => c.id === form.campaignId))
const canRun = computed(() => form.campaignId && (!props.generator.needsCharacter || form.targetId))

async function loadCharacters() {
  characters.value = []
  form.targetId = null
  if (props.generator.needsCharacter && form.campaignId) {
    characters.value = await characterApi.listForCampaign(form.campaignId)
  }
}

watch(() => form.campaignId, (id) => {
  loadCharacters()
  if (!props.fixedCampaignId && id && Number(route.query.campaign) !== id) router.replace({ query: { ...route.query, campaign: id } })
})

async function run() {
  error.value = null
  result.value = null
  running.value = true
  ranAsPreview.value = !isAsk.value && form.preview
  try {
    const body = {
      instruction: form.instruction,
      temperature: form.temperature,
    }
    result.value = isAsk.value
      ? await generatorApi.ask(form.campaignId, body)
      : await generatorApi.run(form.campaignId, props.generator.type, {
          ...body,
          preview: form.preview,
          difficulty: form.difficulty,
          targetId: form.targetId,
        })
    emit('generated', result.value)
  } catch (e) {
    error.value = e
  } finally {
    running.value = false
    refreshBalance()
  }
}

async function save() {
  saving.value = true
  try {
    result.value = await generatorApi.commit(form.campaignId, props.generator.type, result.value.logId)
    emit('generated', result.value)
  } catch (e) {
    error.value = e
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    campaigns.value = await campaignApi.list()
    if (!form.campaignId && campaigns.value.length) form.campaignId = campaigns.value[0].id
    await loadCharacters()
  } finally {
    loadingCampaigns.value = false
  }
})
</script>

<template>
  <div class="stack">
    <div v-if="!loadingCampaigns && !campaigns.length" class="empty">
      <h3>You need a campaign or one-shot first</h3>
      <p>Generators write into a campaign's memory, so everything they create stays connected.</p>
      <RouterLink to="/generators/one-shot" class="btn btn-primary" style="margin-top: 16px">Create a one-shot</RouterLink>
    </div>

    <form v-else class="card stack" @submit.prevent="run">
      <div class="grid grid-2">
        <div v-if="!fixedCampaignId" class="field">
          <label for="g-campaign">Campaign</label>
          <select id="g-campaign" v-model="form.campaignId" class="select" :disabled="loadingCampaigns">
            <option v-for="c in campaigns" :key="c.id" :value="c.id">
              {{ c.name }} {{ c.kind === 'ONE_SHOT' ? '(one-shot)' : '' }}
            </option>
          </select>
          <span class="hint">Its NPCs, places and history are sent to the AI automatically.</span>
        </div>

        <div v-if="generator.needsCharacter" class="field">
          <label for="g-character">Player character</label>
          <select id="g-character" v-model="form.targetId" class="select" required>
            <option :value="null" disabled>Choose a character…</option>
            <option v-for="pc in characters" :key="pc.id" :value="pc.id">{{ pc.name }} — {{ pc.characterClass }}</option>
          </select>
          <span v-if="!characters.length" class="hint">
            No characters yet — <RouterLink :to="`/campaigns/${form.campaignId}`">add one to the party</RouterLink>.
          </span>
        </div>

        <div v-if="generator.hasDifficulty" class="field">
          <label for="g-difficulty">Difficulty</label>
          <select id="g-difficulty" v-model="form.difficulty" class="select">
            <option value="">Let the AI decide</option>
            <option value="EASY">Easy</option>
            <option value="MEDIUM">Medium</option>
            <option value="HARD">Hard</option>
            <option value="DEADLY">Deadly</option>
          </select>
        </div>
      </div>

      <div class="field">
        <label for="g-instruction">{{ isAsk ? 'Your question' : 'What do you want?' }}</label>
        <textarea id="g-instruction" v-model="form.instruction" class="textarea" :placeholder="generator.placeholder" />
        <span class="hint">Optional for generators — leave it empty and the AI picks something that fits.</span>
      </div>

      <div class="options">
        <div class="field creativity">
          <label for="g-temp">Creativity <span class="muted">{{ form.temperature.toFixed(1) }}</span></label>
          <input id="g-temp" v-model.number="form.temperature" type="range" min="0.2" max="1.2" step="0.1" />
        </div>
        <label v-if="!isAsk" class="check">
          <input v-model="form.preview" type="checkbox" /> Preview before saving
        </label>
      </div>

      <div class="row-between">
        <span class="muted small">Costs {{ generator.cost }} {{ generator.cost === 1 ? 'token' : 'tokens' }}</span>
        <button class="btn btn-primary" :disabled="running || !canRun">
          <span v-if="running" class="spinner" /><Sparkles v-else :size="16" />
          {{ running ? 'Generating…' : isAsk ? 'Ask' : `Generate ${generator.label}` }}
        </button>
      </div>
    </form>

    <GenerationError v-if="error" :error="error" />

    <div v-if="result" class="card result">
      <div class="row-between result-head">
        <div class="row">
          <h2>Result</h2>
        </div>
        <div class="row">
          <button class="btn btn-secondary btn-sm" :disabled="running" @click="run"><RotateCcw :size="14" /> Regenerate</button>
          <button v-if="ranAsPreview && !result.saved" class="btn btn-primary btn-sm" :disabled="saving" @click="save">
            <Save :size="14" /> Save to {{ campaign?.name }}
          </button>
        </div>
      </div>

      <div v-if="result.created?.length" class="alert alert-success saved">
        <Check :size="16" />
        <span>
          Saved to <RouterLink :to="`/campaigns/${form.campaignId}`">{{ campaign?.name }}</RouterLink>:
          {{ result.created.map((c) => c.name).join(', ') }}
        </span>
      </div>

      <JsonView :value="result.content" />
    </div>
  </div>
</template>

<style scoped>
.options { display: flex; align-items: flex-end; gap: 24px; flex-wrap: wrap; }
.creativity { width: 220px; }
.creativity input { accent-color: var(--primary); }
.check input { accent-color: var(--primary); }
.result-head { margin-bottom: 16px; }
.result-head h2 { margin: 0; }
.saved { margin-bottom: 16px; }
</style>

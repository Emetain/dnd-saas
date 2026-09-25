<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, Plus, Sparkles, X } from 'lucide-vue-next'
import { campaignApi } from '../services/api'
import { useSession } from '../stores/session'
import CampaignCard from '../components/CampaignCard.vue'
import { gameSystems } from '../data/campaignOptions'

const { isFree } = useSession()
const route = useRoute()
const router = useRouter()

const campaigns = ref([])
const loading = ref(true)
const filter = ref('ALL')
const showForm = ref(route.query.new === '1')
const saving = ref(false)
const error = ref('')
const form = reactive({ name: '', description: '', system: gameSystems[0], kind: 'ONE_SHOT' })

const visible = computed(() =>
  filter.value === 'ALL' ? campaigns.value: campaigns.value.filter((c) => c.kind === filter.value),
)

watch(() => route.query.new, (value) => { showForm.value = value === '1' })

async function load() {
  loading.value = true
  try {
    campaigns.value = await campaignApi.list()
  } finally {
    loading.value = false
  }
}

async function create() {
  error.value = ''
  saving.value = true
  try {
    const created = await campaignApi.create({ ...form })
    router.push(`/campaigns/${created.id}`)
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

function closeForm() {
  showForm.value = false
  if (route.query.new) router.replace('/campaigns')
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Campaigns</h1>
        <p>Every campaign and one-shot you run, with everything it remembers.</p>
      </div>
      <div class="row">
        <button class="btn btn-secondary" @click="showForm = true"><Plus :size="16" /> Start from scratch</button>
        <RouterLink to="/generators/one-shot" class="btn btn-primary"><Sparkles :size="16" /> Generate</RouterLink>
      </div>
    </div>

    <div v-if="showForm" class="card new-form">
      <div class="row-between">
        <h2>New from scratch</h2>
        <button class="btn btn-ghost btn-sm" aria-label="Close" @click="closeForm"><X :size="16" /></button>
      </div>
      <form class="stack" @submit.prevent="create">
        <div class="segmented">
          <button type="button" :class="{ active: form.kind === 'ONE_SHOT' }" @click="form.kind = 'ONE_SHOT'">One-shot</button>
          <button type="button" :class="{ active: form.kind === 'CAMPAIGN' }" :disabled="isFree" @click="form.kind = 'CAMPAIGN'">
            <Lock v-if="isFree" :size="12" /> Campaign
          </button>
        </div>
        <p v-if="isFree" class="muted small">
          Full campaigns are part of Pro. <RouterLink to="/billing#plans">Compare plans</RouterLink>
        </p>
        <div class="grid grid-2">
          <div class="field">
            <label for="c-name">Name</label>
            <input id="c-name" v-model="form.name" class="input" required placeholder="The Sunken Crown" />
          </div>
          <div class="field">
            <label for="c-system">Game system</label>
            <select id="c-system" v-model="form.system" class="select">
              <option v-for="s in gameSystems" :key="s" :value="s">{{ s }}</option>
            </select>
          </div>
        </div>
        <div class="field">
          <label for="c-desc">Pitch</label>
          <textarea id="c-desc" v-model="form.description" class="textarea" placeholder="Two or three sentences you could pitch to your players." />
        </div>
        <div v-if="error" class="alert alert-error">{{ error }}</div>
        <div class="row">
          <button class="btn btn-primary" :disabled="saving">Create</button>
          <button type="button" class="btn btn-ghost" @click="closeForm">Cancel</button>
        </div>
      </form>
    </div>

    <div class="segmented filter">
      <button :class="{ active: filter === 'ALL' }" @click="filter = 'ALL'">All</button>
      <button :class="{ active: filter === 'CAMPAIGN' }" @click="filter = 'CAMPAIGN'">Campaigns</button>
      <button :class="{ active: filter === 'ONE_SHOT' }" @click="filter = 'ONE_SHOT'">One-shots</button>
    </div>

    <div v-if="loading" class="muted">Loading…</div>
    <div v-else-if="!visible.length" class="empty">
      <h3>Nothing here yet</h3>
      <p>Generate a one-shot or start from scratch.</p>
    </div>
    <div v-else class="grid grid-3">
      <CampaignCard v-for="campaign in visible" :key="campaign.id" :campaign="campaign" />
    </div>
  </div>
</template>

<style scoped>
.new-form { margin-bottom: 24px; }
.new-form .segmented { align-self: flex-start; }
.filter { margin-bottom: 16px; }
</style>

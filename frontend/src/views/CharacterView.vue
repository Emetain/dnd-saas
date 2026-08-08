<script setup>
import { ref, onMounted, watch } from 'vue'
import { campaignApi, characterApi } from '../services/api'
import CharacterForm from '../components/CharacterForm.vue'
import CharacterCard from '../components/CharacterCard.vue'

const campaigns = ref([])
const selectedCampaignId = ref(null)
const characters = ref([])
const error = ref(null)
const loading = ref(false)

async function loadCampaigns() {
  error.value = null
  try {
    campaigns.value = await campaignApi.list()
    if (campaigns.value.length && selectedCampaignId.value === null) {
      selectedCampaignId.value = campaigns.value[0].id
    }
  } catch (e) {
    error.value = e.message
  }
}

async function loadCharacters() {
  if (!selectedCampaignId.value) {
    characters.value = []
    return
  }
  loading.value = true
  error.value = null
  try {
    characters.value = await characterApi.listForCampaign(selectedCampaignId.value)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function handleCreate(newCharacter) {
  if (!selectedCampaignId.value) {
    error.value = 'Create a campaign first — characters belong to a campaign.'
    return
  }
  error.value = null
  try {
    const created = await characterApi.create(selectedCampaignId.value, newCharacter)
    characters.value.push(created)
  } catch (e) {
    error.value = e.message
  }
}

watch(selectedCampaignId, loadCharacters)

onMounted(async () => {
  await loadCampaigns()
  await loadCharacters()
})
</script>

<template>
  <section>
    <div class="card">
      <label>
        Campaign
        <select v-model="selectedCampaignId" :disabled="!campaigns.length">
          <option v-for="campaign in campaigns" :key="campaign.id" :value="campaign.id">
            {{ campaign.name }}
          </option>
        </select>
      </label>
      <p v-if="!campaigns.length" style="color: var(--color-muted)">
        No campaigns yet. Create one via the API, then reload this page.
      </p>
    </div>

    <CharacterForm @create="handleCreate" />

    <p v-if="error" class="card" style="color: var(--color-accent)">
      {{ error }}
    </p>
    <p v-if="loading">Loading…</p>

    <div v-for="character in characters" :key="character.id">
      <CharacterCard :character="character" />
    </div>
  </section>
</template>

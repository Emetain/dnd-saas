<script setup>
import { ref, onMounted } from 'vue'
import { characterApi } from '../services/api'
import CharacterForm from '../components/CharacterForm.vue'
import CharacterCard from '../components/CharacterCard.vue'

const characters = ref([])
const error = ref(null)
const loading = ref(false)

async function loadCharacters() {
  loading.value = true
  error.value = null
  try {
    characters.value = await characterApi.list()
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function handleCreate(newCharacter) {
  error.value = null
  try {
    const created = await characterApi.create(newCharacter)
    characters.value.push(created)
  } catch (e) {
    error.value = e.message
  }
}

onMounted(loadCharacters)
</script>

<template>
  <section>
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


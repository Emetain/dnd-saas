<script setup>
import { reactive } from 'vue'

const emit = defineEmits(['create'])

const abilities = ['strength', 'dexterity', 'constitution', 'intelligence', 'wisdom', 'charisma']

const form = reactive({
  name: '',
  characterClass: 'Fighter',
  level: 1,
  strength: 10,
  dexterity: 10,
  constitution: 10,
  intelligence: 10,
  wisdom: 10,
  charisma: 10,
})

function submit() {
  emit('create', { ...form })
}
</script>

<template>
  <form class="card" @submit.prevent="submit">
    <h2>New Character</h2>

    <label>
      Name
      <input v-model="form.name" required placeholder="Aria the Bold" />
    </label>

    <label>
      Class
      <select v-model="form.characterClass">
        <option>Fighter</option>
        <option>Wizard</option>
        <option>Rogue</option>
        <option>Cleric</option>
        <option>Bard</option>
      </select>
    </label>

    <label>
      Level
      <input v-model.number="form.level" type="number" min="1" max="20" />
    </label>

    <label v-for="ability in abilities" :key="ability" style="text-transform: capitalize">
      {{ ability }}
      <input v-model.number="form[ability]" type="number" min="1" max="30" />
    </label>

    <button type="submit">Create</button>
  </form>
</template>


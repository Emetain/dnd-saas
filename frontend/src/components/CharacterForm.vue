<script setup>
import { reactive } from 'vue'

const emit = defineEmits(['create', 'cancel'])

const abilities = ['strength', 'dexterity', 'constitution', 'intelligence', 'wisdom', 'charisma']

const form = reactive({
  name: '',
  playerName: '',
  characterClass: 'Fighter',
  race: '',
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
  <form class="card stack" @submit.prevent="submit">
    <h3>New player character</h3>
    <div class="grid grid-4">
      <div class="field">
        <label for="pc-name">Name</label>
        <input id="pc-name" v-model="form.name" class="input" required placeholder="Aria the Bold" />
      </div>
      <div class="field">
        <label for="pc-player">Player</label>
        <input id="pc-player" v-model="form.playerName" class="input" placeholder="Sam" />
      </div>
      <div class="field">
        <label for="pc-class">Class</label>
        <select id="pc-class" v-model="form.characterClass" class="select">
          <option>Barbarian</option><option>Bard</option><option>Cleric</option><option>Druid</option>
          <option>Fighter</option><option>Monk</option><option>Paladin</option><option>Ranger</option>
          <option>Rogue</option><option>Sorcerer</option><option>Warlock</option><option>Wizard</option>
        </select>
      </div>
      <div class="field">
        <label for="pc-level">Level</label>
        <input id="pc-level" v-model.number="form.level" class="input" type="number" min="1" max="20" />
      </div>
    </div>
    <div class="abilities">
      <div v-for="ability in abilities" :key="ability" class="field">
        <label :for="`pc-${ability}`" class="ability">{{ ability.slice(0, 3) }}</label>
        <input :id="`pc-${ability}`" v-model.number="form[ability]" class="input" type="number" min="1" max="30" />
      </div>
    </div>
    <div class="row">
      <button type="submit" class="btn btn-primary">Add character</button>
      <button type="button" class="btn btn-ghost" @click="emit('cancel')">Cancel</button>
    </div>
  </form>
</template>

<style scoped>
.abilities { display: grid; grid-template-columns: repeat(6, 1fr); gap: 8px; }
.ability { text-transform: uppercase; letter-spacing: 0.04em; }
@media (max-width: 640px) { .abilities { grid-template-columns: repeat(3, 1fr); } }
</style>

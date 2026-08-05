<script setup>
defineProps({
  character: {
    type: Object,
    required: true,
  },
})

function formatModifier(value) {
  return value >= 0 ? `+${value}` : `${value}`
}
</script>

<template>
  <article class="card">
    <h3>{{ character.name }}</h3>
    <p>
      Level {{ character.level }} {{ character.characterClass }} ·
      Proficiency {{ formatModifier(character.proficiencyBonus) }}
    </p>
    <table>
      <thead>
        <tr>
          <th v-for="(score, ability) in character.abilityScores" :key="ability"
              style="text-transform: capitalize; padding: 0 0.5rem;">
            {{ ability.slice(0, 3) }}
          </th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td v-for="(score, ability) in character.abilityScores" :key="ability"
              style="text-align: center;">
            {{ score }}
            <small style="color: var(--color-muted)">
              ({{ formatModifier(character.abilityModifiers[ability]) }})
            </small>
          </td>
        </tr>
      </tbody>
    </table>
  </article>
</template>


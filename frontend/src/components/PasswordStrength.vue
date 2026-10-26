<script setup>
import { computed } from 'vue'
import { Check, X } from 'lucide-vue-next'
import { passwordRules, rulesMet } from '../utils/password'

/**
 * Strength bar plus the rules checklist under a new-password field.
 * Empty without a password; red for 0–1 rules met, orange for 2–3, green for all 4.
 */
const props = defineProps({
  password: { type: String, default: '' },
  /** Id of the element, for the input's aria-describedby. */
  id: { type: String, default: 'password-strength' },
})

const met = computed(() => rulesMet(props.password))
const level = computed(() => {
  if (!props.password) return 'empty'
  if (met.value === passwordRules.length) return 'strong'
  return met.value >= 2 ? 'medium' : 'weak'
})
const label = { empty: '', weak: 'Weak', medium: 'Almost there', strong: 'Strong' }
</script>

<template>
  <div :id="id" class="strength" :class="level">
    <div class="bar-row">
      <div
        class="bar" role="meter" aria-label="Password strength"
        :aria-valuenow="met" aria-valuemin="0" :aria-valuemax="passwordRules.length" :aria-valuetext="label[level] || 'No password yet'"
      >
        <div class="fill" :style="{ width: `${password ? (100 * met) / passwordRules.length : 0}%` }" />
      </div>
      <span class="level-label">{{ label[level] }}</span>
    </div>
    <ul class="rules">
      <li v-for="rule in passwordRules" :key="rule.id" :class="{ ok: rule.test(password), wide: rule.chars }">
        <Check v-if="rule.test(password)" :size="13" /><X v-else :size="13" /> {{ rule.label }}
        <code v-if="rule.chars" class="chars" :aria-label="`one of ${rule.chars.split('').join(' ')}`">{{ rule.chars }}</code>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.strength { --level: var(--border-strong); display: grid; gap: 8px; margin-top: 2px; }
.strength.weak { --level: #d93a3a; }
.strength.medium { --level: #e8871e; }
.strength.strong { --level: #1f9d55; }
.bar-row { display: flex; align-items: center; gap: 10px; }
.bar { flex: 1; height: 6px; border-radius: 999px; background: var(--surface-muted); overflow: hidden; }
.fill { height: 100%; border-radius: inherit; background: var(--level); transition: width 0.2s ease, background-color 0.2s ease; }
.level-label { min-width: 84px; text-align: right; font-size: 12px; font-weight: 600; color: var(--level); }
.rules { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: 1fr 1fr; gap: 4px 12px; font-size: 12.5px; color: var(--text-muted); }
.rules li { display: flex; align-items: center; gap: 5px; }
.rules li.ok { color: var(--success-text); }
.rules li.wide { grid-column: 1 / -1; white-space: nowrap; }
.chars {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 12.5px; letter-spacing: 0.08em;
  background: var(--surface-muted); border-radius: 4px; padding: 1px 5px; color: var(--text-secondary);
}
@media (max-width: 480px) { .rules { grid-template-columns: 1fr; } }
@media (prefers-reduced-motion: reduce) { .fill { transition: none; } }
</style>

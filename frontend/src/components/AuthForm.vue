<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { Eye, EyeOff } from 'lucide-vue-next'
import PasswordStrength from './PasswordStrength.vue'
import { useSession } from '../stores/session'
import { isStrongPassword } from '../utils/password'

/**
 * Sign in or create an account. Used by the /login and /register pages and on
 * the welcome page. Emits `done` once signed in. The button only works once
 * every field is filled in (and, for a new account, the password is strong).
 */
const mode = defineModel('mode', { type: String, default: 'login' }) // 'login' | 'register'
const emit = defineEmits(['done'])
const { login, register } = useSession()

const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/
const form = reactive({ displayName: '', email: '', password: '' })
/** Only on registration: must match the password, nothing else. */
const repeat = ref('')
const showPassword = ref(false)
const error = ref('')
const busy = ref(false)

const isRegister = computed(() => mode.value === 'register')
const repeatMismatch = computed(() => repeat.value.length > 0 && repeat.value !== form.password)
const emailValid = computed(() => EMAIL.test(form.email.trim()))
const canSubmit = computed(() => {
  if (!emailValid.value || !form.password) return false
  if (!isRegister.value) return true
  return form.displayName.trim().length > 0 && isStrongPassword(form.password) && repeat.value === form.password
})

watch(mode, () => { error.value = '' })

async function submit() {
  if (!canSubmit.value) return
  error.value = ''
  busy.value = true
  try {
    if (isRegister.value) await register({ ...form })
    else await login(form.email, form.password)
    emit('done')
  } catch (e) {
    error.value = e.message
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <form class="stack auth-form" novalidate @submit.prevent="submit">
    <div v-if="isRegister" class="field">
      <label for="auth-name">Display name</label>
      <input
        id="auth-name" v-model="form.displayName" class="input" required maxlength="50"
        autocomplete="nickname" placeholder="Dungeon Master"
      />
    </div>
    <div class="field">
      <label for="auth-email">Email</label>
      <input
        id="auth-email" v-model="form.email" class="input" type="email" required
        autocomplete="email" placeholder="you@example.com"
      />
    </div>
    <div class="field">
      <div class="row-between">
        <label for="auth-password">Password</label>
        <RouterLink v-if="!isRegister" to="/forgot-password" class="small">Forgot password?</RouterLink>
      </div>
      <div class="password">
        <input
          id="auth-password" v-model="form.password" class="input" :type="showPassword ? 'text' : 'password'" required
          :autocomplete="isRegister ? 'new-password' : 'current-password'"
          :aria-describedby="isRegister ? 'auth-password-strength' : undefined"
        />
        <button
          type="button" class="reveal" :aria-label="showPassword ? 'Hide password' : 'Show password'"
          @click="showPassword = !showPassword"
        >
          <EyeOff v-if="showPassword" :size="16" /><Eye v-else :size="16" />
        </button>
      </div>
      <PasswordStrength v-if="isRegister" id="auth-password-strength" :password="form.password" />
    </div>
    <div v-if="isRegister" class="field">
      <label for="auth-repeat">Repeat password</label>
      <input
        id="auth-repeat" v-model="repeat" class="input" :type="showPassword ? 'text' : 'password'" required
        autocomplete="new-password" :aria-invalid="repeatMismatch" aria-describedby="auth-repeat-hint"
      />
      <span id="auth-repeat-hint" class="hint" :class="repeatMismatch ? 'mismatch' : repeat && 'match'" aria-live="polite">
        {{ repeatMismatch ? "The passwords don't match" : repeat ? 'The passwords match' : '' }}
      </span>
    </div>

    <div v-if="error" class="alert alert-error" role="alert">{{ error }}</div>

    <button class="btn btn-primary btn-block" :disabled="busy || !canSubmit">
      <span v-if="busy" class="spinner" />
      {{ isRegister ? 'Create free account' : 'Sign in' }}
    </button>

    <p class="switch muted small">
      <template v-if="isRegister">
        Already have an account? <a href="#" @click.prevent="mode = 'login'">Sign in</a>
      </template>
      <template v-else>
        New to Narrion? <a href="#" @click.prevent="mode = 'register'">Create a free account</a>
      </template>
    </p>
  </form>
</template>

<style scoped>
.password { position: relative; }
.password .input { width: 100%; padding-right: 42px; }
.reveal {
  position: absolute; right: 4px; top: 50%; transform: translateY(-50%);
  width: 34px; height: 34px; display: grid; place-items: center;
  border: none; background: none; border-radius: var(--radius-sm); color: var(--text-muted); cursor: pointer;
}
.reveal:hover { color: var(--text); background: var(--surface-muted); }
.switch { text-align: center; margin: 0; }
.hint.mismatch { color: var(--accent-text); }
.hint.match { color: var(--success-text); }
</style>

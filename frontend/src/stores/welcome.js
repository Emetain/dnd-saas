import { ref } from 'vue'

/**
 * Lets the banner and navbar open the account form on the welcome page:
 * "Register now" and "Sign in" switch its mode and scroll it into view.
 */
const mode = ref('register')

function openAccountForm(which) {
  mode.value = which
  document.getElementById('account-form')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

export function useWelcome() {
  return { mode, openAccountForm }
}

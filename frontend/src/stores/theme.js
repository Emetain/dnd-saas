import { ref } from 'vue'

/**
 * Light / dark / system theme.
 *
 * The choice is remembered per browser. "system" follows the OS setting and
 * updates live when it changes. The resolved theme is written to
 * <html data-theme="...">, which main.css reads.
 */
const STORAGE_KEY = 'narrion.theme'
const media = window.matchMedia('(prefers-color-scheme: dark)')

function readPreference() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return ['light', 'dark', 'system'].includes(stored) ? stored : 'system'
  } catch {
    return 'system'
  }
}

const preference = ref(readPreference())
const resolved = ref('light')

function apply() {
  resolved.value = preference.value === 'system' ? (media.matches ? 'dark' : 'light') : preference.value
  document.documentElement.dataset.theme = resolved.value
}

function setTheme(value) {
  preference.value = value
  try {
    localStorage.setItem(STORAGE_KEY, value)
  } catch {
    // Storage unavailable — the choice lasts until the page is reloaded.
  }
  apply()
}

/** Flips between light and dark, leaving "system" for an explicit choice. */
function toggleTheme() {
  setTheme(resolved.value === 'dark' ? 'light' : 'dark')
}

media.addEventListener('change', () => {
  if (preference.value === 'system') apply()
})
apply()

export function useTheme() {
  return { preference, resolved, setTheme, toggleTheme }
}

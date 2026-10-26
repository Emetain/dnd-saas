/**
 * The password rules, mirroring UserService.checkPasswordRules on the backend.
 */

/** Exactly these count as special characters (same list as UserService.SPECIAL_CHARACTERS). */
export const SPECIAL_CHARACTERS = '!@#$%^&*()-_=+[]{}?.,:;'

export const passwordRules = [
  { id: 'length', label: 'At least 8 characters', test: (p) => p.length >= 8 },
  { id: 'capital', label: 'A capital letter', test: (p) => /\p{Lu}/u.test(p) },
  { id: 'number', label: 'A number', test: (p) => /\d/.test(p) },
  { id: 'special', label: 'A special character:', chars: SPECIAL_CHARACTERS, test: (p) => [...p].some((c) => SPECIAL_CHARACTERS.includes(c)) },
]

/** How many rules the password meets (0-4). */
export function rulesMet(password) {
  return passwordRules.filter((rule) => rule.test(password)).length
}

export function isStrongPassword(password) {
  return rulesMet(password) === passwordRules.length
}

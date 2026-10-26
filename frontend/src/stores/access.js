import { reactive } from 'vue'
import { tierAtLeast } from '../data/plans'
import { useSession } from './session'

/**
 * What the signed-in user's plan unlocks, and the upgrade pop-up shown when
 * they reach for something it doesn't.
 *
 * Locked features stay visible (with a lock) instead of disabled: clicking one
 * calls `guard`, which opens the pop-up naming the plan that unlocks it.
 */
const upgrade = reactive({
  open: false,
  /** What they tried to use, e.g. "Full campaign". */
  feature: '',
  /** The lowest plan that includes it. */
  tier: 'PRO',
})

export function useAccess() {
  const { state } = useSession()

  const hasTier = (minimum) => tierAtLeast(state.user?.subscriptionTier, minimum)

  /** For generators from data/generators.js. */
  const isLocked = (generator) => !hasTier(generator.minTier)

  function showUpgrade(feature, tier = 'PRO') {
    upgrade.feature = feature
    upgrade.tier = tier
    upgrade.open = true
  }

  function closeUpgrade() {
    upgrade.open = false
  }

  /** True when the plan allows it; otherwise opens the upgrade pop-up and returns false. */
  function guard(minimum, feature) {
    if (hasTier(minimum)) return true
    showUpgrade(feature, minimum)
    return false
  }

  /** Click handler for links to generators: stops the navigation and explains when locked. */
  function guardGenerator(event, generator) {
    if (!isLocked(generator)) return
    event.preventDefault()
    showUpgrade(generator.label, generator.minTier)
  }

  return { upgrade, hasTier, isLocked, showUpgrade, closeUpgrade, guard, guardGenerator }
}

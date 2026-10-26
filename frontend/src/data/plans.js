/**
 * The subscription plans, shared by the Billing page and the upgrade pop-up.
 * Prices and token amounts mirror SubscriptionTier on the backend.
 */

/** Plans from lowest to highest; the index is the plan's rank. */
export const tierOrder = ['FREE', 'PRO', 'PRO_PLUS', 'ULTIMATE_DM']

/** Highlighted on the plans page. */
export const MOST_POPULAR = 'PRO_PLUS'

export const plans = [
  {
    tier: 'FREE', price: '€0', tokens: '50 to start',
    features: ['One-shots', 'NPC, Boss, Backstory, Quest, Encounter, Loot, Shop and Puzzle generators', 'Earn tokens with ads and referrals', 'Buy token packs'],
  },
  {
    tier: 'PRO', price: '€9.99', tokens: '1,000 / month',
    features: ['Everything in Free', 'Full campaign generator', 'Plan, rewrite and add sessions', 'Factions and full Campaign Memory', 'Free-form questions', 'Unused tokens roll over up to 2,000'],
  },
  {
    tier: 'PRO_PLUS', price: '€24.99', tokens: '3,000 / month',
    features: ['Everything in Pro', 'Generate a campaign with every session planned, at a discount', 'Rolls over up to 6,000'],
  },
  {
    tier: 'ULTIMATE_DM', price: '€49.99', tokens: '5,000 / month',
    features: ['Everything in Pro+', 'Image and map generation (coming soon)', 'Rolls over up to 10,000'],
  },
]

export function findPlan(tier) {
  return plans.find((plan) => plan.tier === tier)
}

/** Whether `tier` includes everything `minimum` does. */
export function tierAtLeast(tier, minimum) {
  return tierOrder.indexOf(tier ?? 'FREE') >= tierOrder.indexOf(minimum ?? 'FREE')
}

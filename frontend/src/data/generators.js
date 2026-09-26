import {
  BookOpen, Castle, Map, Crown, Dices, Gem, MessageCircleQuestion, Puzzle, ScrollText, Store, Swords, UserRound,
} from 'lucide-vue-next'

/**
 * Every generator in the app, grouped the way the navigation shows them.
 *
 * `kind` decides which screen runs it:
 *  - 'world'  — the campaign generator (full campaign or one-shot), creates a campaign
 *  - 'module' — an in-campaign generator module (POST /campaigns/{id}/generate/{type})
 *  - 'ask'    — a free-form question answered with campaign context
 *
 * Costs and `minTier` (the lowest plan that may run it) mirror GenerationType on the backend.
 */
export const generatorGroups = [
  {
    id: 'world',
    label: 'World building',
    generators: [
      {
        slug: 'campaign', kind: 'world', label: 'Campaign', icon: Castle, cost: 50, minTier: 'PRO',
        description: 'A whole world: lore, locations, factions, NPCs, quests and a ready-to-run first session.',
      },
      {
        slug: 'full-campaign', kind: 'world', label: 'Full campaign', icon: Map, cost: 'from 75', minTier: 'PRO_PLUS',
        description: 'A whole world with every session planned, from the first scene to the finale. Cheaper than planning them one by one.',
      },
      {
        slug: 'one-shot', kind: 'world', label: 'One-Shot', icon: ScrollText, cost: 25, minTier: 'FREE',
        description: 'A compact adventure for a single sitting, with a proper ending.',
      },
    ],
  },
  {
    id: 'people',
    label: 'Characters',
    generators: [
      {
        slug: 'npc', kind: 'module', type: 'NPC', label: 'NPC', icon: UserRound, cost: 5, minTier: 'FREE',
        description: 'A character who fits your world, with a motivation, a secret and a voice.',
        placeholder: 'A tavern keeper who secretly spies for the cult',
      },
      {
        slug: 'boss', kind: 'module', type: 'BOSS', label: 'Boss', icon: Crown, cost: 15, minTier: 'FREE',
        description: 'A memorable antagonist with phases, lair actions and a reason to fight.',
        placeholder: 'The cult leader, cornered in the flooded crypt',
      },
      {
        slug: 'backstory', kind: 'module', type: 'BACKSTORY', label: 'Character Backstory', icon: BookOpen, cost: 10, minTier: 'FREE',
        description: "A player character's history, woven into the places and people of your campaign.",
        placeholder: 'An exiled noble who still sends letters home',
        needsCharacter: true,
      },
    ],
  },
  {
    id: 'adventure',
    label: 'Adventures',
    generators: [
      {
        slug: 'quest', kind: 'module', type: 'QUEST', label: 'Quest', icon: ScrollText, cost: 10, minTier: 'FREE',
        description: 'A quest that hooks into existing NPCs, places and conflicts.',
        placeholder: 'A side quest while escorting the heir to the capital',
      },
      {
        slug: 'encounter', kind: 'module', type: 'ENCOUNTER', label: 'Encounter', icon: Swords, cost: 10, minTier: 'FREE',
        description: 'A structured fight: SRD creatures with roles and tactics, terrain, and a calculated difficulty.',
        placeholder: 'An ambush on the mountain pass',
        hasDifficulty: true,
      },
      {
        slug: 'random-encounter', kind: 'module', type: 'RANDOM_ENCOUNTER', label: 'Random Encounter', icon: Dices, cost: 5, minTier: 'FREE',
        description: 'A rollable table of encounters for travel through a region.',
        placeholder: 'Travelling through the haunted marshes',
      },
      {
        slug: 'puzzle', kind: 'module', type: 'PUZZLE', label: 'Puzzle', icon: Puzzle, cost: 8, minTier: 'FREE',
        description: 'A puzzle with hints, alternative solutions and a way past it if the table gets stuck.',
        placeholder: 'A sealed door in a dwarven vault',
      },
    ],
  },
  {
    id: 'treasure',
    label: 'Treasure & trade',
    generators: [
      {
        slug: 'loot', kind: 'module', type: 'LOOT', label: 'Loot', icon: Gem, cost: 8, minTier: 'FREE',
        description: 'Treasure and magic items that belong in your world, saved as real items.',
        placeholder: 'Treasure in a crypt beneath the village church',
      },
      {
        slug: 'shop', kind: 'module', type: 'SHOP', label: 'Shop', icon: Store, cost: 12, minTier: 'FREE',
        description: 'A shop with a shopkeeper, stock and prices.',
        placeholder: 'A back-alley apothecary that asks no questions',
      },
    ],
  },
  {
    id: 'ask',
    label: 'Ask the AI',
    generators: [
      {
        slug: 'ask', kind: 'ask', label: 'Free-form question', icon: MessageCircleQuestion, cost: 5, minTier: 'PRO',
        description: 'Ask anything about your campaign and get an answer that knows your world.',
        placeholder: 'What might the villain do next, given what the party has done?',
      },
    ],
  },
]

export const allGenerators = generatorGroups.flatMap((group) =>
  group.generators.map((generator) => ({ ...generator, group: group.label })),
)

export function findGenerator(slug) {
  return allGenerators.find((g) => g.slug === slug)
}

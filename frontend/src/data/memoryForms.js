/**
 * The add/edit forms for Campaign Memory, one per resource. Field kinds:
 *  - text / textarea / checkbox
 *  - select: one of `options` (mirrors the enums in MemoryEnums.kt)
 *  - ref: one other memory entry, picked from memory[`source`]; sent as `key`,
 *    read back from the response's `from` ({ id, name })
 *  - refs: several, likewise
 * `wide` fields span both columns. The first field is the name/title and is required.
 */

const locationTypes = ['CONTINENT', 'REGION', 'CITY', 'TOWN', 'VILLAGE', 'DISTRICT', 'BUILDING', 'SHOP', 'DUNGEON', 'LANDMARK', 'PLANE', 'OTHER']

export const memoryForms = {
  npcs: {
    singular: 'NPC',
    fields: [
      { key: 'name', label: 'Name', kind: 'text', required: true },
      { key: 'role', label: 'Role', kind: 'text', placeholder: 'Quest giver, villain, merchant…' },
      { key: 'race', label: 'Race', kind: 'text' },
      { key: 'occupation', label: 'Occupation', kind: 'text' },
      { key: 'status', label: 'Status', kind: 'select', options: ['ALIVE', 'DEAD', 'MISSING', 'UNKNOWN'], default: 'ALIVE' },
      { key: 'disposition', label: 'Attitude to the party', kind: 'select', options: ['ALLY', 'FRIENDLY', 'NEUTRAL', 'UNFRIENDLY', 'HOSTILE', 'UNKNOWN'], default: 'NEUTRAL' },
      { key: 'locationId', from: 'location', label: 'Lives in', kind: 'ref', source: 'locations' },
      { key: 'factionId', from: 'faction', label: 'Faction', kind: 'ref', source: 'factions' },
      { key: 'description', label: 'Description', kind: 'textarea', wide: true },
      { key: 'appearance', label: 'Appearance', kind: 'textarea' },
      { key: 'personality', label: 'Personality', kind: 'textarea' },
      { key: 'motivation', label: 'Motivation', kind: 'textarea' },
      { key: 'secret', label: 'Secret', kind: 'textarea' },
      { key: 'voice', label: 'Voice & mannerisms', kind: 'text', wide: true },
      { key: 'notes', label: 'DM notes', kind: 'textarea', wide: true },
    ],
  },
  locations: {
    singular: 'location',
    fields: [
      { key: 'name', label: 'Name', kind: 'text', required: true },
      { key: 'type', label: 'Type', kind: 'select', options: locationTypes, default: 'OTHER' },
      { key: 'parentId', from: 'parent', label: 'Part of', kind: 'ref', source: 'locations', excludeSelf: true },
      { key: 'discovered', label: 'The party has discovered it', kind: 'checkbox', default: false },
      { key: 'description', label: 'Description', kind: 'textarea', wide: true },
      { key: 'atmosphere', label: 'Atmosphere', kind: 'textarea', wide: true },
      { key: 'notes', label: 'DM notes', kind: 'textarea', wide: true },
    ],
  },
  quests: {
    singular: 'quest',
    fields: [
      { key: 'title', label: 'Title', kind: 'text', required: true },
      { key: 'status', label: 'Status', kind: 'select', options: ['RUMOURED', 'AVAILABLE', 'ACTIVE', 'COMPLETED', 'FAILED', 'ABANDONED'], default: 'AVAILABLE' },
      { key: 'questGiverId', from: 'questGiver', label: 'Quest giver', kind: 'ref', source: 'npcs' },
      { key: 'locationId', from: 'location', label: 'Location', kind: 'ref', source: 'locations' },
      { key: 'involvedNpcIds', from: 'involvedNpcs', label: 'Other NPCs involved', kind: 'refs', source: 'npcs', wide: true },
      { key: 'description', label: 'Description', kind: 'textarea', wide: true },
      { key: 'hook', label: 'Hook', kind: 'textarea' },
      { key: 'objective', label: 'Objective', kind: 'textarea' },
      { key: 'reward', label: 'Reward', kind: 'text', wide: true },
      { key: 'notes', label: 'DM notes', kind: 'textarea', wide: true },
    ],
  },
  items: {
    singular: 'item',
    fields: [
      { key: 'name', label: 'Name', kind: 'text', required: true },
      { key: 'type', label: 'Type', kind: 'text', placeholder: 'Weapon, potion, wondrous item…' },
      { key: 'rarity', label: 'Rarity', kind: 'select', options: ['COMMON', 'UNCOMMON', 'RARE', 'VERY_RARE', 'LEGENDARY', 'ARTIFACT'], default: 'COMMON' },
      { key: 'requiresAttunement', label: 'Requires attunement', kind: 'checkbox', default: false },
      { key: 'ownerCharacterId', from: 'ownerCharacter', label: 'Carried by (player character)', kind: 'ref', source: 'characters' },
      { key: 'ownerNpcId', from: 'ownerNpc', label: 'Carried by (NPC)', kind: 'ref', source: 'npcs' },
      { key: 'locationId', from: 'location', label: 'Found at', kind: 'ref', source: 'locations' },
      { key: 'description', label: 'Description', kind: 'textarea', wide: true },
      { key: 'properties', label: 'Properties', kind: 'textarea' },
      { key: 'history', label: 'History', kind: 'textarea' },
      { key: 'notes', label: 'DM notes', kind: 'textarea', wide: true },
    ],
  },
}

/** "VERY_RARE" → "Very rare" */
export function optionLabel(value) {
  const text = String(value).replaceAll('_', ' ').toLowerCase()
  return text.charAt(0).toUpperCase() + text.slice(1)
}

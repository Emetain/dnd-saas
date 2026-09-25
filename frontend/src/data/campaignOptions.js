/**
 * Choices offered when setting up a campaign or one-shot.
 *
 * Game systems: only systems the generators actually support are listed. The
 * generators use SRD 5e creatures, challenge ratings and 5e encounter maths,
 * so offering other systems would produce content that does not fit them.
 * Add a system here once the backend supports it.
 */
export const gameSystems = ['D&D 5e']

export const limits = {
  players: 10,
  startingLevel: 20,
  oneShotHours: 20,
  campaignSessions: 100,
}

/** [1, 2, ..., max] for number dropdowns. */
export const range = (max) => Array.from({ length: max }, (_, i) => i + 1)

/** One of these is shown in the "Your idea" box each time the page opens. */
export const ideaSuggestions = {
  oneShot: [
    'A heist in a floating library during a thunderstorm',
    'A wedding where the groom is secretly a doppelganger',
    'Escort a caravan through a desert where the dunes move at night',
    'The party wakes up in a lighthouse with no memory of the last three days',
    'A village festival where every mask hides a cursed face',
    'Rescue a merchant from a pirate ship that is slowly sinking',
    'A haunted manor where the ghosts want help solving their own murder',
    'A race across a frozen lake before the ice gives way',
    'A dragon hires the party to recover an egg stolen from its hoard',
    'A trial in a fey court where lying is the only crime',
  ],
  campaign: [
    'The party slowly realises they have been working for the villain all along',
    'A sleeping god is waking, and every temple has a different idea of what to do',
    'A kingdom where magic is rationed — and the rations are running out',
    'Three noble houses race to claim the throne after the queen vanishes',
    'The sun rises a little later each day, and nobody knows why',
    'A thieves\' guild slowly takes over a city from the inside',
    'An ancient empire returns through a portal, convinced it still rules the world',
    'The party inherits a crumbling castle — and every debt its owners ever made',
    'A plague turns its victims to stone, and the cure lies beyond the edge of the map',
    'Rival wizarding schools drag their students into a secret war',
  ],
}

export function randomIdea(oneShot) {
  const list = oneShot ? ideaSuggestions.oneShot : ideaSuggestions.campaign
  return list[Math.floor(Math.random() * list.length)]
}

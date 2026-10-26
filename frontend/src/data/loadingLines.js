/**
 * Status lines for the generation overlay. The AI call is one request, so these
 * are flavour, not real progress: each theme walks through the same four phases
 * as the fake progress bar moves (world → people → plot → finishing).
 */

const themes = {
  horror: {
    keywords: ['horror', 'dark', 'grim', 'gothic', 'undead', 'vampire', 'dread*', 'curse*', 'eldritch', 'zombie', 'haunt*', 'creepy'],
    phases: [
      ['Lighting a single flickering candle…', 'Digging the graveyard (six feet, per regulations)…', 'Fogging up the moors…'],
      ['Convincing the villagers to be suspicious of strangers…', 'Teaching the butler to say "you shouldn\'t have come here"…', 'Waking something that should have stayed asleep…'],
      ['Hiding the diary with the last page torn out…', 'Placing ominous portraits whose eyes follow you…', 'Adding one (1) door that must never be opened…'],
      ['Checking under the bed…', 'Dimming the lights for dramatic effect…', 'Counting the survivors. Recounting. Hm…'],
    ],
  },
  comedic: {
    keywords: ['comed*', 'comic*', 'funny', 'silly', 'humo*', 'goofy', 'lighthearted', 'light-hearted', 'whimsic*', 'absurd', 'wacky'],
    phases: [
      ['Inflating the rubber chickens…', 'Assigning the tavern a ridiculous name…', 'Hiring a mimic to play the treasure chest…'],
      ['Giving the goblin boss a tragic backstory and a tiny hat…', 'Teaching the bard exactly one song, badly…', 'Rolling a natural 1 on the NPC\'s dignity…'],
      ['Hiding the main quest behind a lost-and-found counter…', 'Making the prophecy rhyme (mostly)…', 'Placing a trap that is, frankly, a bucket on a door…'],
      ['Tuning the laugh track…', 'Removing the third pun about "chest"… keeping the other two…', 'Asking the kobolds to take a bow…'],
    ],
  },
  mystery: {
    keywords: ['mystery', 'intrigue', 'detective', 'murder', 'conspiracy', 'noir', 'heist', 'spy', 'secret', 'investigat*', 'politic*'],
    phases: [
      ['Drawing the map… then redacting half of it…', 'Setting the fog to "suspiciously convenient"…', 'Locking the room. From the inside. Somehow.'],
      ['Giving every suspect a flimsy alibi…', 'Teaching the butler to look guilty (he didn\'t do it)…', 'Handing out secret letters in unmarked envelopes…'],
      ['Planting a red herring. Then another red herring…', 'Connecting the red string on the corkboard…', 'Hiding the real clue in plain sight…'],
      ['Double-checking who did it…', 'Rehearsing the dramatic reveal in the library…', 'Burning the evidence. Kidding. Mostly.'],
    ],
  },
  nautical: {
    keywords: ['pirate', 'sea', 'ocean', 'ship', 'island', 'naval', 'sail', 'kraken', 'coast', 'harbor', 'harbour'],
    phases: [
      ['Pouring the ocean (salted to taste)…', 'Scattering islands shaped like skulls…', 'Burying treasure and immediately forgetting where…'],
      ['Recruiting a crew of questionable hygiene…', 'Fitting the captain with a dramatic peg leg…', 'Teaching the parrot one secret it shouldn\'t know…'],
      ['Drawing an X that definitely marks the spot…', 'Summoning a kraken for scheduled Tuesday appearances…', 'Writing a mutiny into the fine print…'],
      ['Swabbing the deck…', 'Checking the ship for holes. Found one. Ignoring it.', 'Hoisting the sails…'],
    ],
  },
  epic: {
    keywords: ['heroic', 'epic', 'war', 'dragon', 'legend', 'high fantasy', 'prophecy', 'chosen', 'kingdom', 'empire', 'battle'],
    phases: [
      ['Raising mountains of suspicious majesty…', 'Carving ancient runes nobody can read…', 'Building a kingdom and a slightly larger rival kingdom…'],
      ['Polishing the heroes\' armour to a blinding shine…', 'Waking the dragon (carefully)…', 'Writing a villain speech with extra monologue…'],
      ['Forging a legendary sword and losing it in a lake…', 'Drafting a prophecy with at least three loopholes…', 'Marching armies into dramatic formation…'],
      ['Cueing the orchestra…', 'Adding one more glorious sunset…', 'Tightening the cape for maximum billow…'],
    ],
  },
  default: {
    keywords: [],
    phases: [
      ['Unfolding the world map…', 'Placing a tavern in every village (it\'s tradition)…', 'Growing forests, rivers and a suspicious cave…'],
      ['Rolling up NPCs with strong opinions…', 'Giving the innkeeper a secret…', 'Convincing the villain to have a plan…'],
      ['Writing quests on crumpled notice-board paper…', 'Hiding treasure behind a puzzle only the rogue will solve…', 'Sharpening plot hooks…'],
      ['Rolling for initiative…', 'Dusting off the dice tower…', 'Putting the final touches on the DM screen…'],
    ],
  },
}

/**
 * Keywords match whole words (plus a plural "s"), so "ship" doesn't match "friendship".
 * A trailing * matches any word starting with it: "comed*" matches "comedy" and "comedic".
 */
function matcher(keyword) {
  const stem = keyword.replace(/\*$/, '')
  return new RegExp(keyword.endsWith('*') ? `\\b${stem}` : `\\b${stem}s?\\b`, 'i')
}

for (const theme of Object.values(themes)) theme.matchers = theme.keywords.map(matcher)

/** Picks a theme from the tone first, then the themes, then the idea itself. */
export function pickTheme({ tone = '', themes: themeText = '', idea = '' } = {}) {
  for (const text of [tone, themeText, idea]) {
    for (const [name, theme] of Object.entries(themes)) {
      if (theme.matchers.some((m) => m.test(text || ''))) return name
    }
  }
  return 'default'
}

/** The four phases of status lines for a theme. */
export function linesFor(themeName) {
  return (themes[themeName] || themes.default).phases
}

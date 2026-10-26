/**
 * Example results for the showcase below the banner, one list per type.
 * Add an example by adding an object to a list — the showcase and the
 * generator pages pick it up without any other change.
 *
 * Each type opens one generator (`generator` is its slug in data/generators.js).
 *
 * Each example:
 *  - id        unique, used in the link that opens it (?example=<id>)
 *  - name      shown large in the result card
 *  - label     the small gold line above the name
 *  - settings  the options it was made with (the result card's footer)
 *  - fields    { label, value, highlight? } — highlighted fields get a box
 *  - prefill   what "Use this example" fills in on the generator:
 *              module generators take { instruction, difficulty? },
 *              the campaign generator takes its form fields ({ idea, tone, themes, startingLevel, … })
 */
export const exampleTypes = [
  {
    id: 'npc',
    label: 'NPC',
    generator: 'npc',
    examples: [
      {
        id: 'npc-mirela-thornwick',
        name: 'Mirela Thornwick',
        label: 'Half-elf · Harbormaster',
        settings: 'Level 5–10 · Coastal city · Grim',
        fields: [
          { label: 'Look', value: 'Salt-stained coat, one silver earring, never sits down.' },
          { label: 'Personality', value: 'Polite, precise, and always counting something.' },
          { label: 'Secret', value: "She lets a smuggler's ship dock off the books. It belongs to her brother.", highlight: true },
          { label: 'Story hook', value: "A page is missing from her ledger, and she'll pay quietly to get it back.", highlight: true },
        ],
        prefill: { instruction: 'A half-elf harbormaster in a coastal city, for a party of level 5–10. Grim tone.' },
      },
      {
        id: 'npc-brother-aldous-venn',
        name: 'Brother Aldous Venn',
        label: 'Human · Temple archivist',
        settings: 'Level 1–4 · Forest village · Mysterious',
        fields: [
          { label: 'Look', value: 'Ink up to the elbows, spectacles mended with twine.' },
          { label: 'Personality', value: 'Warm, rambling, and quietly terrified of the crypt below.' },
          { label: 'Secret', value: 'He sealed the lower vault himself, and something knocked back.', highlight: true },
          { label: 'Story hook', value: "He needs a book fetched from the vault, and won't say why it matters.", highlight: true },
        ],
        prefill: { instruction: 'A human temple archivist in a forest village, for a party of level 1–4. Mysterious tone.' },
      },
      {
        id: 'npc-kessa-dunmore',
        name: 'Kessa Dunmore',
        label: 'Halfling · Fence and pie-seller',
        settings: 'Level 5–10 · Coastal city · Lighthearted',
        fields: [
          { label: 'Look', value: 'Flour on her apron, a knife in her boot, a laugh you hear across the market.' },
          { label: 'Personality', value: 'Friendly to everyone, loyal to no one, and honest about it.' },
          { label: 'Secret', value: 'She passes tips to the city watch to keep her son out of trouble.', highlight: true },
          { label: 'Story hook', value: "She's selling a map the thieves' guild badly wants back.", highlight: true },
        ],
        prefill: { instruction: 'A halfling fence and pie-seller in a coastal city, for a party of level 5–10. Lighthearted tone.' },
      },
    ],
  },
  {
    id: 'campaign',
    label: 'Campaign',
    generator: 'campaign',
    examples: [
      {
        id: 'campaign-the-drowned-crown',
        name: 'The Drowned Crown',
        label: 'Campaign · Levels 1–10 · Coastal kingdom',
        settings: 'Levels 1–10 · Coastal kingdom',
        fields: [
          { label: 'Premise', value: 'A sunken crown is rising from the bay, and every faction in the port city wants it first.' },
          { label: 'Villain', value: 'Admiral Sera Voss, who believes the crown will let her command the tides.' },
          { label: 'Factions', value: 'The Tidewardens (temple), the Salt Guild (smugglers), the Royal Navy.' },
          { label: 'Story arc', value: 'Act 1 Strange tides and missing fishermen. Act 2 A race to the reef ruins. Act 3 The crown awakens.' },
          { label: 'Opening hook', value: "The party's ship is the only one to survive the first freak tide.", highlight: true },
        ],
        prefill: {
          idea: 'A sunken crown is rising from the bay, and every faction in the port city wants it first. The villain, Admiral Sera Voss, believes the crown will let her command the tides.',
          themes: 'a coastal kingdom, rival factions, the sea',
          startingLevel: 1,
        },
      },
      {
        id: 'campaign-ashes-of-the-ember-throne',
        name: 'Ashes of the Ember Throne',
        label: 'Campaign · Levels 3–12 · Volcanic empire',
        settings: 'Levels 3–12 · Volcanic empire',
        fields: [
          { label: 'Premise', value: 'The emperor has died, and his fire-bound heir refuses to wake.' },
          { label: 'Villain', value: 'Chancellor Idris Kael, who keeps the heir asleep so he can rule in his place.' },
          { label: 'Factions', value: 'The Ember Legion, the Ashwalkers (rebels), the Glass Monks.' },
          { label: 'Story arc', value: "Act 1 A coup at the funeral. Act 2 Finding the heir's hidden tomb. Act 3 The volcano wakes with the heir." },
          { label: 'Opening hook', value: 'The party is hired to guard the funeral, and the coffin is empty.', highlight: true },
        ],
        prefill: {
          idea: 'The emperor has died, and his fire-bound heir refuses to wake. Chancellor Idris Kael keeps the heir asleep so he can rule in his place.',
          themes: 'a volcanic empire, succession, rebellion',
          startingLevel: 3,
        },
      },
      {
        id: 'campaign-the-long-winter-road',
        name: 'The Long Winter Road',
        label: 'Campaign · Levels 1–8 · Frozen north',
        settings: 'Levels 1–8 · Frozen north',
        fields: [
          { label: 'Premise', value: 'Winter has lasted three years, and the last warm city is closing its gates.' },
          { label: 'Villain', value: 'The Frost Shepherd, a fey who wants the world to sleep forever.' },
          { label: 'Factions', value: 'The Gatekeepers, the Reindeer Clans, the Hearthfire Church.' },
          { label: 'Story arc', value: 'Act 1 Escorting refugees to the city. Act 2 Finding the lost summer stone. Act 3 Facing the Shepherd on the endless glacier.' },
          { label: 'Opening hook', value: 'A child in the caravan can melt snow with a touch.', highlight: true },
        ],
        prefill: {
          idea: 'Winter has lasted three years, and the last warm city is closing its gates. The Frost Shepherd, a fey, wants the world to sleep forever.',
          themes: 'the frozen north, survival, refugees',
          startingLevel: 1,
        },
      },
    ],
  },
  {
    id: 'character',
    label: 'Character',
    generator: 'backstory',
    examples: [
      {
        id: 'character-tamsin-ashgrove',
        name: 'Tamsin Ashgrove',
        label: 'Wood elf · Ranger · Level 3',
        settings: 'Wood elf · Ranger · Level 3',
        fields: [
          { label: 'Background', value: 'Outlander. Grew up guiding caravans through the Thornwood.' },
          { label: 'Personality', value: 'Quiet, patient, trusts animals more than people.' },
          { label: 'Goal', value: 'Find the poacher who burned her home forest.' },
          { label: 'Flaw', value: 'Never asks for help, even when she should.', highlight: true },
          { label: 'Ability scores', value: 'STR 12 · DEX 17 · CON 13 · INT 10 · WIS 15 · CHA 8' },
          { label: 'Starting gear', value: 'Longbow, two shortswords, a hawk-feather cloak.' },
        ],
        prefill: { instruction: 'A wood elf ranger, level 3, an outlander who grew up guiding caravans through the Thornwood.' },
      },
      {
        id: 'character-borin-stoneheart',
        name: 'Borin Stoneheart',
        label: 'Mountain dwarf · Cleric · Level 1',
        settings: 'Mountain dwarf · Cleric · Level 1',
        fields: [
          { label: 'Background', value: "Acolyte. Kept the forge-temple's fire burning for twenty years." },
          { label: 'Personality', value: 'Loud, generous, and quotes scripture at the worst moments.' },
          { label: 'Goal', value: 'Relight the lost temple of his clan.' },
          { label: 'Flaw', value: "Can't stand being told he's wrong.", highlight: true },
          { label: 'Ability scores', value: 'STR 15 · DEX 10 · CON 16 · INT 8 · WIS 15 · CHA 12' },
          { label: 'Starting gear', value: 'Warhammer, chain mail, a shield with a hammer symbol.' },
        ],
        prefill: { instruction: "A mountain dwarf cleric, level 1, an acolyte who kept the forge-temple's fire burning for twenty years." },
      },
      {
        id: 'character-vex',
        name: 'Vex',
        label: 'Tiefling · Rogue · Level 2',
        settings: 'Tiefling · Rogue · Level 2',
        fields: [
          { label: 'Background', value: 'Charlatan. Sold fake fortunes in every city on the coast.' },
          { label: 'Personality', value: 'Charming, quick-witted, always has an exit plan.' },
          { label: 'Goal', value: 'Pay off a debt to a crime lord before it gets collected the hard way.' },
          { label: 'Flaw', value: "Can't resist a bet, even a bad one.", highlight: true },
          { label: 'Ability scores', value: 'STR 8 · DEX 15 · CON 13 · INT 13 · WIS 10 · CHA 16' },
          { label: 'Starting gear', value: "Two daggers, thieves' tools, a deck of marked cards." },
        ],
        prefill: { instruction: 'A tiefling rogue, level 2, a charlatan who sold fake fortunes in every city on the coast.' },
      },
    ],
  },
  {
    id: 'item',
    label: 'Item',
    generator: 'loot',
    examples: [
      {
        id: 'item-lantern-of-the-last-watch',
        name: 'Lantern of the Last Watch',
        label: 'Wondrous item · Uncommon',
        settings: 'Wondrous item · Uncommon',
        fields: [
          { label: 'Description', value: 'A dented brass lantern that never runs out of oil.' },
          { label: 'Effect', value: "While it's lit, invisible creatures within 30 feet show a faint outline." },
          { label: 'History', value: 'Carried by a lighthouse keeper who kept watch for 40 years after the town drowned.' },
          { label: 'Quirk', value: 'On stormy nights it whispers the names of sailors lost at sea.', highlight: true },
        ],
        prefill: { instruction: 'One uncommon wondrous item: a dented brass lantern that never runs out of oil.' },
      },
      {
        id: 'item-boots-of-the-muddy-road',
        name: 'Boots of the Muddy Road',
        label: 'Wondrous item · Common',
        settings: 'Wondrous item · Common',
        fields: [
          { label: 'Description', value: 'Worn leather boots that always feel a little damp.' },
          { label: 'Effect', value: 'You never leave footprints in mud or snow.' },
          { label: 'History', value: 'Made for a courier who could never be followed.' },
          { label: 'Quirk', value: 'They squelch loudly whenever the wearer tells a lie.', highlight: true },
        ],
        prefill: { instruction: 'One common wondrous item: worn leather boots that always feel a little damp.' },
      },
      {
        id: 'item-emberglass-dagger',
        name: 'Emberglass Dagger',
        label: 'Weapon · Rare · Requires attunement',
        settings: 'Weapon · Rare · Requires attunement',
        fields: [
          { label: 'Description', value: 'A dagger made of dark red glass, warm to the touch.' },
          { label: 'Effect', value: 'Deals an extra 1d6 fire damage on a hit.' },
          { label: 'History', value: 'Forged inside a volcano by a smith who wanted revenge.' },
          { label: 'Quirk', value: "It glows brighter when its maker's family is nearby.", highlight: true },
        ],
        prefill: { instruction: 'One rare weapon that requires attunement: a dagger made of dark red glass, warm to the touch.' },
      },
    ],
  },
  {
    id: 'battle',
    label: 'Battle',
    generator: 'encounter',
    examples: [
      {
        id: 'battle-ambush-at-broken-bridge',
        name: 'Ambush at Broken Bridge',
        label: 'Encounter · 4 players · Level 5 · Hard',
        settings: '4 players · Level 5 · Hard',
        fields: [
          { label: 'Enemies', value: 'Bandit leader (veteran), 2 thugs, 4 bandits, 2 scouts with bows.' },
          { label: 'Terrain', value: 'A collapsed stone bridge over a fast river, with a 15-foot gap.' },
          { label: 'Tactics', value: 'Scouts shoot from the trees, thugs block the bridge, the leader waits until someone falls into the river.' },
          { label: 'Twist', value: 'Halfway through the fight, the bridge starts to crumble further.', highlight: true },
          { label: 'Loot', value: '120 gp, a forged toll permit, a map with a red X.' },
        ],
        prefill: { instruction: 'An ambush at a collapsed stone bridge over a fast river, for 4 players at level 5.', difficulty: 'HARD' },
      },
      {
        id: 'battle-goblin-lanterns-in-the-marsh',
        name: 'Goblin Lanterns in the Marsh',
        label: 'Encounter · 4 players · Level 2 · Medium',
        settings: '4 players · Level 2 · Medium',
        fields: [
          { label: 'Enemies', value: '4 goblins carrying lanterns, 1 giant frog hiding in the reeds.' },
          { label: 'Terrain', value: 'Knee-deep marsh water (difficult terrain) with small islands of dry ground.' },
          { label: 'Tactics', value: 'The goblins swing their lanterns to lure the party off the path, and the frog grabs whoever wanders too close.' },
          { label: 'Twist', value: "One lantern holds a trapped will-o'-wisp that escapes if it breaks.", highlight: true },
          { label: 'Loot', value: '28 gp, a silver frog charm, a waterproof map case.' },
        ],
        prefill: { instruction: 'Goblins with lanterns luring travellers off the path in a marsh, for 4 players at level 2.', difficulty: 'MEDIUM' },
      },
      {
        id: 'battle-the-crypt-guardian',
        name: 'The Crypt Guardian',
        label: 'Encounter · 4 players · Level 8 · Hard',
        settings: '4 players · Level 8 · Hard',
        fields: [
          { label: 'Enemies', value: '1 helmed horror, 2 mummies, 6 skeletons.' },
          { label: 'Terrain', value: 'A long crypt hall with stone coffins for cover and a pit in the middle.' },
          { label: 'Tactics', value: 'Skeletons rise from the coffins in waves, the mummies guard the sealed door, and the helmed horror attacks whoever touches the treasure.' },
          { label: 'Twist', value: 'Breaking the glowing seal on the door stops the skeletons from rising.', highlight: true },
          { label: 'Loot', value: '600 gp in old coins, a +1 shield, a key carved from bone.' },
        ],
        prefill: { instruction: 'A guarded crypt hall with undead rising from the coffins, for 4 players at level 8.', difficulty: 'HARD' },
      },
    ],
  },
]

/** Finds an example by its id, together with the type it belongs to. */
export function findExample(id) {
  for (const type of exampleTypes) {
    const example = type.examples.find((e) => e.id === id)
    if (example) return { type, example }
  }
  return null
}

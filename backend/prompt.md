Restyle my existing frontend with a new visual style, and add one new
"example results" section. Apart from that section, this is a VISUAL-ONLY change.

Do NOT change:
- the layout or where things are placed
- which pages, routes or sections exist (including the pricing page)
- text/copy, features or any logic
- how my theme toggle works or how it saves the choice
  Only change colors, fonts, buttons, cards, backgrounds and hero styling,
  plus add the showcase in section 7.

If the frontend-design skill/plugin is active: follow THIS spec over its
defaults (fonts, colors, layout). Use the skill only for details this spec
doesn't cover.

Reference design (use it ONLY for the visual style, not the layout).
The "Dark & light theme" board shows both themes side by side:
https://claude.ai/artifact/FiyYbwzPG1TGMUzmcqzPv1

## 1. Theme tokens (set up FIRST)
My site has a light/dark toggle. Define ONE set of token names with a dark
and a light value, switched by the toggle (use the mechanism the app already
uses, e.g. [data-theme] or a .dark class). Components must only use tokens,
never hard-coded colors, so both themes work automatically.

Token            Dark      Light
bg (page)        #0E0D0C   #F6F4EF
surface (cards)  #161513   #FFFFFF
raised           #1D1C19   #EFECE5
inset/highlight  #1A1917   #F4F1EA
border           #2A2824   #E2DED5
divider          #1F1E1B   #E9E5DD
border-strong    #3A3732   #CFC9BD
text             #EDE9E1   #1A1814
text-body        #D9D4CB   #3A3630
text-muted       #A39E94   #625D54
text-subtle      #8A857C   #6F6A61
accent-text      #D4A84B   #8A6414   (gold text, links, eyebrows, icons)
accent-bg        #D4A84B   #D4A84B   (primary button, same in both)
on-accent        #1A1408   #1A1408   (text on the gold button)

Fonts (Google Fonts):
- Display: Cormorant Garamond 600 for big headings and names
- Body: Geist 400/500/600
- Labels: Geist Mono 12px, uppercase, letter-spacing 0.08em
  Shape: radius 8 inputs, 10 buttons, 12 cards, 16 large panels; 1px borders.

## 2. Background
- Flat page background (bg token). No gradients, no glows, no heavy shadows.
- Cards and panels use the surface token with a 1px border.
- Separate big sections with a 1px divider line instead of color blocks.

## 3. Buttons (replace all existing button styles)
- Primary: accent-bg background, on-accent text, Geist 600 15px, min-height 48px
  (44px for small/nav), padding 0 24px, radius 10px, no border, no shadow.
- Secondary: transparent background, 1px border-strong, text color, weight 500,
  same size and radius as primary.
- Text link button: accent-text color, weight 500, optional "→" at the end.
- Icons in buttons: 16–18px stroke SVGs using currentColor, 8px gap.
- Hover: slightly lighter/darker gold for primary, darker border for secondary.
  Focus: visible 2px outline in accent-text. All buttons at least 44px tall.

## 4. Generator result card (use for ALL generator results)
- Container: surface, 1px border, radius 16px, overflow hidden.
- Header (padding 28–32px, 20px on mobile, bottom border):
    - small accent-text mono label above the name, e.g. "HALF-ELF · HARBORMASTER"
    - name in Cormorant Garamond 600, ~40–44px, line-height 1.05
    - action buttons (Reroll, Copy) as secondary buttons with icons, on the
      right (wrap on mobile)
- Body: 2-column grid (1 column on mobile), gap 24px 32px.
  Each field = mono label (12px, uppercase, text-subtle) + value (15–16px,
  line-height 1.6, text-body).
- Highlight boxes for important fields (marked "highlight" in the examples):
  inset/highlight background, 1px border, radius 12px, padding 16–20px.
- Footer strip: top border, Geist Mono 12px text-subtle, chosen options on the
  left (e.g. "Level 5–10 · Coastal city · Grim") and "Roll #n" on the right.
- Each generator keeps its own fields but uses this same structure.

## 5. Hero (keep my current hero's layout and content, only refine it)
- Small eyebrow line above the headline: Geist Mono 13px, uppercase,
  accent-text color.
- Headline: Cormorant Garamond 600, clamp(48px, 7vw, 92px), line-height 1,
  letter-spacing -0.01em, max-width ~900px so it breaks into 2–3 lines.
- Subtext: 18–19px, text-muted, line-height 1.55, max-width ~560px.
- Buttons: at most one primary + one secondary, 12px apart, ~40px below subtext.
- Spacing: ~112px above and ~88px below on desktop, less on mobile.
- If the hero has an image or product preview, put it in a bordered panel
  (radius 16) instead of shadows or glows.
- Remove anything that competes for attention: gradients, glows, extra badges,
  more than two buttons.
- Show me a before/after of the hero and explain each tweak in one line.

## 6. Everything else
- Inputs/selects: surface background, 1px border-strong, radius 8px, 44px tall,
  label above in 13px text-muted.
- Pills/badges: 1px border, radius 999px, 12–13px text-muted; highlighted
  badges use accent-text for border and text.
- No emoji, no gradients. Text contrast at least 4.5:1 in BOTH themes.
  Works down to 375px wide.

## 7. NEW: Example results showcase (the ONE layout exception)
A section showing example results from different generators. Visitors can
browse them and use one as a starting point if they want.

Placement: directly below the hero. If my site already has an example section,
upgrade that one instead of adding a second one.

How it works:
- A row of pill buttons to switch type: NPC, Campaign, Character, Item, Battle.
  Accessible (tabs with arrow-key support, or buttons with aria-pressed).
  Wraps on mobile. "Battle" means my Encounter generator.
- The selected example shows in the result card style from section 4.
- Each type has 3 examples, but only ONE is shown at a time:
    - "Show another" button (secondary, dice or refresh icon) switches to the
      next example of the same type, like the generator's Reroll button.
    - Small counter next to it, Geist Mono 12px, text-subtle: "1 / 3".
    - Switching type always starts at that type's first example.
    - Optional soft fade (150–200ms); none with prefers-reduced-motion.
      Never auto-rotate.
- Card actions:
    - "Use this example" (primary): opens that generator with the example's
      settings filled in and this result shown, so they can reroll, copy or edit it.
      Always uses the example currently on screen.
    - "Try this generator" (secondary): opens that generator empty.
- Respect plan access: if the user's plan doesn't include that generator, use
  the same lock/upgrade behavior the app already has.
- Put all examples in ONE data file (e.g. examples.ts or examples.json),
  as a list per type, so I can add more later without touching the component.

Example content (use exactly this to start):

NPC 1: "Mirela Thornwick" (Half-elf · Harbormaster)
Settings: Level 5–10 · Coastal city · Grim
- Look: Salt-stained coat, one silver earring, never sits down.
- Personality: Polite, precise, and always counting something.
- Secret (highlight): She lets a smuggler's ship dock off the books. It belongs to her brother.
- Story hook (highlight): A page is missing from her ledger, and she'll pay quietly to get it back.

NPC 2: "Brother Aldous Venn" (Human · Temple archivist)
Settings: Level 1–4 · Forest village · Mysterious
- Look: Ink up to the elbows, spectacles mended with twine.
- Personality: Warm, rambling, and quietly terrified of the crypt below.
- Secret (highlight): He sealed the lower vault himself, and something knocked back.
- Story hook (highlight): He needs a book fetched from the vault, and won't say why it matters.

NPC 3: "Kessa Dunmore" (Halfling · Fence and pie-seller)
Settings: Level 5–10 · Coastal city · Lighthearted
- Look: Flour on her apron, a knife in her boot, a laugh you hear across the market.
- Personality: Friendly to everyone, loyal to no one, and honest about it.
- Secret (highlight): She passes tips to the city watch to keep her son out of trouble.
- Story hook (highlight): She's selling a map the thieves' guild badly wants back.

Campaign 1: "The Drowned Crown" (Campaign · Levels 1–10 · Coastal kingdom)
- Premise: A sunken crown is rising from the bay, and every faction in the port city wants it first.
- Villain: Admiral Sera Voss, who believes the crown will let her command the tides.
- Factions: The Tidewardens (temple), the Salt Guild (smugglers), the Royal Navy.
- Story arc: Act 1 Strange tides and missing fishermen. Act 2 A race to the reef ruins. Act 3 The crown awakens.
- Opening hook (highlight): The party's ship is the only one to survive the first freak tide.

Campaign 2: "Ashes of the Ember Throne" (Campaign · Levels 3–12 · Volcanic empire)
- Premise: The emperor has died, and his fire-bound heir refuses to wake.
- Villain: Chancellor Idris Kael, who keeps the heir asleep so he can rule in his place.
- Factions: The Ember Legion, the Ashwalkers (rebels), the Glass Monks.
- Story arc: Act 1 A coup at the funeral. Act 2 Finding the heir's hidden tomb. Act 3 The volcano wakes with the heir.
- Opening hook (highlight): The party is hired to guard the funeral, and the coffin is empty.

Campaign 3: "The Long Winter Road" (Campaign · Levels 1–8 · Frozen north)
- Premise: Winter has lasted three years, and the last warm city is closing its gates.
- Villain: The Frost Shepherd, a fey who wants the world to sleep forever.
- Factions: The Gatekeepers, the Reindeer Clans, the Hearthfire Church.
- Story arc: Act 1 Escorting refugees to the city. Act 2 Finding the lost summer stone. Act 3 Facing the Shepherd on the endless glacier.
- Opening hook (highlight): A child in the caravan can melt snow with a touch.

Character 1: "Tamsin Ashgrove" (Wood elf · Ranger · Level 3)
- Background: Outlander. Grew up guiding caravans through the Thornwood.
- Personality: Quiet, patient, trusts animals more than people.
- Goal: Find the poacher who burned her home forest.
- Flaw (highlight): Never asks for help, even when she should.
- Ability scores: STR 12 · DEX 17 · CON 13 · INT 10 · WIS 15 · CHA 8
- Starting gear: Longbow, two shortswords, a hawk-feather cloak.

Character 2: "Borin Stoneheart" (Mountain dwarf · Cleric · Level 1)
- Background: Acolyte. Kept the forge-temple's fire burning for twenty years.
- Personality: Loud, generous, and quotes scripture at the worst moments.
- Goal: Relight the lost temple of his clan.
- Flaw (highlight): Can't stand being told he's wrong.
- Ability scores: STR 15 · DEX 10 · CON 16 · INT 8 · WIS 15 · CHA 12
- Starting gear: Warhammer, chain mail, a shield with a hammer symbol.

Character 3: "Vex" (Tiefling · Rogue · Level 2)
- Background: Charlatan. Sold fake fortunes in every city on the coast.
- Personality: Charming, quick-witted, always has an exit plan.
- Goal: Pay off a debt to a crime lord before it gets collected the hard way.
- Flaw (highlight): Can't resist a bet, even a bad one.
- Ability scores: STR 8 · DEX 15 · CON 13 · INT 13 · WIS 10 · CHA 16
- Starting gear: Two daggers, thieves' tools, a deck of marked cards.

Item 1: "Lantern of the Last Watch" (Wondrous item · Uncommon)
- Description: A dented brass lantern that never runs out of oil.
- Effect: While it's lit, invisible creatures within 30 feet show a faint outline.
- History: Carried by a lighthouse keeper who kept watch for 40 years after the town drowned.
- Quirk (highlight): On stormy nights it whispers the names of sailors lost at sea.

Item 2: "Boots of the Muddy Road" (Wondrous item · Common)
- Description: Worn leather boots that always feel a little damp.
- Effect: You never leave footprints in mud or snow.
- History: Made for a courier who could never be followed.
- Quirk (highlight): They squelch loudly whenever the wearer tells a lie.

Item 3: "Emberglass Dagger" (Weapon · Rare · Requires attunement)
- Description: A dagger made of dark red glass, warm to the touch.
- Effect: Deals an extra 1d6 fire damage on a hit.
- History: Forged inside a volcano by a smith who wanted revenge.
- Quirk (highlight): It glows brighter when its maker's family is nearby.

Battle 1: "Ambush at Broken Bridge" (Encounter · 4 players · Level 5 · Hard)
- Enemies: Bandit leader (veteran), 2 thugs, 4 bandits, 2 scouts with bows.
- Terrain: A collapsed stone bridge over a fast river, with a 15-foot gap.
- Tactics: Scouts shoot from the trees, thugs block the bridge, the leader
  waits until someone falls into the river.
- Twist (highlight): Halfway through the fight, the bridge starts to crumble further.
- Loot: 120 gp, a forged toll permit, a map with a red X.

Battle 2: "Goblin Lanterns in the Marsh" (Encounter · 4 players · Level 2 · Medium)
- Enemies: 4 goblins carrying lanterns, 1 giant frog hiding in the reeds.
- Terrain: Knee-deep marsh water (difficult terrain) with small islands of dry ground.
- Tactics: The goblins swing their lanterns to lure the party off the path,
  and the frog grabs whoever wanders too close.
- Twist (highlight): One lantern holds a trapped will-o'-wisp that escapes if it breaks.
- Loot: 28 gp, a silver frog charm, a waterproof map case.

Battle 3: "The Crypt Guardian" (Encounter · 4 players · Level 8 · Hard)
- Enemies: 1 helmed horror, 2 mummies, 6 skeletons.
- Terrain: A long crypt hall with stone coffins for cover and a pit in the middle.
- Tactics: Skeletons rise from the coffins in waves, the mummies guard the
  sealed door, and the helmed horror attacks whoever touches the treasure.
- Twist (highlight): Breaking the glowing seal on the door stops the skeletons from rising.
- Loot: 600 gp in old coins, a +1 shield, a key carved from bone.

(Battle difficulty labels follow the 2014 encounter rules. If my app uses the
2024 rules, adjust the labels to match.)

## How to work
1. Look at how styling and the theme toggle work now, then add the tokens and fonts.
2. Restyle the buttons, the generator result card and the hero first.
   Show me one page in both light and dark before continuing.
3. After I approve, build the example showcase (section 7) and show it to me
   in both themes.
4. Then apply the style to the rest of the app.
5. Finish with a short list of what you changed.
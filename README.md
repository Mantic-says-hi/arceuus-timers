# Arceuus Timers
An alternate implementation for Arceuus spellbook timers.

This plugin is a visual upgrade over the Arceuus spells in the RuneLite 'Timers' plugin: unique
icons per spell, more supported spells, on-player Death Charge tracking, and config for nearly
every visible detail.

![allIcons](https://imgur.com/kG2g3bS.png)

## Unique spell icons
- The RuneLite 'Timers' plugin shows the same icon regardless of the Arceuus spell cast.
  This plugin gives each spell its own icon.
- Icons are the native game sprites, drawn pixel-for-pixel with no rescaling blur, and placed
  to leave room for the time-remaining text below.

### Unique thrall icons
- Custom icons for every thrall attack style and subtype
- Impish thralls get their own icons, with a toggle for the classic ones
- Custom icon for the thrall re-cast cooldown

![icons](https://imgur.com/u0rN6DY.png)

## Death Charge
### Infobox timers
- An active Death Charge has no in-game timer, so only the cooldown is timed
- Optional infobox for the active charge, and an infobox for the cooldown
- **Stack Death Charge Boxes** combines the active charge infoboxes into one
- **Small Icon** uses the small centred sprite in the active charge infobox instead of
  filling the box

![charge](https://imgur.com/aKRhIaG.png)

### On-player tracking
- **Show On Player** draws the Death Charge icon above your character while it's active,
  with a small `2` under it while you're holding two charges
- **One Charge Icon** (on by default) keeps it to a single icon with the number; turn it
  off to stack a second offset icon instead; the text follows the RuneLite infobox font,
  with size and per-side colours configurable
- **Show On Party Members** draws the icon on RuneLite Party members who also run this plugin
- **Detect Other Casters** draws the icon on any player you *see* cast Death Charge, even if
  they aren't in your party or don't have the plugin. The count is a guess, shown with a `?`,
  and clears on a nearby kill or when the spell would expire.
- Players stacked on one tile collapse into a single icon, with the number of casters shown
  under its left side
- **Positioning** lets you anchor the icon to Head / Neck / Middle / Feet, adjust its placement with X/Y
  and size offsets, or turn on **Reposition Mode** to drag it into place with the mouse.
  Shift-right-click the icon to start or stop repositioning, reset the offsets, or hide it.

## Resurrected Thralls
- Infobox timer for an active thrall, plus an optional cooldown timer
- Support for the impish thrall override

## Other spells
- Shadow Veil timer and cooldown
- Ward of Arceuus timer and cooldown
- Mark of Darkness timer and cooldown
- Lesser and Greater Corruption cooldown
- Dark Lure cooldown
- Demonic and Sinister Offering cooldown
- Vile Vigour cooldown
- Spellbook Swap, showing the time left to cast a swapped spell

The RuneLite 'Timers' plugin doesn't track these at all. This plugin adds them:
- Dark Lure cooldown
- Demonic and Sinister Offering cooldown
- Vile Vigour cooldown

## Display options
### Timer text format
- Choose how the countdown is shown:
    - Seconds
    - Minutes
    - Game ticks

### Icon style
- **Dark Cooldown Icons** uses blacked-out icons for the Death Charge, Shadow Veil and
  Ward of Arceuus cooldowns, matching the standard 'Timers' plugin

### Layout
- **Separate Infoboxes** gives each timer its own group so they can be detached and positioned
  individually. When off (the default) every timer stays in one group that moves together

![format](https://imgur.com/Lrih4lv.png)

## Config Options
Config options exist for every visible part of the plugin:
- Infobox toggles for every spell timer and cooldown
- Custom text colour, plus a separate low-time colour
- Infobox priority (how high or low they sit in the infobox stack)
- Timer format: seconds, minutes, or game ticks
- Separate-infobox layout
- Full Death Charge on-player controls (anchor, offsets, reposition mode)

## Contact
- Submit an issue on this GitHub project
- Message Discord user: mantic.
    - Through the RuneLite Discord server or direct message

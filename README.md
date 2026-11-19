# Hunting Helper

One Fabric mod for hunting, Rift Puffs and Critter Safari in Hypixel SkyBlock. Open **`/hh`** to track mobs by name or type, time a Puff chain, and use the integrated Sparkling Mutuals suite with Hunter trades and the quota-aware missing list.

**Minecraft 26.2 · Java 25 · Fabric Loader 0.19.5+**

[Download the latest release](https://github.com/SleepCodeX/foragingPuckGrinding/releases/latest) · [Safari hunting features](docs/HUNTING.md)

![The HD Frost Hivethief opening into the matte charcoal dashboard](https://github.com/SleepCodeX/foragingPuckGrinding/releases/latest/download/menu-opening.gif)

## Puff Crux

Open **/hh → Puff Crux** or type **/hh puff** and switch the timer on. On the Rift Mountaintop, the HUD shows how many Puffs are loaded and the estimated time left for the oldest. The optional action-bar reminder appears at about 25 seconds left. Popping a chain clears its timers.

The estimate uses the roughly 100-second Puff lifetime and starts at the first sighting on your client. A Puff already on the mountain when you arrive can pop sooner. The timer runs locally, independently of the tracer switch. [Puff farming observations](https://hypixel.net/threads/puff-crux-farming-method.5830761/)

![Puff Crux countdown in the packaged Minecraft client](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.7.0/puff-hud.png)

## Custom trackers

Open **Custom trackers → Add tracker**, or go straight to the library with **`/hh custom`**. Rules can run together. Each row has an on/off switch, **Edit** and **Remove**. Choose a line color and a separate glow color, **Match line**, or **Off**.

| Mode | Example input | What it highlights |
| --- | --- | --- |
| Nametag | `Revenant Sycophant` | Mobs carrying that SkyBlock name, including decorated level and health labels |
| Nametag | `rev` | Names containing those letters, including Revenant Horror and its minibosses, plus the Tier V Atoned Horror |
| Nametag | `Pollendart` | The bee under a Pollendart label |
| Mob type | `wither_skeleton` | Every loaded wither skeleton in range, across different SkyBlock names |
| Mob type | `zombie` | Mobs with the exact Minecraft zombie type |

Nametag matching ignores capitalization and Minecraft formatting. The tracker reads armor stands, text displays and custom names on mobs. If several rules match one body, a matching nametag takes priority over a mob type. The first saved custom rule wins within the same mode.

![Custom library in Minecraft](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.6.2/library.png)

## Hunting presets and controls

**Vex · Blue Jay · Hivethief · Parched · Pollendart**. The last four presets use the Torrhus location indicator. Vex and custom rules work in all areas. The renderer checks client-loaded entities every tick across the entire effective render distance.

- **`/hh`** or **`/huntinghelper`** opens settings. You can also bind **Open tracker settings** in Minecraft Controls.
- **V** toggles all selected trackers. **F1** hides overlays.
- **All presets / Clear presets** controls the five hunting presets.
- **`/hh debug`** toggles detection summaries in the game log.

## Critter Safari (Sparkling Mutuals)

Sparkling Mutuals 4.3.7 is compiled into Hunting Helper and is the Safari backend: run tracking, HUDs, ESP, banners, party sync, auto-entry, warping, history, achievements and Hunting Box valuation are SM's own. Its settings open in Hunting Helper's style from **/hh** (Safari modes, Hunting & journal, Visuals & ESP, HUD layout, Alerts, Capture tools, Party & sync, Miria contests, Run history, SkyBlock helpers), from **/hh safari** and from **/sm**. Every SM setting is read from SM's own metadata, so new upstream settings show up without UI changes. SM's history and update screens use the same charcoal palette, and the update check follows Hunting Helper's releases.

Remove any standalone Sparkling Mutuals JAR from `mods` (the loader reports the conflict). `config/sparkling-mutuals` settings, history and PBs carry over. [Upstream source and local changes](docs/SM-UPSTREAM.md) · [Upstream feature guide](https://github.com/johnceoskyblock/sparkling-mutuals/tree/v4.3.7)

## Hunting features

CritterMod's tools that SM did not have now live inside it and read SM's run ledger:

- **Missing panel extras** on SM's own missing panel: quota species stay listed as **Gazer 2/4** until done (or tick after one catch with **Uniques only**), each row shows located creatures, and done species with creatures left appear under "also here".
- **Everything except Macaw done!** and **Everything Done!** announcements, each with a switch.
- **Hunter trades**: offers in a movable box and as waypoints, shared with party in CritterMod's format, retired when taken. Party prefixes are read in any capitalisation.
- **World marks**: Hideonwall perches, recatch pins, Troodon walls.
- **Floor-drop filter**: quest items only, hide quest items already collected, while items a Hunter trade wants stay highlighted.
- **Located creatures** for all 37 species: the run remembers every creature it has seen (loaded models, feeder birds, Rockmite mounds) through burrows, escapes and biome changes, and checks one off per catch. The missing panel shows "· 1 located", the run report has a Located column.
- **This run** (**/hh run**): who caught each species, capsules tried and escaped, shards and their value at both Bazaar prices.
- Per-event party/all-chat broadcasts, and a one-time import of CritterMod and Hunting Helper 1.6 journal settings and run history.

[Hunting features and settings](docs/HUNTING.md)

## Install

Put the Hunting Helper JAR in `mods` on Minecraft 26.2 with Fabric Loader. Replace the previous Hunting Helper JAR when updating. Your saved trackers and colors carry over.

[Hunting features](docs/HUNTING.md) · [Build and behavior](docs/DEVELOPMENT.md) · [Release downloads](https://github.com/SleepCodeX/foragingPuckGrinding/releases/latest)

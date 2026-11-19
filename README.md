# Hunting Helper

Hunting Helper draws tracers, matching boxes and colored model outlines for mobs in Hypixel SkyBlock. Open **`/hh`** for hunting presets, custom trackers, and the complete Sparkling Mutuals Safari suite.

![Hunting Helper's in-game settings opening](docs/media/1.5.0/menu-opening.gif)

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

![Custom library in Minecraft](docs/media/1.5.0/library.png)

## Hunting presets and controls

**Vex · Blue Jay · Hivethief · Parched · Pollendart**. The last four presets use the Torrhus location indicator. Vex and custom rules work in all areas. The renderer checks client-loaded entities every tick across the entire effective render distance.

- **`/hh`** or **`/huntinghelper`** opens settings. You can also bind **Open tracker settings** in Minecraft Controls.
- **V** toggles all selected trackers. **F1** hides overlays.
- **All presets / Clear presets** controls the five hunting presets.
- **`/hh debug`** toggles detection summaries in the game log.

## Sparkling Mutuals Safari suite

**/hh → All Safari settings** opens the full Sparkling Mutuals 4.3.7 interface. Closing it takes you back to Hunting Helper. The dashboard also takes you directly to modes, critter highlights, HUDs, capture tools, party settings, contests, history, and SkyBlock helpers. The original **/sm** commands, settings and history also work. Safari HUDs and ESP, party sync, capture tools, warping, history, achievements, contests, Hunting Box value and the other upstream tools are included.

Install the Hunting Helper JAR and Fabric API. Hunting Helper includes the matching Sparkling Mutuals and Fabric Language Kotlin JARs. The custom trackers and settings run locally. Party synchronization, live Bazaar/profile data and other online features use their respective services when active.

![Bundled Safari settings](docs/media/1.5.0/safari.png)

[Upstream feature guide](https://github.com/johnceoskyblock/sparkling-mutuals/tree/v4.3.7) · [Upstream release](https://github.com/johnceoskyblock/sparkling-mutuals/releases/tag/v4.3.7)

## Install

**Minecraft 26.2 · Java 25 · Fabric Loader 0.19.5+ · Fabric API 0.161.0+26.2**

[Download Hunting Helper 1.5.0](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.5.0/hunting-helper-1.5.0%2Bmc26.2.jar) and replace the previous tracer JAR in `mods`. Your settings carry over.

[In-game screenshots](docs/media/1.5.0) · [Build and behavior](docs/DEVELOPMENT.md) · [Release downloads](https://github.com/SleepCodeX/foragingPuckGrinding/releases/latest)

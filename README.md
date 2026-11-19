# Hunting Helper

One Fabric mod for hunting and Critter Safari in Hypixel SkyBlock. Open **`/hh`** to track mobs by name or type, customize through-wall highlights, see which Safari creatures remain in each area, review Hunter offers and per-player field reports, or open the complete Sparkling Mutuals suite.

**Minecraft 26.2 · Java 25 · Fabric Loader 0.19.5+**

[Download Hunting Helper 1.6.1](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.6.1/hunting-helper-1.6.1%2Bmc26.2.jar) · [In-game visual guide](docs/FIELD-JOURNAL.md)

![The HD Frost Hivethief opening into the matte charcoal dashboard](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.6.1/menu-opening.gif)

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

![Custom library in Minecraft](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.6.1/library.png)

## Hunting presets and controls

**Vex · Blue Jay · Hivethief · Parched · Pollendart**. The last four presets use the Torrhus location indicator. Vex and custom rules work in all areas. The renderer checks client-loaded entities every tick across the entire effective render distance.

- **`/hh`** or **`/huntinghelper`** opens settings. You can also bind **Open tracker settings** in Minecraft Controls.
- **V** toggles all selected trackers. **F1** hides overlays.
- **All presets / Clear presets** controls the five hunting presets.
- **`/hh debug`** toggles detection summaries in the game log.

## Sparkling Mutuals Safari suite

**/hh → All Safari settings** opens the full Sparkling Mutuals 4.3.7 interface. Closing it takes you back to Hunting Helper. The dashboard also takes you directly to modes, critter highlights, HUDs, capture tools, party settings, contests, history, and SkyBlock helpers. Party sync, auto-entry, warping, history, achievements and Hunting Box valuation use the original implementation. The upstream **/sm** commands and saved settings are available.

Install the Hunting Helper JAR on Fabric Loader. It includes Fabric API, Fabric Language Kotlin, the native field journal and the Sparkling Mutuals source. Sparkling Mutuals is compiled into Hunting Helper, so remove any standalone Sparkling Mutuals JAR from `mods` (the loader reports the conflict). Its `config/sparkling-mutuals` settings carry over. [Upstream source and local changes](docs/SM-UPSTREAM.md) The custom trackers and settings run locally. Party synchronization, live Bazaar/profile data and other online features use their respective services when active.

![Bundled Safari settings](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.6.1/safari.png)

[Upstream feature guide](https://github.com/johnceoskyblock/sparkling-mutuals/tree/v4.3.7) · [Upstream release](https://github.com/johnceoskyblock/sparkling-mutuals/releases/tag/v4.3.7)

## Full-clear tracking

Use **/fc** for the run HUD. **Species caught** credits the first capture of each creature, including a teammate's loot share. **Fully Collected** counts species cleared under the Safari completion rules. The current biome's **remaining** panel lists every unfinished species, with partially caught creatures first. Three Gazer catches show **Gazer · 1 remaining**. Catch the fourth and the row clears.

Open **/hh remaining** or **Field journal → Remaining critters** to inspect Forest, Cavern, Icy or Haunted from anywhere. Published spawn ranges narrow with loaded mobs and catch messages. Feeder, mound, wall and coin checks stay visible while their spawns are unresolved. The tracker also receives loot shares hidden from chat.

![Haunted remaining list in the packaged Minecraft client](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.6.1/full-clear-remaining.png)

## Safari field journal

Open **/hh → Field journal** or **/hh journal**. The nearest Hunter offers appear in a movable panel. Taken trades clear automatically, including compatible offers and completions received through party chat. **World markers** adds recatch pins, eight Hideonwall perches, and the three Icy Troodon walls. **Open field report** shows who caught each species, capsule attempts, and the spread between selling the run's shards now and listing them.

![Field journal settings in Minecraft](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.6.1/field-journal.png)

[Field journal controls and in-game screenshots](docs/FIELD-JOURNAL.md)

## Install

Put the Hunting Helper JAR in `mods` on Minecraft 26.2 with Fabric Loader. Replace the previous Hunting Helper JAR when updating. Your saved trackers and colors carry over.

[In-game screenshots](docs/FIELD-JOURNAL.md) · [Build and behavior](docs/DEVELOPMENT.md) · [Release downloads](https://github.com/SleepCodeX/foragingPuckGrinding/releases/latest)

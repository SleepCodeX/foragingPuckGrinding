# Development and source reference

Colored tracers and matching mob boxes for **Minecraft 26.2 / Fabric**.

| Tracker | Color | Detection |
| --- | --- | --- |
| Vex | Purple | "Vex" nametag, else loaded, living Vex entities |
| Blue Jay | Cyan | "Blue Jay" nametag, else untamed blue parrots, in Torrhus |
| Hivethief | Gold | "Hivethief" nametag, else Ghast entities, in Torrhus |
| Parched | Red | Mobs below a "Parched" nametag in Torrhus |
| Pollendart | Green | Bees below a "Pollendart" nametag in Torrhus |

Open **`/hh` → Hunting presets**. Toggle each mob separately, use **All presets / Clear presets**,
or use the master switch. **V** toggles all selected trackers and F1 hides the overlays.
Lines and boxes match their mob's color and remain visible through blocks.

## Full source and build

All Java source is public in [src](../src), including the [renderer](../src/client/java/dev/sleepcodex/foragingpuckgrinding/VexTracerRenderer.java),
[settings](../src/client/java/dev/sleepcodex/foragingpuckgrinding/TracerSettingsScreen.java),
and [configuration](../src/main/java/dev/sleepcodex/foragingpuckgrinding/TracerConfig.java).
The Gradle wrapper, build files, assets, licenses, unit tests and packaged-client tests
are included. There is no separate closed-source component.

Build from this checkout with **JDK 25**:

```sh
./gradlew build
```

Windows: `gradlew.bat build`. Output:

- `build/libs/hunting-helper-1.6.1+mc26.2.jar` — installable mod.
- `build/libs/hunting-helper-1.6.1+mc26.2-sources.jar` — Java source archive.

[GitHub Actions](https://github.com/SleepCodeX/foragingPuckGrinding/actions/workflows/build.yml)
builds the mod and source JARs from the visible commit and uploads both, alongside
SHA-256 checksums and a BUILD-INFO file identifying the exact source commit. The separate
client-test harness lives under `build/gametest` and is not shipped in the mod.

## Install

Use **Minecraft 26.2**, **Java 25** and **Fabric Loader 0.19.5+**. The release includes Fabric API 0.161.0+26.2. Put the installable JAR in `mods`, replacing the previous
Hunting Helper/Vex tracer JAR. Install only one version.

Tracers start off, with Vex selected. Each choice persists in
`config/foragingpuckgrinding.properties`. Older Vex / Blue Jay / Both configurations
migrate without automatically enabling new targets.

| Command | Effect |
| --- | --- |
| `/huntinghelper`, `/hh` | Open settings |
| `/hh debug`, `/huntinghelper debug` | Toggle a detection summary in `latest.log` every 5 seconds |
| `/vextracers menu` | Open the same settings |
| `/vextracers` | Toggle the master switch |
| `/vextracers on` / `/vextracers off` | Set the master switch |

## Behavior and implementation

Targets must already be loaded by Minecraft; this does not discover unloaded mobs.
Every built-in tracker except Vex requires a Torrhus Canyon/Heights/Springs location label
in sidebar or tab.

Hypixel names each SkyBlock mob with a custom-named armor stand about two blocks above it.
Each tick, every loaded entity across the complete render-distance footprint is checked.
Named stands are matched against the tracker names (whole words, formatting removed) and paired with the closest living non-player mob from 0.5 blocks above the tag to its body height plus 2.5 blocks below it,
within 1.5 blocks horizontally; Pollendart pairs only with bees. Without a mob below, the
nametag itself is tracked. Parched and Pollendart are nametag-only. Vex, Blue Jay and
Hivethief additionally keep type detection for mobs without a matching nametag: the
Ghast tracker uses the entity type, so other Ghasts in the same area also match. The type fallback excludes other
parrot colors, tamed birds and Allays; an explicit matching Hypixel nametag remains authoritative.

Discovery runs once per client tick; location text is cached. One native line render
submission draws the interpolated tracers and twelve-edge boxes. OFF and an empty
selection skip discovery and frame-array allocation. Changing worlds and removing
entities clear their overlays. It does not send gameplay commands or perform hunts.

CI covers configuration migration and every target combination, nametag parsing, box-edge
geometry, and real packaged-client controls/colors/boxes, through-wall rendering,
master/key/F1 toggles, persistence, location gating and removed/unrelated entities.
Live Hypixel, the full personal modpack and shader combinations are not reproduced.

## References and licenses

[Hivethief reference](https://hypixelskyblock.minecraft.wiki/w/Hivethief) confirms the
Ghast model in Torrhus. [Skyblocker's Torrhus detector](https://github.com/SkyblockerMod/Skyblocker/blob/74fc9ae67c8eddf3dbf46f3eb2a884cd853feded/src/main/java/de/hysky/skyblocker/skyblock/entity/glow/adder/TorrhusCanyonGlowAdder.java)
is the Blue Jay identity reference; no Skyblocker code or dependency is bundled.

The menu uses bundled mob symbols, matte charcoal surfaces and compact OneClient-inspired controls.
Adapted helpers retain their MIT notices; Nunito assets use OFL. See [LICENSE](../LICENSE), [NOTICE](../NOTICE)
and [licenses](../licenses).


## Render distance and storage

The renderer covers the entire loaded chunk square around the player, including
corner chunks and their far edges. It caches these bounds until the player changes
chunk or effective render distance. Minecraft and the server still determine which
entities are available; the mod cannot find entities whose data was never sent.

Candidate entity/type arrays grow only when needed and are reused between ticks.
Dead/world references are cleared promptly. Extraction uses cached classifications
and four range comparisons, then publishes privately owned immutable frame arrays
for the native renderer. There is no per-frame world scan or reuse of in-flight
render snapshots. The dashboard opens in 400 ms. A 1254 × 1254 transparent Frost Hivethief travels from the expanding badge to the header. The controls accept input throughout. Smaller icons leave room for independent line/glow color dropdowns; compact screens scroll the card body while header/footer stay fixed.

## Model outlines and color preferences

`line.<TARGET>` and `glow.<TARGET>` store named palette choices. Existing settings gain the original per-mob line colors and matching glow. `MATCH` resolves to the current line color; `OFF` disables only the custom glow. Changing a target, master switch, area, world or F1 visibility also gates glow.

Client mixins add outline colors during native entity-state extraction, permit matching mobs through section occlusion, and lift only the size-based entity distance cull for glowing mobs, so their glow reaches as far as the line; frustum checks remain. Glowing mobs also report as glowing to the client, which keeps the EntityCulling mod from hiding them behind walls. A second, see-through model pass adds a light tint (19% alpha) in the glow color under the outline. The native outline post-process draws the actual animated model silhouette through walls. Entity flags, scoreboard teams, server data and other mobs' vanilla glow are preserved.

Native tests inspect actual extracted outline colors behind a wall, independent palette persistence, Off/F1/master behavior, dropdown keyboard scrolling and every menu control, plus nametag pairing for Parched and Pollendart (only the bee under a Pollendart tag glows), the tint color, and glow on a bee beyond the vanilla distance cull. The opening frames use a deterministic clock with the real Minecraft renderer.

## 1.4.0 implementation notes

Hypixel nametag parsing and the existing pairing tolerance are preserved. A reusable index groups **all** loaded living mobs into 4-block horizontal columns. Looking in adjacent columns is only a nametag/body association optimization, not a range cap; there is no vertical bucketing or height limit. Clearing the index each tick and on world/disabled transitions removes stale references. The current build prioritizes a living mounted body, then horizontal alignment, 3D distance and entity ID.

The shared `MenuTheme` defines charcoal surfaces, cool gray text and restrained accents. At a 640 × 360 GUI, the hunting cards and custom library entry fit without scrolling; narrower windows keep the scrollable list and keyboard reveal. No extra blur shader or continuous animation loop is installed.

Debug render counters and location-change logs run only while diagnostics are enabled. The native client tests cover disabled diagnostics, the new trackers' F1/location gates, moving nametags, entity removal, full-distance bee rendering, and normal/large/compact menus. EntityCulling interoperability follows its glow exemption but a live third-party modpack is not part of the automated test.

## Custom rules in 1.5.0

`custom.ids` lists stable UUIDs in creation order. Each rule stores its mode, query, enabled state, line and glow. The config writes a new snapshot atomically before publishing it to the tracker. The reader skips damaged custom entries independently. Older configs start with an empty library.

Each tick indexes all loaded living, non-player, non-stand bodies. A named stand or text display uses its living vehicle when available, then looks in nearby spatial buckets for the best horizontally aligned body in the pairing window. Vertical proximity and entity ID settle ties. Unpaired labels can receive a line and box. Direct custom names on bodies are also eligible. Identically positioned, unmounted bodies with indistinguishable labels remain ambiguous.

Case-insensitive substring matching and a narrow alias for **Atoned Horror** cover short queries such as **rev**. Exact registry IDs handle type matching. The registration order of matches is custom name, built-in name, custom type, built-in type. Per-tick discovery and identity checks keep deleted or edited rules out of render lookups. The type index and normalized queries are rebuilt when the saved rule list changes.

The packaged game checks exercise the library, editor and persistence with multiple active rules, close bees, a text display, an armor stand above an Atoned Horror fixture, a direct miniboss name and two differently named wither skeletons.

Reference implementations consulted for the distinction between named and typed selectors and for floating-label association include [Athen MobHighlight](https://github.com/skies-starred/Athen/blob/4168236a9fdf9ad50947fa6df12f9c53161512de/src/main/kotlin/foo/starred/athen/modules/impl/render/highlight/MobHighlight.kt) and [ESP's label manager](https://github.com/noname-mods/ESP/blob/54d7092e0c3c3e6fe29cc4a441204a61cdd99501/src/main/java/com/esp/core/EspManager.java). Hunting Helper implements these rules in its own discovery and rendering code. [Revenant Horror tiers](https://hypixelskyblock.minecraft.wiki/w/Revenant_Horror) documents the Tier V display name.

## Bundled Safari suite

Sparkling Mutuals 4.3.7 is compiled from source in `src/client/kotlin` and `src/client/java/net` (26.2 variant, packages and asset namespace unchanged). Its unit tests run in `:test`. The Gradle build checks the published SHA-256 values of Fabric API 0.161.0+26.2 and Fabric Language Kotlin 1.14.1+kotlin.2.4.20 and nests both unchanged with Fabric Loader's jar-in-jar metadata, together with HM API 1.0.4+26.2. The upstream sync, chat and API behavior is unchanged. Local changes are marked `// Hunting Helper:`; see [SM-UPSTREAM.md](SM-UPSTREAM.md). Core tracking requires no account or backend beyond the loaded Minecraft world.

The native packaged-client test opens Safari settings through Hunting Helper and returns to the original menu. It runs with only the distributed Hunting Helper JAR and the game-test harness. Live party synchronization and external services require an eligible session and peers. See [Sparkling Mutuals source](https://github.com/johnceoskyblock/sparkling-mutuals/tree/2b361320476d7e6e6a42390d4b1c3b7ab5e20731) and [NOTICE](../NOTICE).

## Safari field journal

The Minecraft 26.2 journal is compiled directly into the client source set. The packaged test loads Fabric API from Hunting Helper, opens its screens, exercises filtered chat, records a trade and removes it on completion. It reads the bundled Safari quote snapshot for both order-book prices. Each release now publishes native screenshots from the same production test as the distributed JAR. [Journal controls and upstream attribution](FIELD-JOURNAL.md)

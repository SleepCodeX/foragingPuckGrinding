# Development and source reference

Colored tracers and matching mob boxes for **Minecraft 26.2 / Fabric**.

| Tracker | Color | Detection |
| --- | --- | --- |
| Vex | Purple | Loaded, living Vex entities |
| Blue Jay | Cyan | Untamed blue parrots in Torrhus |
| Hivethief | Gold | Ghast entities in Torrhus |

Open **`/huntinghelper`**. Toggle each mob separately, use **Select all / Clear all**,
or use the master switch. **V** toggles all selected trackers; F1 hides the overlays.
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

- `build/libs/hunting-helper-1.2.0+mc26.2.jar` — installable mod.
- `build/libs/hunting-helper-1.2.0+mc26.2-sources.jar` — Java source archive.

[GitHub Actions](https://github.com/SleepCodeX/foragingPuckGrinding/actions/workflows/build.yml)
builds the mod and source JARs from the visible commit and uploads both, alongside
SHA-256 checksums and a BUILD-INFO file identifying the exact source commit. The separate
client-test harness lives under `build/gametest` and is not shipped in the mod.

## Install

Use **Minecraft 26.2**, **Java 25**, **Fabric Loader 0.19.5+** and
**Fabric API 0.161.0+26.2**. Put the installable JAR in `mods`, replacing the previous
Hunting Helper/Vex tracer JAR. Install only one version.

Tracers start off, with Vex selected. Each choice persists in
`config/foragingpuckgrinding.properties`. Older Vex / Blue Jay / Both configurations
migrate without automatically enabling the new Ghast tracker.

| Command | Effect |
| --- | --- |
| `/huntinghelper` | Open settings |
| `/vextracers menu` | Open the same settings |
| `/vextracers` | Toggle the master switch |
| `/vextracers on` / `/vextracers off` | Set the master switch |

## Behavior and implementation

Targets must already be loaded by Minecraft; this does not discover unloaded mobs.
Blue Jay and Ghast tracking require a Torrhus Canyon/Heights/Springs location label
in sidebar or tab. The Ghast tracker uses the entity type, so other Ghasts in the
same area also match. Other parrot colors, tamed birds and Allays are excluded.

Discovery runs once per client tick; location text is cached. One native render
submission draws the interpolated tracers and twelve-edge boxes. OFF and an empty
selection skip discovery and frame-array allocation. Changing worlds and removing
entities clear their overlays. It does not send gameplay commands or perform hunts.

CI covers configuration migration and all eight target combinations, box-edge
geometry, and real packaged-client controls/colors/boxes, through-wall rendering,
master/key/F1 toggles, persistence, location gating and removed/unrelated entities.
Live Hypixel, the full personal modpack and shader combinations are not reproduced.

## References and licenses

[Hivethief reference](https://hypixelskyblock.minecraft.wiki/w/Hivethief) confirms the
Ghast model in Torrhus. [Skyblocker's Torrhus detector](https://github.com/SkyblockerMod/Skyblocker/blob/74fc9ae67c8eddf3dbf46f3eb2a884cd853feded/src/main/java/de/hysky/skyblocker/skyblock/entity/glow/adder/TorrhusCanyonGlowAdder.java)
is the Blue Jay identity reference; no Skyblocker code or dependency is bundled.

The original axe icon and Trevor-inspired navy/lavender menu are included. Adapted
Trevor helpers use MIT; Nunito assets use OFL. See [LICENSE](../LICENSE), [NOTICE](../NOTICE)
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
render snapshots. The menu has a 160 ms fade and no blocking intro.

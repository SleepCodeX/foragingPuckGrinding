# Hunting Helper

A small client-side Fabric mod for **Minecraft 26.2**, with camera-anchored purple
tracers and a compact, Trevor-inspired menu.

## Install and use

Requires **Java 25**, **Fabric Loader 0.19.5 or later**, and
**Fabric API 0.161.0+26.2 or later for Minecraft 26.2**.

1. Close Minecraft.
2. Replace the earlier Vex tracer jar with `hunting-helper-1.1.0+mc26.2.jar` in the instance's `mods` folder. Install only one version.
3. Launch the Fabric 26.2 instance.
4. Use **`/huntinghelper`** to open the menu.

The menu has exactly two settings:

- **Tracers:** master on/off switch.
- **Targets:** click to cycle **Vex / Blue Jay / Both**. Both renders both target types simultaneously.

The original axe badge and brief 650 ms opening splash introduce the menu. Controls
remain responsive throughout. Rounded navy cards, lavender accents and Nunito text
follow the user's [Trevor menu](https://github.com/SleepCodeX/trevorRngMod/blob/dac1b7e1e68fc3f803be69db5e16a97a85e4cebf/DESIGN.md).

Tracers start **OFF**, with **Vex** selected. Both settings persist across restarts
in `config/foragingpuckgrinding.properties`. The existing config and mod ID are kept
so upgrading preserves the previous enabled setting.

**V** still toggles the master switch. Rebind it in **Options > Controls > Key Binds >
Hunting Helper** if needed. Existing commands are preserved:

| Command | Effect |
| --- | --- |
| `/huntinghelper` | Open the two-setting menu |
| `/vextracers menu` | Open the same menu |
| `/vextracers` | Toggle the master switch |
| `/vextracers on` | Enable |
| `/vextracers off` | Disable |

These commands are client-side and require no server permissions.

## Target identification and behavior

**Vex** selects all living, loaded Vex entities, preserving the original feature.
**Blue Jay** selects untamed blue-variant parrots in Torrhus Canyon, using the
[entity identity verified in Skyblocker's Torrhus detector](https://github.com/SkyblockerMod/Skyblocker/blob/74fc9ae67c8eddf3dbf46f3eb2a884cd853feded/src/main/java/de/hysky/skyblocker/skyblock/entity/glow/adder/TorrhusCanyonGlowAdder.java).
The client must expose a Torrhus Canyon, Torrhus Heights, or Torrhus Springs location
label in the sidebar or tab list. This prevents matching blue parrots in other
islands, ordinary parrot colors, tamed birds or Allays. Location labels are checked
at most twice per second and cleared when changing worlds. A live Hypixel session
has not been used to validate the current server's labels.

- Purple, two-pixel lines remain visible through blocks and reach all selected
  loaded targets, including targets outside the current view.
- The horizontal cutoff is effective render distance in chunks times 16 blocks,
  measured from the active camera. Height alone does not exclude a loaded target.
- Positions interpolate each frame in first and third person. The line origin is
  two blocks in front of the camera, anchoring it at the crosshair without near clipping.
- F1 hides lines. Dead, removed and unloaded entities stop drawing; old-world
  candidate lists are never used to render a new world.
- Only entities sent to the client can be tracked.

## Performance

Target discovery runs once per client tick, rather than rescanning the world every
frame. Location matching stays on the tick side and only runs when a Blue Jay mode
is enabled. Each rendered frame interpolates the selected targets into one private
coordinate array and submits the lines together. Disabling tracers clears the
candidate list and skips entity discovery and line-buffer allocation. No FPS gain
or full-modpack performance measurement is claimed.

## Build and verify

Use JDK 25 and the real Fabric Loom toolchain. Minecraft 26.2 is unobfuscated.

```sh
./gradlew build
./gradlew runClientGameTest
./gradlew runProductionClientGameTest
```

On Windows, use `gradlew.bat`. Installable and source jars are in `build/libs/`.
Nine unit tests cover range boundaries, location matching, target cycling and
independent persistence/migration of both settings. GitHub Actions builds and runs
unit tests, validates the wrapper, and uploads the mod and source jars.

The graphical tests use isolated flat worlds. They exercise the actual menu,
master switch, V key, all three target modes, settings persistence, through-wall
rendering, target removal, exclusion of red parrots/Allays, and exclusion of blue
parrots outside Torrhus. Menu captures cover 854x480 and 1280x720 windows.
The production test loads the packaged jar. Screenshots/logs are written under
`build/run/clientGameTest/` or `build/run/productionClientGameTest/`.
The test mod is excluded from the installable jar.

## References and licensing

SkyHanni's [Fast Fairy Souls Pathfinder](https://github.com/SkyHanniStudios/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/misc/FastFairySoulsPathfinder.kt)
and camera-anchored line rendering were the original tracer visual reference.
The tracer implementation is original; no SkyHanni or Skyblocker code/assets or
dependency is bundled.

Menu font rendering and rounded texture helpers are adapted from the user's
MIT-licensed Trevor mod. Its circle texture and OFL Nunito atlas/metrics are
included with their licenses. The Hunting Helper axe badge is original.
See `NOTICE`, `licenses/Trevor-MIT.txt` and the bundled `NUNITO-OFL.txt`.

Rendering uses Fabric 26.2 extraction/submission events and a depth-disabled
Minecraft line pipeline, with no rendering mixins or raw OpenGL calls. See the
[Fabric world-rendering guide](https://docs.fabricmc.net/develop/rendering/world).
Original code is MIT; the Gradle wrapper is Apache-2.0.

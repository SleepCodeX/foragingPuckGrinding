# Foraging Puck Grinding - Vex Tracer

A small client-side Fabric mod for **Minecraft 26.2**. It draws purple, two-pixel
lines from the crosshair/viewpoint to the center of every living, loaded Vex
within the client's effective render distance, including through blocks.

## Install and use

Requires **Java 25**, **Fabric Loader 0.19.5 or later**, and
**Fabric API 0.161.0+26.2 or later for Minecraft 26.2**.

1. Close Minecraft.
2. Put `foragingpuckgrinding-1.0.0+mc26.2.jar` in the instance's `mods` folder.
3. Launch the Fabric 26.2 instance.
4. Press **V** to toggle tracers, or use **`/vextracers`**.

Tracers start **OFF**. The setting persists across restarts in
`config/foragingpuckgrinding.properties`. Rebind the key in **Options > Controls >
Key Binds > Foraging Puck Grinding** if V is used by another mod.

Commands are client-side and require no server permissions:

| Command | Effect |
| --- | --- |
| `/vextracers` | Toggle |
| `/vextracers on` | Enable |
| `/vextracers off` | Disable |

## Behavior

- Tracks all living Vex entities loaded on the client, including those outside
  the current view and behind walls; it does not use entity frustum culling.
- The horizontal cutoff is the effective render distance in chunks times 16
  blocks, measured from the camera. Changing render distance takes effect next
  frame. Height alone does not exclude a loaded Vex.
- Interpolates moving Vex positions each frame. Dead, removed, or unloaded Vexes
  stop contributing a line; the mod keeps no entity cache across worlds.
- Uses the active camera in first and third person. The origin is two blocks in
  front of the camera to anchor the line at the crosshair without near clipping.
- F1 hides the lines along with the HUD.
- Only actual Vex entities are tracked. The mod cannot detect entities a server
  has not sent to the client, or server objects represented by other entity types.

## Build and verify

Use JDK 25. No mapping download or remapping is needed for Minecraft 26.2.

```sh
./gradlew build
```

On Windows, use `gradlew.bat build`. The installable jar and source jar are in
`build/libs/`. Five unit tests cover render-distance boundaries and persisted
toggle settings. GitHub Actions builds and runs those tests on pushes and pull
requests, then uploads the jars as an artifact.

An optional automated client test uses its own temporary flat world:

```sh
./gradlew runClientGameTest
```

It creates Vexes behind a stone wall, verifies rendered purple pixels, tests the
on/off commands and V keybind, and checks that removing Vexes removes the lines.
It requires a graphical desktop and graphics support for Minecraft 26.2.
Screenshots and logs are written under `build/run/clientGameTest/`.
The test mod is excluded from the installable jar.

## Rendering reference and licensing

The requested visual reference was SkyHanni's
[Fast Fairy Souls Pathfinder](https://github.com/SkyHanniStudios/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/misc/FastFairySoulsPathfinder.kt).
Its [camera-anchored lines](https://github.com/SkyHanniStudios/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/utils/render/WorldRenderUtils.kt)
and [line rendering](https://github.com/SkyHanniStudios/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/utils/render/LineDrawer.kt)
were inspected for appearance and modern rendering patterns. This is an original
implementation; SkyHanni code and assets are not bundled and SkyHanni is not a
dependency. See `NOTICE`.

Rendering uses Fabric's Minecraft 26.2 extraction and submission events, an
immutable list of camera-relative lines attached to each frame's render state,
and a depth-disabled Minecraft line pipeline. It adds no rendering mixins or
raw OpenGL calls. See the official
[Fabric world-rendering guide](https://docs.fabricmc.net/develop/rendering/world).

The mod's original code is licensed under MIT. The Gradle wrapper is Apache-2.0.

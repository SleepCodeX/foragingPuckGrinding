# Sparkling Mutuals source in Hunting Helper

Hunting Helper compiles the complete Sparkling Mutuals (SM) client from source. It no longer nests the upstream SM JAR.

| | |
| --- | --- |
| Upstream | https://github.com/johnceoskyblock/sparkling-mutuals |
| Tag | `v4.3.7` |
| Commit | `2b361320476d7e6e6a42390d4b1c3b7ab5e20731` |
| License | CC0-1.0 ([licenses/sparkling-mutuals/LICENSE-CC0-1.0.txt](../licenses/sparkling-mutuals/LICENSE-CC0-1.0.txt)), plus the third-party notices in [licenses/sparkling-mutuals](../licenses/sparkling-mutuals) |
| Minecraft variant | 26.2 |

## Where it lives

| Upstream path | Hunting Helper path |
| --- | --- |
| `src/main/kotlin/net/johnceo/sparklingmutuals/**` | `src/client/kotlin/net/johnceo/sparklingmutuals/**` |
| `src/main/java/net/johnceo/sparklingmutuals/**` | `src/client/java/net/johnceo/sparklingmutuals/**` |
| `src/main/resources/**` except `fabric.mod.json` | `src/client/resources/**` (assets `sparkling-mutuals`, `defaults/`, `safari-waypoint-sites.json`, `sparkling-mutuals.mixins.json`) |
| `src/test/kotlin/**`, `src/test/resources/**` | `src/test/kotlin/**`, `src/test/resources/**` |
| `licenses/*`, `LICENSE` | `licenses/sparkling-mutuals/*`, `licenses/sparkling-mutuals/LICENSE-CC0-1.0.txt` |

Packages (`net.johnceo.sparklingmutuals.*`), the asset namespace (`sparkling-mutuals`), the mixin config and the config folder (`config/sparkling-mutuals/`) are unchanged. Existing SM settings, history and PBs carry over.

These upstream parts are not copied: `backend/`, `buildSrc/` and the release tooling, `scripts/`, `tools/`, `docs/`, `.devAuth/`, the Stonecutter setup and the 26.1.2 and 26.3 variants.

`fabric.mod.json` is merged into Hunting Helper's. SM's `main` entrypoint (Kotlin adapter) and `sparkling-mutuals.mixins.json` are registered there. The `sparkling-mutuals:developer` custom value is `false`. `"breaks": {"sparkling-mutuals": "*"}` turns a standalone SM JAR in `mods` into a clear loader error instead of duplicate classes. SM has no access widener.

Build: Kotlin JVM plugin `2.4.20` (`kotlin_version`), JVM target 25. `fabric-language-kotlin` is an `implementation` dependency and is still packaged as the intact upstream JAR (`kotlinBundle`). `net.azureaaron:hm-api:1.0.4+26.2` (the version the SM 4.3.7 26.2 JAR nested) is `include`d from `maven.azureaaron.net`. The `test` Kotlin compilation is associated with `client`, so the upstream unit tests compile against `internal` members as they do upstream.

## How the 26.2 source was produced

Upstream checks in the Stonecutter `vcsVersion` 26.1.2. To reproduce:

```sh
git clone --branch v4.3.7 https://github.com/johnceoskyblock/sparkling-mutuals.git sm
cd sm
./gradlew "Set active project to 26.2"   # rewrites the versioned comments in place
./gradlew ":26.2:build"                  # sanity check: upstream 26.2 JAR and tests pass
```

Then strip the Stonecutter comments, keeping only the active (26.2) branch. Drop every `//? ...` and `*///? ...` marker line, plus each inactive `/* ... */` block that directly follows an opening `//? if ... {` or `} else {` marker. 19 files contained such markers (16 main, 3 test). The result was diffed against the upstream 26.2 JAR. The Hunting Helper JAR contains the same 1245 `net/johnceo/sparklingmutuals` classes and the same assets.

## Local modifications

Each modification is marked with a `// Hunting Helper:` comment (`#` in the defaults properties). `git grep -n "Hunting Helper:" -- src/client/kotlin src/client/java/net src/client/resources` lists them. Larger additions live in new files, so an upstream merge rarely conflicts.

### New files (Hunting Helper only)

| Path | Contents |
| --- | --- |
| `hunting/*.kt` (Kotlin) | CritterMod features built on SM: `HuntingConfig` (`sparkling-mutuals-hunting.properties`), `HuntingSettings` (the **Hunting** settings page), `HuntingCompletion` + `MilestoneTracker` (one done-rule for the second HUD and the run announcements), `HuntingHuds` (second HUD and trades box), `HuntingRuntime` (chat reader, capsule attempts, encounter broadcasts, announcements), `HuntingImport` (CritterMod / journal import), `PlayerChat` (case-insensitive player-line rules), `QuestDropFilter` (floor-drop quest filter), `HuntingOutput` (CritterMod double-install guard, chat queue), `HuntingFeatures` (startup). |
| `hunting/*.java` (`src/client/java/net/...`) | Ported CritterMod world logic: `TraderParser`, `TraderWatch`, `HideonwallSpots`, `RecatchSpots`, `TroodonWalls`, `CritterEntities`, `Visibility`, `Markers`, `WaypointRenderer`, `Where` (adapter to SM's location and roster). |
| `config/SettingsScreens.kt` | Factory that opens the settings workspace. Hunting Helper installs its own screen; upstream `SafariConfigScreen` is the fallback. |
| `src/test/kotlin/.../hunting/*Test.kt` | Unit tests for the files above. |

### Edited SM files

| File | Change | Why |
| --- | --- | --- |
| `SparklingMutuals.kt` | `UPSTREAM_VERSION = "4.3.7"` | No `sparkling-mutuals` mod container |
| `debug/SafariDeveloper.kt` | Developer flag from the `foragingpuckgrinding` container; snapshot uses `UPSTREAM_VERSION` | Same |
| `config/ModUpdates.kt` | Checks `SleepCodeX/foragingPuckGrinding` releases against the installed Hunting Helper version and asset `hunting-helper-<v>+mc<mc>.jar` | Never point players at upstream SM builds |
| `config/ReleaseUpdateChecker.kt` | Optional `asset` parameter (default = upstream name) | Used by `ModUpdates` |
| `party/PartyManager.kt` | `requestPartyInfo` / `refreshPartyInfo` only on Hypixel | No party queries in test worlds |
| `safari/SafariCatch.kt` | Loot-share parse handles rank prefixes and catchers named like a species | Correct catch counts |
| `safari/SafariTracking.kt` | `receive` is public | Canceled chat (GAME_CANCELED) is forwarded by `HuntingChat` |
| `safari/SafariRun.kt` | `populationOf(name)` reads the private `knownPopulation` high-water mark | Located counts for all species (replaces upstream Hunting Helper #10's `SafariPopulationAccess` mixin) |
| `history/RunHistory.kt` | `receiveCanceled(raw)` and `liveShards(run)` | Canceled shard lines count; run report reads live shards |
| `commands/PartyCommand.kt`, `alerts/WarpListRanks.kt` | Channel prefix (`Party >`, `guild >`) matched case-insensitively | Same fix as CritterMod PR #5 |
| `commands/SparklingCommand.kt`, `contest/ContestGui.kt`, `config/SafariSettings.kt` (runnable 13) | Open settings through `SettingsScreens` | `/sm`, `/sm config`, HUD-editor right-click open Hunting Helper's screen |
| `config/SafariSettings.kt` | `@Category("Hunting") hunting = HuntingSettings()` appended (page 18); `apply()` calls `hunting.apply()` | Hunting settings in SM's metadata |
| `config/SafariConfigScreen.kt`, `config/SettingsCatalog.kt` | Page 18 placed under Safari → Hunting | Upstream screen stays usable |
| `hud/HudLayout.kt` | `SafariHud.HUNTING_MISSING`, `SafariHud.HUNTING_TRADES` | Movable in SM's HUD editor |
| `config/AppearanceConfig.kt` | `hunting_missing` / `hunting_trades` panel styles; upstream theme defaults migrate to the new theme | Same |
| `config/ConfigLayout.kt` | `HudConfigLinks` targets for the two HUDs | Right-click in the HUD editor |
| `contest/ContestGui.kt` | Adds `HuntingHuds.previews()` | Editor previews |
| `config/FloorDropToggleSettings.kt` | "Quest items only" / "Hide collected quest items"; `apply()` skips non-drop fields | Floor-drop filter |
| `safari/resources/SafariFloorDrops.kt` | `selected()` asks `QuestDropFilter.decide` first | Same |
| `config/SafariTheme.kt` | Hunting Helper charcoal colours (upstream values in the comment), `UPSTREAM_BORDER/BACKGROUND` | One look across HUDs and chat |
| `config/SettingsPalette.kt` | Palette 0 is "Hunting Helper" (was "Default"); `HuntingFeatures` selects it once | SM screens use HH colours |
| `history/ui/HistoryScreen.kt`, `history/ui/HistoryLogScreen.kt` | Charcoal frame, tabs, buttons, scroll thumb | Same |
| `src/client/resources/defaults/sparkling-mutuals.properties` | Fresh-install styles for the two hunting panels | Match SM's fresh panel look |

Reverted from phase 1 (back to upstream): `safari/ui/SafariPanels.kt` (full-clear hooks, replaced by the second HUD), `history/BazaarPrices.kt` and `safari/structures/BeeNests.kt` (only the removed field journal used them).

### Upstream tests adapted

| Test | Change |
| --- | --- |
| `config/NativeSettingsTest` | 19 pages (+ Hunting) |
| `config/SafariCustomizationTest`, `safari/completion/CommunalCompletionTest` | Expected panel background is the new `SafariTheme.BACKGROUND` |
| `safari/integration/SafariFeatureIntegrationTest` | Opt-in hunting switches (`uniquesOnly`, Hideonwall, recatch, Troodon marks) listed as default-off exceptions |

## Merging a future SM release

1. Clone the new tag, run `./gradlew "Set active project to 26.2"` (or the Minecraft version Hunting Helper targets then) and `./gradlew ":26.2:build"`.
2. Strip the Stonecutter comments as above.
3. Diff the result against the previous stripped tag, not against Hunting Helper. Reproduce the old one from `v4.3.7` the same way. Apply that diff to `src/client/{kotlin,java}`, `src/client/resources`, `src/test` and `licenses/sparkling-mutuals`. Alternatively, copy the new tree over and reapply the `// Hunting Helper:` changes from the table.
4. Compare upstream `fabric.mod.json`, `build.gradle.kts` and `versions/<mc>/gradle.properties` for new entrypoints, mixin configs, dependencies, `hm_api_version`, Kotlin and fabric-language-kotlin versions. Mirror them in Hunting Helper's `fabric.mod.json`, `gradle.properties` and `build.gradle`.
5. Update `UPSTREAM_VERSION`, this file (tag, commit) and `NOTICE`.
6. Re-check the `Edited SM files` table: re-apply each change, keep `SafariSettings`' Hunting page last, and add any new SM settings page to `AREAS` in `SafariSettingsScreen` (unknown pages appear under **More** until then).
7. Run `./gradlew build runClientGameTest`. The upstream unit tests run in `:test`.

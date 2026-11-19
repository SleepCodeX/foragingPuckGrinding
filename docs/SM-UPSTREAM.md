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

Each modification is marked with a `// Hunting Helper:` comment. `git grep -n "Hunting Helper:" -- src/client/kotlin src/client/java/net` lists them.

| File | Change | Replaces |
| --- | --- | --- |
| `SparklingMutuals.kt` | `UPSTREAM_VERSION = "4.3.7"` constant | (new) |
| `config/ModUpdates.kt` | `installed` uses `UPSTREAM_VERSION`. There is no `sparkling-mutuals` mod container. | upstream lookup, which would fall back to `"dev"` |
| `debug/SafariDeveloper.kt` | Developer flag read from the `foragingpuckgrinding` container. Snapshot `modVersion` uses `UPSTREAM_VERSION`. | upstream lookups, which would crash or read nothing |
| `history/BazaarPrices.kt` | `cache` is `internal` (was `private`). `cache` and `guidanceQuotes` get JVM names `marketCache()` and `guidanceQuotes()` for the Java field journal. | `MarketCacheAccess` mixin |
| `safari/structures/BeeNests.kt` | `clicked` is public | `NestSurveyAccess` mixin |
| `safari/SafariTracking.kt` | `receive` is public | `SafariCatchReceiver` mixin |
| `party/PartyManager.kt` | `requestPartyInfo` / `refreshPartyInfo` return early unless `hmapi Utils.isOnHypixel()` | `SafariPartyConnectionMixin` + `HypixelSessionAccess` mixins |
| `safari/SafariCatch.kt` | Loot-share parse handles rank prefixes and usernames that are species names | `SafariLootShareMixin` |
| `safari/ui/SafariPanels.kt` | `fullClearProgress` / `fullClearMissing` hooks. In full-clear mode Hunting Helper's `FullClearProgress` replaces the progress and missing panels. `VexTracerClient` installs them. They are unset in unit tests, so upstream panel tests still pass. | `FullClearPanelsMixin` |

Hunting Helper code calls these directly. `VexTracerClient`, `journal/client/BazaarPrices`, `journal/client/HoneybugRelease` and `FieldJournalGameChecks` use them.

## Merging a future SM release

1. Clone the new tag, run `./gradlew "Set active project to 26.2"` (or the Minecraft version Hunting Helper targets then) and `./gradlew ":26.2:build"`.
2. Strip the Stonecutter comments as above.
3. Diff the result against the previous stripped tag, not against Hunting Helper. Reproduce the old one from `v4.3.7` the same way. Apply that diff to `src/client/{kotlin,java}`, `src/client/resources`, `src/test` and `licenses/sparkling-mutuals`. Alternatively, copy the new tree over and reapply the `// Hunting Helper:` changes from the table.
4. Compare upstream `fabric.mod.json`, `build.gradle.kts` and `versions/<mc>/gradle.properties` for new entrypoints, mixin configs, dependencies, `hm_api_version`, Kotlin and fabric-language-kotlin versions. Mirror them in Hunting Helper's `fabric.mod.json`, `gradle.properties` and `build.gradle`.
5. Update `UPSTREAM_VERSION`, this file (tag, commit) and `NOTICE`.
6. Run `./gradlew build runClientGameTest`. The upstream unit tests run in `:test`.

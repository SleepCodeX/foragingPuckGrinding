# Hunting Helper 1.5.0

The HD ivory-and-honey Frost Hivethief opens into a matte charcoal dashboard. Type **`/hh`** for hunting presets, custom trackers and the complete Sparkling Mutuals Safari suite.

![Frost Hivethief opening the in-game dashboard](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.5.0/menu-opening.gif)

- **Custom trackers** can run together. Add, edit, toggle or remove individual rules, each with its own line and glow colors.
- **Nametag** reads floating armor stands, text displays and direct mob names. Enter **Revenant Sycophant** for the miniboss or **rev** to include Revenant names and the Tier V **Atoned Horror**.
- **Mob type** accepts IDs such as **wither_skeleton** and **zombie** across different SkyBlock names. Tracking checks all loaded entities across the effective render distance every tick.
- **Critter Safari** has eight dashboard shortcuts and **All Safari settings**. The full Sparkling Mutuals 4.3.7 mod, including party sync, and Fabric Language Kotlin ship inside the Hunting Helper JAR.
- **Existing players** keep their five hunting presets, saved colors and toggles. **V** toggles tracers. A separate settings shortcut can be assigned in Minecraft Controls.

**Install** the Hunting Helper JAR with Fabric API for Minecraft 26.2, Fabric Loader 0.19.5+ and Java 25. Downloadable screenshots, SHA-256 checksums and a source archive accompany the release.

# Hunting Helper 1.4.0

A matte charcoal redesign and five Hypixel hunting trackers.

![Matte charcoal settings](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.4.0/settings.png)

- **Parched** and **Pollendart** join Vex, Blue Jay and Hivethief, each with independent Line and Glow colors.
- Smaller icons and an adaptive two-row layout fit all five trackers at the usual GUI size. Compact windows retain scrolling and keyboard navigation.
- Hypixel nametags remain authoritative. Every loaded entity across the full render distance is checked on every client tick; a reusable horizontal index speeds up pairing without adding a tracking radius or a height limit.
- Through-wall model outlines gain a light matching tint and full-distance rendering for small mobs. Glow **Off** disables both outline and tint.
- **Match line**, separate colors, saved settings, the Hivethief opening fade, **V**, and **F1** remain available.
- **`/hh`** opens settings; **`/hh debug`** toggles diagnostics. Debug logging and render counters stay idle while diagnostics are off.

![All five trackers through a wall](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.4.0/trackers.png)

Thanks to [evolveperformance](https://github.com/SleepCodeX/foragingPuckGrinding/pull/5) for the new trackers, nametag detection and glow improvements.

**Install:** Minecraft 26.2, Java 25, Fabric Loader 0.19.5+ and Fabric API 0.161.0+26.2. Replace the older Hunting Helper JAR.

Tests cover unit/config migration, the packaged Minecraft client, small and large menu layouts, palette controls, name pairing, movement, distance, tint, location/F1/master gates and entity removal. Live Hypixel and the user's complete modpack are not reproduced by CI.

# Hunting Helper 1.3.0

![Hivethief opening fade and smaller tracker cards](https://raw.githubusercontent.com/SleepCodeX/foragingPuckGrinding/v1.3.0/docs/media/1.3.0/menu-opening.gif)

Smaller icons, wider card spacing and scrollable per-mob color dropdowns. A Hivethief fades into the settings header as the menu opens.

![Through-wall model glow, matching boxes and tracers](https://raw.githubusercontent.com/SleepCodeX/foragingPuckGrinding/v1.3.0/docs/media/1.3.0/trackers.png)

Native model glow shows tracked mobs through walls. Set **Line** and **Glow** independently; glow also offers **Match line** and **Off**. All choices persist.

Open **`/huntinghelper`**; **V** toggles your selection and **F1** hides overlays. Full loaded render-distance coverage is preserved.

**Install:** Minecraft 26.2, Java 25, Fabric Loader 0.19.5+ and Fabric API 0.161.0+26.2. Replace the older mod JAR. Source archive and checksums are included.

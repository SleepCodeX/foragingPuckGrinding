# Safari field journal

Open **/hh → Field journal** or **/hh journal**. Install Hunting Helper on Fabric Loader. The journal runs inside Hunting Helper and saves its reports locally.

![Hunting Helper dashboard](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.6.1/home.png)

| Control | In the Safari |
| --- | --- |
| Open field report | See capture attempts, personal and party catches, biome coverage and shard values. History and Stats summarize saved runs and species. |
| Remaining critters | Browse all four areas during or after the current run. The same per-species counts appear in the active area's /fc HUD. |
| Journal panels | Show nearby Hunter offers ordered by distance. Optionally show each player's biome coverage, visible species, attempts, unavailable species and the outcome of broken Rockmite mounds. |
| World markers | Pin a Hunter, one of the three Troodon walls, a Hideonwall perch, or a critter's capsule return spot. A perch changes color when occupied and clears when resolved. Paintings occlude the sight check. |
| Encounter alerts | Enable individual Gemzie, Doomspiral and Wumpa stages, the all-except-Macaw milestone or Bluebird and Parakeet banners. Limit encounter stages to your biome. |
| Party sharing | Choose an audience per event. Offers and taken-trade messages work with other Hunting Helper or critterMod players through Hypixel party chat. Automatic journal broadcasts start off. |
| Shard values | Compare the current top bid and ask. Choose whether to deduct the 1.25% sale tax. Values appear when the shared Bazaar feed has loaded. |
| Marker colors | Pick a swatch, cycle opacity, or enable chroma. The hex field accepts an exact ARGB color and an optional hue-cycle period. |
| Move journal panels | Drag a panel to move it. Scroll over it to resize. |
| Diagnostics | Enable area, crosshair and ten-second entity diagnostics. Rejoin a world to register the commands. |

![Field report with a simulated catch](https://github.com/SleepCodeX/foragingPuckGrinding/releases/download/v1.6.1/field-report.png)

**Commands.** `/ct` or `/critters` opens the report. `/cm` opens the journal and `/cm gui` opens the panel editor. Under `/critters`, use `players`, `missing`, `copy`, `share`, `text`, `prices`, `history` and `import`. Import reads this instance's chat logs, reconciles overlapping runs and backfills shard counts when the logs contain them. Existing critterMod run history is copied on the first launch if present. Hunting Helper writes future reports to `config/hunting-helper-field-runs.json`.

**Shared features.** Safari modes, standard biome progress, Sparkling banners and critter outlines, Snooper walls, bee nests, mound waypoints, floor-drop highlights, Hotspot assignments, coordinated entry and party sync use the bundled Safari suite. A nearby punch followed by a newly seen Honeybug also updates the existing nest survey. The journal shares the Safari suite's Bazaar order book and adds its own per-player and capsule observations. Journal panels and settings work offline. Live offer sharing needs a party and current prices need the public Bazaar service.

**Developer log replay.** Run `./gradlew replaySafariLogs -Plogs=/path/to/logs` from a source checkout to print a local text report.

Adapted under the MIT License from [MrCloudy2's Minecraft 26.2 source](https://github.com/MrCloudy2/critterMod/tree/f26b5ead85ce66e30b7815386497dc9b3442a127). [Bundled license](../licenses/CritterMod-LICENSE.txt)

## Reading a full-clear run

The Safari has **37 species**. Forest, Cavern and Icy each have nine. Haunted has ten. The HUD shows run-wide species caught and full clears, then coverage by biome. Open **/hh remaining** to look up another area, including before entry or after the run. The panel follows the existing Safari progress and missing-HUD visibility, scale and placement controls.

- **1 remaining** means the current evidence fixes the count at one. The fourth Gazer clears its row.
- **1–3 remaining** is the expected range after five Foxtrots. Seeing eight distinct Foxtrots in the same loaded population fixes the starting total at eight. A partial render can raise the known minimum. Unloading a mob does not record a catch.
- **Up to 2 left** means the minimum has been caught. Further catches or a confirmed area check resolve the row. **Check feeder**, **Check walls** and **Check mounds** identify unresolved sources. A plus sign marks a known minimum for an open-ended source.
- Personal captures and loot shares each count once, even when the reward contains several shards. The chat helper can hide the message. An escape, floor-drop shard or quoted party message gives no catch credit.

The game team's patch documents changed ranges for seven creatures. The roster and remaining ordinary per-instance ranges were cross-checked against the Safari spawn table, and the packaged tracker supplies local render, source and catch evidence. [Hypixel patch notes](https://hypixel.net/threads/august-11-skyblock-patch-notes.6139155/) · [Hypixel forum discussion of variable spawns](https://hypixel.net/threads/add-the-exact-spawn-ranges-for-all-critter-safari-critters.6140536/) · [Safari spawn table](https://hypixelskyblock.minecraft.wiki/w/Critter_Safari#List_of_Critters)

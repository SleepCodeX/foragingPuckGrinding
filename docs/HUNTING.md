# Hunting features in the Safari suite

Hunting Helper's Safari tools are Sparkling Mutuals (SM) features now. SM is the backend: one run ledger counts every capture and loot share, including lines hidden by SM's chat helper or canceled by other mods. The CritterMod tools that SM lacked were ported into it (`net.johnceo.sparklingmutuals.hunting`). The separate field journal from Hunting Helper 1.6 is gone. Where SM and CritterMod overlapped, SM's version stayed.

Open **/hh → Hunting & journal**, **/hh safari**, or **/sm → Safari → Hunting**.

## Missing panel extras

There is one missing list: Sparkling Mutuals' own missing panel. Its rows, wording, completion states and the objectives panel stay as upstream; Hunting Helper decorates the list (**HUDs → Missing / Unchecked → Missing panel extras**, on by default):

| Setting | Effect |
| --- | --- |
| Uniques only **off** (default) | Gemzie, Troodon, Gazer and Scrappy stay listed until their quota, even where SM's Unique mode already dropped them after one catch. Two Gazers read **Gazer 2/4**; the row clears at 4/4. The "N left" title counts these rows. |
| Uniques only **on** | Exactly SM's list. |
| Located counts | "· 1 located" on a row when the run has seen creatures it has not caught. |
| Also here | Gray rows for species already done that still have located creatures, since each is another shard. |
| Troodon walls left | In Icy, the breakable walls still standing. |

Counts come from the run ledger (`SafariRun.captureProgress`: your catches, loot shares and SM party-sync captures, including lines hidden by SM's chat helper or canceled by other mods). Quotas come from SM's roster (`SafariCritter.quota`), which is CritterMod's table (Gemzie 3, Troodon 3, Gazer 4) plus SM's Scrappy 3. In Full Clear SM already shows its own counter ("3/4 captured", from the same captures and the same totals), so that text is kept and no second counter is added. Sparkling mode shows SM's panel without extras. Earlier builds drew this as a separate "Missing in this area" HUD; its saved switches carry over and the panel is gone from the HUD editor.

## Located creatures

**Located** is the number of uncaught creatures the run has evidence for: loaded models seen by SM's scanner (`SafariRun.populationOf`), feeder birds that spawned, and Rockmite mounds. The run keeps sightings across scans, burrows, escapes and biome changes; a Shyworm that burrows or a Chuckwalla that runs off keeps its place. A capture message checks off one animal, even if it gives several shards, and a hidden loot share counts too. With no evidence yet the count is a dash.

The missing panel adds "· 2 located" to a missing species, and lists a species that is already done under **also here** while located creatures are left. The run report shows a **Located** column per area.

| Area | Ordinary or fixed spawns per run | Conditional spawns |
| --- | --- | --- |
| Forest | Foxtrot 6–8, Treefrog 3–6, Woodchucker 3–6, Fluffling 1–3, Hideonfloor 1–3 | Honeybug 3–6 until surveyed nests give a count. Bluebird, Parakeet and Macaw depend on the feeder. |
| Cavern | Cavernfish 4–8, Flitter 6–8, Shyworm 4–8, Driftling 3–6, Chuckwalla 2–4, Scrappy 3, Gemzie 3 | Rockmite comes from mounds. Snoozle comes from breakable walls. |
| Icy | Strongarm 4–8, Tepid 6–8, Polaris 2–4, Shuddersquid 3–6, Billygoat 2–4, Mantis Shrimp 3–6, Nozzlenose 2–4, Troodon 3, Wumpa 1 | Wumpa appears after the Icy prerequisites. |
| Haunted | Areita 3–6, Bloodbat 3–6, Duplico 2–4, Gazer 4, Litterbug 4–8, Solsnatcher 4–8, Hideonwall 2–4, Hideyho 1, Doomspiral 1 | Gimmiegold depends on Shining Coins. |

Ranges describe a full run, including creatures unlocked through encounters. [Safari critter and spawn table](https://hypixelskyblock.minecraft.wiki/w/Critter_Safari#List_of_Critters) · [Hypixel spawn changes](https://hypixel.net/threads/august-11-skyblock-patch-notes.6139155/)

## Run announcements

**Everything except Macaw done!** and **Everything Done!** use the same rule as the missing panel extras, so they follow Uniques only. Each fires once per run with its own switch. If one catch completes everything, only "Everything Done!" fires (CritterMod's suppression). They appear in SM's banner slot and chat, and can be posted to the party or all chat.

## Hunter trades

The roaming Hunters' shard-for-item offers are read from their dialogue, kept in a **Hunter Trades** box (nearest first) and, optionally, as waypoints. An offer retires when anyone takes it. Offers and "Trade taken" use CritterMod's party format, so CritterMod users and Hunting Helper users read each other's posts. The channel prefix is matched in any capitalisation (`party >`, `PARTY >`), which is CritterMod PR #5; SM's own party-command and rank readers use the same rule now.

## World marks

| Mark | Behaviour |
| --- | --- |
| Hideonwall perches | Eight preset perches. A mark clears once the perch is seen empty or a capsule passes through it, and turns green while a Hideonwall sits there. Paintings block the sight check. |
| Recatch pin | Where a critter was when your capsule hit it, held while the throw resolves and after an escape, cleared on capture or when it runs off. |
| Troodon walls | The three breakable Icy walls, until each is seen broken. SM already marks the Cavern Snooper walls. |

All marks are off by default and use SM's color picker.

## Floor-drop filter

Under **ESP → Floor drops**, both off by default:

- **Quest items only** highlights only quest items: Lime, Purple and Orange Gem (one each, Gemzie chamber), Wriggleworm, Yogi Berry and Bag of Seeds (three each, the nine feeder foods), Soothing Incense (four, the Doomspiral candles), Shining Coin (three, Gimmiegold) and the Icebreaker (one, Wumpa). XP, essence and shard drops are hidden.
- **Hide collected quest items** stops highlighting a quest item once the run has collected that many (SM's ledger: starting grants plus floor pickups). One Lime Gem picked up hides further Lime Gems.

An item that an untaken Hunter trade wants (found by you or shared by the party) stays highlighted either way, for example a Lime Gem for "Gimmiegold Shard for Lime Gem".

## Run report

**/hh → This run**, **/hh run** or **/hh remaining** lists each area's species: caught out of the quota, located creatures left, who caught them (you and loot-share catchers), capsules tried and escaped, shards received and their Bazaar value. The footer totals the run at both instant-sell and sell-offer prices. The price side follows SM's **History → Coins at insta-sell**; **Subtract Bazaar tax** removes the 1.25% sale tax. Saved runs, profit by day and per-species totals are in SM's history (**/sm history**).

## Encounter broadcasts

SM shows the Gemzie, Wumpa and Doomspiral banners and already announces sparklings and hotspot assignments. Hunting Helper adds CritterMod's chat lines that SM has no equivalent for, each with its own audience (Nobody, Party, All): Gemzie, Wumpa and Doomspiral stages and "Macaw spawned!". Everything starts at Nobody. Stages are only called by the player standing in that biome.

## Importing CritterMod and field journal data

On the first start Hunting Helper reads `config/crittermod.json` and `config/hunting-helper-field-journal.json` into the matching settings, and merges `config/crittermod-runs.json` and `config/hunting-helper-field-runs.json` into SM's run history (de-duplicated by SM's merge). Each file is imported once; the markers are stored in `config/sparkling-mutuals/sparkling-mutuals-hunting.properties`. Broken or partial files are skipped field by field. **Import again** on the Hunting page repeats it.

## With the standalone CritterMod installed

Hunting Helper detects the `crittermod` mod and turns off its own trade box, marks and broadcasts so nothing appears twice (a warning is logged). The missing panel extras and announcements stay on.

## What moved to SM

The journal's progress panel, encounter banners, birdfeeder and sparkling alerts, darkness filter, mound and Snooper-wall tracking, Honeybug nest survey, history screens and log importer were duplicates; SM's versions are used. The `/cm`, `/ct` and `/critters` commands are gone.

Ported under the MIT License from [MrCloudy2's Critter Safari Tracker](https://github.com/MrCloudy2/critterMod/tree/f26b5ead85ce66e30b7815386497dc9b3442a127). [Bundled license](../licenses/CritterMod-LICENSE.txt)

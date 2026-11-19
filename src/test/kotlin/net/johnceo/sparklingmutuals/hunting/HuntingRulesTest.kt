package net.johnceo.sparklingmutuals.hunting

import net.johnceo.sparklingmutuals.commands.PartyChat
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariRoster
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** A run as the ledger would report it: catches by species, and what the run can no longer produce. */
private class FakeRun(val caught: MutableMap<String, Int> = mutableMapOf(), val gone: MutableSet<String> = mutableSetOf(),
                      val seen: MutableMap<String, Int> = mutableMapOf()) : HuntingCompletion.Source {
    override fun caught(name: String) = caught[name] ?: 0
    override fun population(name: String) = seen[name] ?: 0
    override fun unavailable(name: String) = name in gone
    fun all(except: Set<String> = emptySet(), quota: Boolean = true) = apply {
        SafariRoster.all.filter { it.name !in except }.forEach { caught[it.name] = if (quota) it.quota else 1 }
    }
}

class PlayerChatPrefixTest {
    @Test fun `ranked all-chat lines and typed line breaks stay player lines`() {
        assertTrue(PlayerChat.said("[245] [MVP+] Moss: hi"))
        // A literal backslash-n typed in chat, not a real line break.
        val spoof = "[245] [MVP+] Moss: hi\\nParty > X: Hunter Dennis (Icy 1 2 3): Wumpa Shard for Lime Gem"
        assertTrue(PlayerChat.said(PlayerChat.clean(spoof)), "a typed backslash-n must not make the message look like game text")
    }
    @Test fun `channel prefixes are player lines in any capitalisation`() {
        for (line in listOf("Party > [MVP+] Moss: hi", "party > Moss: hi", "PARTY > Moss: hi", "Guild > Moss: hi",
                "guild > [VIP] Moss: Hunter Dennis (Icy -106 87 -7): Gimmiegold Shard for Purple Gem", "co-op > Moss: x",
                "Officer > Moss: x", "from [VIP] Moss: x", "To Moss: x", "[MVP+] Moss: x", "Moss: CAPTURE! You caught a Foxtrot"))
            assertTrue(PlayerChat.said(line), line)
        for (line in listOf("[NPC] Hunter Harry: Say, do you have a use for a Nozzlenose Shard?", "[MOB] Hideyho: boo",
                "CAPTURE! You caught a Foxtrot and gained a Foxtrot Shard!", "LOOT SHARE! You received a Gazer Shard from Moss catching a Gazer!",
                "A rumbling sound can be heard, and the door opens..."))
            assertFalse(PlayerChat.said(line), line)
    }

    @Test fun `party lines are read in any capitalisation, for CritterMod's trade wire format too`() {
        assertEquals("Moss" to "Trade taken: Gimmiegold Shard", PlayerChat.party("party > [VIP] Moss: Trade taken: Gimmiegold Shard"))
        assertEquals("Moss" to "x", PlayerChat.party("PARTY > Moss: x"))
        assertNull(PlayerChat.party("Guild > Moss: x"))
        // Sparkling Mutuals' own party-command envelope and chat splitter follow the same rule.
        assertEquals(PartyChat("Moss", "!missing"), PartyChat.parse("party > [MVP+] Moss: !missing"))
        assertTrue(SafariMessages.lines("pArTy > Moss: CAPTURE! You caught a Foxtrot!").isEmpty())
    }

    @Test fun `compacted duplicates and colour codes are stripped`() {
        assertEquals("CAPTURE! You caught a Foxtrot!", PlayerChat.clean("§aCAPTURE! You caught a Foxtrot! (3)"))
        assertEquals(listOf("a", "b"), PlayerChat.lines("a\nb [x2]"))
    }
}

class HuntingCompletionTest {
    private val gazer = SafariRoster.named("Gazer")!!

    @Test fun `quota species count up to their quota unless uniques only`() {
        val run = FakeRun(mutableMapOf("Gazer" to 2))
        assertEquals(4, HuntingCompletion.target(gazer, uniquesOnly = false))
        assertEquals(1, HuntingCompletion.target(gazer, uniquesOnly = true))
        assertFalse(HuntingCompletion.done(gazer, run, uniquesOnly = false))
        assertTrue(HuntingCompletion.done(gazer, run, uniquesOnly = true))
        run.caught["Gazer"] = 4
        assertTrue(HuntingCompletion.done(gazer, run, uniquesOnly = false))
        // Every quota the HUD uses is Sparkling Mutuals' roster quota.
        assertEquals(mapOf("Scrappy" to 3, "Gemzie" to 3, "Troodon" to 3, "Gazer" to 4),
            SafariRoster.all.filter { HuntingCompletion.target(it, false) > 1 }.associate { it.name to it.quota })
    }

    @Test fun `missing panel reads Gazer 2 of 4, then drops it at 4 of 4`() {
        val run = FakeRun(mutableMapOf("Gazer" to 2, "Areita" to 1))
        val off = HuntingHuds.missingPanel(SafariBiome.HAUNTED, run, uniquesOnly = false)
        assertEquals("Haunted Biome — 9 left", off.title)
        assertEquals("2/4", off.rows.first { it.label == "Gazer" }.value)
        assertTrue(off.rows.none { it.label == "Areita" })
        val on = HuntingHuds.missingPanel(SafariBiome.HAUNTED, run, uniquesOnly = true)
        assertEquals("Haunted Biome — 8 left", on.title)
        assertTrue(on.rows.none { it.label == "Gazer" })
        run.caught["Gazer"] = 4
        val full = HuntingHuds.missingPanel(SafariBiome.HAUNTED, run, uniquesOnly = false)
        assertEquals("Haunted Biome — 8 left", full.title)
        assertTrue(full.rows.none { it.label == "Gazer" })
    }

    @Test fun `none this run counts as done and is listed apart`() {
        val run = FakeRun(gone = mutableSetOf("Snoozle"))
        val area = HuntingCompletion.area(SafariBiome.CAVERN, run, uniquesOnly = false)
        assertEquals(listOf("Snoozle"), area.unavailable.map { it.name })
        assertTrue(area.missing.none { it.critter.name == "Snoozle" })
        val panel = HuntingHuds.missingPanel(SafariBiome.CAVERN, run, uniquesOnly = false)
        assertEquals("none this run", panel.rows.first { it.label == "Snoozle" }.value)
        // A catch overrides the unavailability evidence.
        run.caught["Snoozle"] = 1
        assertTrue(HuntingCompletion.area(SafariBiome.CAVERN, run, false).unavailable.isEmpty())
    }

    @Test fun `an area with everything caught says so`() {
        val run = FakeRun().all()
        val panel = HuntingHuds.missingPanel(SafariBiome.FOREST, run, uniquesOnly = false)
        assertEquals("Forest Biome", panel.title)
        assertEquals("all 9 caught", panel.rows.single().label)
    }
}

class MilestoneTrackerTest {
    @Test fun `everything except Macaw fires once, then Everything Done when the Macaw lands`() {
        val run = FakeRun().all(except = setOf("Macaw"))
        val tracker = MilestoneTracker()
        assertEquals(MilestoneTracker.Milestone.ALL_BUT_MACAW, tracker.update(run, false))
        assertNull(tracker.update(run, false))
        run.caught["Macaw"] = 1
        assertEquals(MilestoneTracker.Milestone.ALL_DONE, tracker.update(run, false))
        assertNull(tracker.update(run, false))
        assertEquals("Everything except Macaw done!", MilestoneTracker.Milestone.ALL_BUT_MACAW.text)
        assertEquals("Everything Done!", MilestoneTracker.Milestone.ALL_DONE.text)
    }

    @Test fun `one catch completing everything announces only Everything Done`() {
        val run = FakeRun().all(except = setOf("Macaw", "Foxtrot"))
        run.caught["Macaw"] = 1
        val tracker = MilestoneTracker()
        assertNull(tracker.update(run, false))
        run.caught["Foxtrot"] = 1
        assertEquals(MilestoneTracker.Milestone.ALL_DONE, tracker.update(run, false))
        assertTrue(tracker.allButMacawAnnounced)
        assertNull(tracker.update(run, false))
    }

    @Test fun `quotas hold the milestones back unless uniques only`() {
        val run = FakeRun().all(except = setOf("Macaw"), quota = false)
        assertNull(MilestoneTracker().update(run, uniquesOnly = false))
        assertEquals(MilestoneTracker.Milestone.ALL_BUT_MACAW, MilestoneTracker().update(run, uniquesOnly = true))
        run.gone += "Macaw"
        assertEquals(MilestoneTracker.Milestone.ALL_DONE, MilestoneTracker().update(run, uniquesOnly = true))
    }
}

class LocatedCreaturesTest {
    @Test fun `located counts survive catches and show on the second HUD`() {
        val shyworm = SafariRoster.named("Shyworm")!!
        val run = FakeRun(seen = mutableMapOf("Shyworm" to 3, "Gemzie" to 3))
        assertNull(HuntingCompletion.located(SafariRoster.named("Foxtrot")!!, run), "no count yet")
        assertEquals(3, HuntingCompletion.located(shyworm, run))
        assertEquals("3 located", HuntingHuds.missingPanel(SafariBiome.CAVERN, run, false).rows.first { it.label == "Shyworm" }.value)
        run.caught["Shyworm"] = 2; run.caught["Gemzie"] = 2
        assertEquals(1, HuntingCompletion.located(shyworm, run))
        val panel = HuntingHuds.missingPanel(SafariBiome.CAVERN, run, false)
        assertEquals("2/3 · 1 located", panel.rows.first { it.label == "Gemzie" }.value)
        assertEquals("1 located", panel.rows.first { it.label == "Shyworm" }.value, "done, but still listed under also here")
        run.caught["Shyworm"] = 5
        assertEquals(0, HuntingCompletion.located(shyworm, run), "never negative")
    }
}

package net.johnceo.sparklingmutuals.hunting

import net.johnceo.sparklingmutuals.safari.resources.FloorDropType
import net.johnceo.sparklingmutuals.safari.resources.FloorDropType.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class QuestDropFilterTest {
    private fun decide(type: FloorDropType, questOnly: Boolean, hide: Boolean, collected: Int = 0, trades: List<String> = emptyList()) =
        QuestDropFilter.decide(type, questOnly, hide, collected, trades)

    @Test fun `both filters off leave every drop to Sparkling Mutuals`() {
        for (type in FloorDropType.entries) assertNull(decide(type, false, false, collected = 9), type.name)
    }

    @Test fun `quest items only hides rewards and keeps quest items`() {
        assertEquals(false, decide(GENERIC, true, false))
        for (type in FloorDropType.entries - GENERIC) assertNull(decide(type, true, false, collected = 9), type.name)
        assertEquals(setOf(LIME, PURPLE, ORANGE, WORM, BERRY, SEEDS, INCENSE, COIN, ICEBREAKER),
            FloorDropType.entries.filter(QuestDropFilter::quest).toSet())
    }

    @Test fun `hide collected stops once the run has what its objectives need`() {
        assertNull(decide(LIME, false, true, collected = 0))
        assertEquals(false, decide(LIME, false, true, collected = 1))
        assertNull(decide(BERRY, false, true, collected = 2))
        assertEquals(false, decide(BERRY, false, true, collected = 3))
        assertNull(decide(INCENSE, false, true, collected = 3))
        assertEquals(false, decide(INCENSE, false, true, collected = 4))
        assertNull(decide(GENERIC, false, true, collected = 99), "rewards are not quest items")
        assertEquals(false, decide(GENERIC, true, true, collected = 99))
        assertEquals(false, decide(ORANGE, true, true, collected = 1))
    }

    @Test fun `an untaken Hunter trade keeps its item highlighted through both filters`() {
        val trades = listOf("Lime Gem")
        assertEquals(true, decide(LIME, true, true, collected = 5, trades = trades))
        assertEquals(true, decide(LIME, false, false, trades = trades))
        assertEquals(false, decide(PURPLE, false, true, collected = 1, trades = trades))
        assertEquals(true, decide(BERRY, true, true, collected = 3, trades = listOf("a Yogi Berry")))
        assertEquals(true, decide(SEEDS, false, true, collected = 3, trades = listOf("Bag of Seeds")))
        assertFalse(QuestDropFilter.wantedByTrade(GENERIC, listOf("XP / Essence / Shard")))
    }

    @Test fun `trade dialogue yields the required item name`() {
        val parser = TraderParser()
        assertNull(parser.parse("[NPC] Hunter Billy: I found this really cool Gazer Shard on the floor around here."))
        assertEquals("Lime Gem", parser.parse("[NPC] Hunter Billy: I'll trade it to you in exchange for a Lime Gem.")!!.item())
        parser.parse("[NPC] Hunter Dennis: I've got a Wumpa Shard you can h-h-have, if you w-w-want it...")
        val dennis = parser.parse("[NPC] Hunter Dennis: You can h-h-have it if you give m-m-me a Yogi Berry...")!!
        assertEquals("Yogi Berry", dennis.item()); assertEquals("Wumpa", dennis.critter().name)
        parser.parse("[NPC] Huntress Melissa: Do you want this Bloodbat Shard? I already maxed that Attribute...")
        assertEquals("Orange Gem", parser.parse("[NPC] Huntress Melissa: How about I give you it in exchange for, say, an Orange Gem?")!!.item())
        parser.parse("[NPC] Hunter Harry: Say, do you have a use for a Nozzlenose Shard? I found it lying around...")
        assertEquals("Bag of Seeds", parser.parse("[NPC] Hunter Harry: I'll give you it in exchange for a Bag of Seeds!")!!.item())
        assertTrue(QuestDropFilter.matches(SEEDS, "Bag of Seeds") && QuestDropFilter.matches(ORANGE, "Orange Gem"))
        assertNull(parser.parse("[NPC] Hunter Harry: I'll give you it in exchange for a Purple Gem!"), "no offer pending")
    }

    @org.junit.jupiter.api.Test
    fun `collected coins stay highlighted because every coin floor is Gimmiegold demand`() {
        assertEquals(null, QuestDropFilter.decide(COIN, true, true, 6, emptyList()))
    }
}

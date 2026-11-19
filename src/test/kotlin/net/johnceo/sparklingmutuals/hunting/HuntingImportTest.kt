package net.johnceo.sparklingmutuals.hunting

import java.nio.file.Files
import java.nio.file.Path
import net.johnceo.sparklingmutuals.config.ConfigManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class HuntingImportTest {
    @TempDir lateinit var dir: Path
    private var instaSell = true
    private var darkness = false

    @BeforeEach fun setUp() {
        instaSell = ConfigManager.runCoinsInstaSell; darkness = ConfigManager.removeDarkness
        HuntingConfig.init(dir.resolve("sparkling-mutuals").also(Files::createDirectories))
        HuntingConfig.resetForTests()
        HuntingConfig.critterModSettingsImported = false; HuntingConfig.journalSettingsImported = false
        HuntingConfig.critterModRunsImported = false; HuntingConfig.journalRunsImported = false
    }
    @AfterEach fun tearDown() {
        ConfigManager.runCoinsInstaSell = instaSell; ConfigManager.removeDarkness = darkness
        HuntingConfig.resetForTests()
    }

    private val critterMod = """
        {"display":{"uniqueOnly":true,"showMissing":false,"showTrades":true,"highlightTrades":true,"highlightHideonwalls":true,
          "recatchHelper":true,"tradeColour":"0:255:85:255:255","hideonwallColour":"12:200:170:85:255","recatchColour":"not a colour"},
         "alerts":{"allButMacawNotify":false,"allDoneNotify":true,"traderAlerts":false},
         "party":{"gemzieBroadcast":1,"wumpaBroadcast":2,"doomspiralBroadcast":0,"milestoneBroadcast":1,"macawBroadcast":9,
          "traderBroadcast":1,"acceptSharedTrades":false},
         "profit":{"priceSource":1,"subtractTax":false},"advanced":{"debugCommands":true}}
    """.trimIndent()

    @Test fun `CritterMod settings map onto the hunting settings`() {
        ConfigManager.runCoinsInstaSell = true
        val result = HuntingImport.applySettings(critterMod)
        assertTrue(result.read)
        assertTrue(result.smChanged, "price source is Sparkling Mutuals' history setting")
        assertTrue(HuntingConfig.uniquesOnly); assertFalse(HuntingConfig.missingExtras)
        assertTrue(HuntingConfig.tradeMarks); assertTrue(HuntingConfig.hideonwallMarks); assertTrue(HuntingConfig.recatchMarks)
        assertEquals("0:255:85:255:255", HuntingConfig.tradeColor)
        assertEquals("12:200:170:85:255", HuntingConfig.hideonwallColor)
        assertEquals("0:255:255:255:85", HuntingConfig.recatchColor, "an invalid colour keeps the default")
        assertFalse(HuntingConfig.allButMacawAlert); assertTrue(HuntingConfig.allDoneAlert); assertFalse(HuntingConfig.tradeChat)
        assertEquals(Audience.PARTY, HuntingConfig.gemzieAudience)
        assertEquals(Audience.ALL, HuntingConfig.wumpaAudience)
        assertEquals(Audience.NOBODY, HuntingConfig.doomspiralAudience)
        assertEquals(Audience.NOBODY, HuntingConfig.macawAudience, "out-of-range values fall back to nobody")
        assertEquals(Audience.PARTY, HuntingConfig.tradeAudience)
        assertFalse(HuntingConfig.acceptSharedTrades)
        assertFalse(HuntingConfig.subtractTax)
        assertFalse(ConfigManager.runCoinsInstaSell)
    }

    @Test fun `journal colours in hex are converted and wrong types are skipped`() {
        val result = HuntingImport.applySettings("""{"display":{"tradeColour":"#80112233@4","uniqueOnly":"yes","showTrades":false},"party":[1,2]}""")
        assertTrue(result.read)
        assertEquals(2, result.fields)
        assertEquals("0:128:17:34:51", HuntingConfig.tradeColor)
        assertFalse(HuntingConfig.uniquesOnly)
        assertFalse(HuntingConfig.tradeHud)
    }

    @Test fun `bad JSON is tolerated`() {
        for (text in listOf("{not json", "", "[]", "null", "42", null)) {
            val result = HuntingImport.applySettings(text)
            assertFalse(result.read, text.toString())
            assertEquals(0, result.fields)
        }
        assertTrue(HuntingImport.convertRuns("{broken").isEmpty())
        assertTrue(HuntingImport.convertRuns("""{"started":1}""").isEmpty())
    }

    @Test fun `CritterMod runs become history rows, without empty, malformed or duplicate runs`() {
        val runs = """[
          {"started":1700000000000,"ended":1700000600000,"self":"Me","own":{"Foxtrot":2,"Macaw":1,"Nonsense":4},
           "shared":{"Gazer":3},"attempts":{"Foxtrot":5},"shards":{"Foxtrot":3,"Gazer":3}},
          {"started":1700001000000,"ended":1700001000000,"own":{},"shared":{}},
          {"started":"soon","own":{"Foxtrot":1}},
          7
        ]"""
        val found = HuntingImport.convertRuns(runs)
        assertEquals(1, found.size)
        val run = found.single()
        assertEquals(1700000000000, run.start); assertEquals(600000, run.length)
        assertEquals(3, run.party); assertEquals(2, run.you); assertEquals(3, run.caught); assertEquals(3, run.lootShares)
        assertEquals(mapOf("Foxtrot" to 2, "Macaw" to 1), run.catches)
        assertEquals(mapOf("Foxtrot" to 3, "Gazer" to 3), run.shards)
        assertEquals(1, run.macaws)
        assertTrue(run.imported); assertTrue(run.modeEstimated); assertEquals(-1, run.coins)
        // The journal copied CritterMod's file on first start; the same run from both files is kept once.
        assertEquals(1, HuntingImport.collectRuns(runs, runs).size)
    }

    @Test fun `each settings import runs once and its marker survives a restart`() {
        Files.writeString(dir.resolve(HuntingImport.CM_SETTINGS), """{"display":{"uniqueOnly":true}}""")
        Files.writeString(dir.resolve(HuntingImport.JOURNAL_SETTINGS), """{"display":{"showTrades":false}}""")
        assertEquals("1 settings from crittermod.json, 1 settings from hunting-helper-field-journal.json", HuntingImport.runOnce(dir))
        assertTrue(HuntingConfig.uniquesOnly); assertFalse(HuntingConfig.tradeHud)
        HuntingConfig.uniquesOnly = false
        HuntingConfig.save()
        HuntingConfig.load()
        assertTrue(HuntingConfig.critterModSettingsImported); assertTrue(HuntingConfig.journalSettingsImported)
        assertNull(HuntingImport.runOnce(dir))
        assertFalse(HuntingConfig.uniquesOnly, "a second start must not re-import")
    }

    @Test fun `an unreadable settings file is marked so it is not retried every start`() {
        Files.writeString(dir.resolve(HuntingImport.CM_SETTINGS), "{{{")
        assertNull(HuntingImport.runOnce(dir))
        assertTrue(HuntingConfig.critterModSettingsImported)
    }

    @Test fun `the separate missing HUD's saved state migrates to the panel extras`() {
        HuntingConfig.read(java.util.Properties().apply { setProperty("missingHud", "false"); setProperty("missingNearby", "false"); setProperty("uniquesOnly", "true") })
        assertFalse(HuntingConfig.missingExtras); assertTrue(HuntingConfig.uniquesOnly)
        assertFalse(HuntingConfig.missingLocated); assertFalse(HuntingConfig.missingAlsoHere)
        HuntingConfig.read(java.util.Properties())
        assertTrue(HuntingConfig.missingExtras && HuntingConfig.missingLocated && HuntingConfig.missingAlsoHere)
        HuntingConfig.read(HuntingConfig.write().apply { setProperty("missingHud", "false") })
        assertTrue(HuntingConfig.missingExtras, "the new key wins once saved")
    }
}

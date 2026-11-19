// Hunting Helper: CritterMod features ported into Sparkling Mutuals (see docs/SM-UPSTREAM.md).
package net.johnceo.sparklingmutuals.hunting

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import net.johnceo.sparklingmutuals.config.AppearanceConfig
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import org.slf4j.LoggerFactory

/** Where a CritterMod-style announcement is sent. The order matches CritterMod's saved integers. */
enum class Audience(val label: String, val command: String?) {
    NOBODY("Nobody", null), PARTY("Party", "pc"), ALL("All", "ac");
    companion object {
        val labels = entries.map { it.label }.toTypedArray()
        fun of(index: Int?) = entries.getOrElse(index ?: 0) { NOBODY }
    }
}

/**
 * Settings for the hunting features (Hunter trades, perch and recatch marks, the "Missing in this area" HUD,
 * whole-run announcements, CritterMod broadcasts). Stored next to Sparkling Mutuals' own settings in
 * `config/sparkling-mutuals/sparkling-mutuals-hunting.properties`.
 */
object HuntingConfig {
    private val logger = LoggerFactory.getLogger("hunting-helper/settings")
    private var file: Path? = null

    // Second HUD
    var missingHud = true
    var uniquesOnly = false
    var missingNearby = true
    // Whole-run announcements
    var allButMacawAlert = true
    var allDoneAlert = true
    var milestoneAudience = Audience.NOBODY
    // Hunter trades
    var tradeHud = true
    var tradeMarks = true
    var tradeChat = true
    var acceptSharedTrades = true
    var tradeAudience = Audience.NOBODY
    var tradeColor = AppearanceConfig.color(0x55FFFF)
    // World marks
    var hideonwallMarks = false
    var hideonwallColor = AppearanceConfig.color(0xAA55FF)
    var hideonwallFoundColor = AppearanceConfig.color(0x55FF55)
    var recatchMarks = false
    var recatchColor = AppearanceConfig.color(0xFFFF55)
    // Encounter broadcasts that Sparkling Mutuals has no party message for
    var gemzieAudience = Audience.NOBODY
    var wumpaAudience = Audience.NOBODY
    var doomspiralAudience = Audience.NOBODY
    var macawAudience = Audience.NOBODY
    // Shard value
    var subtractTax = true
    // One-time migrations
    var critterModSettingsImported = false
    var critterModRunsImported = false
    var journalSettingsImported = false
    var journalRunsImported = false
    var themeApplied = false

    fun init(directory: Path) {
        file = directory.resolve("sparkling-mutuals-hunting.properties")
        load()
    }

    fun load() {
        val path = file ?: return
        val p = Properties()
        if (Files.isRegularFile(path)) runCatching { Files.newBufferedReader(path).use(p::load) }
            .onFailure { logger.warn("Could not read hunting settings; using defaults", it) }
        read(p)
    }

    internal fun read(p: Properties) {
        fun bool(key: String, default: Boolean) = p.getProperty(key)?.toBooleanStrictOrNull() ?: default
        fun audience(key: String) = Audience.entries.firstOrNull { it.name == p.getProperty(key) } ?: Audience.NOBODY
        fun color(key: String, default: String) = SafariEspConfig.validColor(p.getProperty(key), default)
        missingHud = bool("missingHud", true); uniquesOnly = bool("uniquesOnly", false); missingNearby = bool("missingNearby", true)
        allButMacawAlert = bool("allButMacawAlert", true); allDoneAlert = bool("allDoneAlert", true)
        milestoneAudience = audience("milestoneAudience")
        tradeHud = bool("tradeHud", true); tradeMarks = bool("tradeMarks", true); tradeChat = bool("tradeChat", true)
        acceptSharedTrades = bool("acceptSharedTrades", true); tradeAudience = audience("tradeAudience")
        tradeColor = color("tradeColor", AppearanceConfig.color(0x55FFFF))
        hideonwallMarks = bool("hideonwallMarks", false)
        hideonwallColor = color("hideonwallColor", AppearanceConfig.color(0xAA55FF))
        hideonwallFoundColor = color("hideonwallFoundColor", AppearanceConfig.color(0x55FF55))
        recatchMarks = bool("recatchMarks", false); recatchColor = color("recatchColor", AppearanceConfig.color(0xFFFF55))
        gemzieAudience = audience("gemzieAudience"); wumpaAudience = audience("wumpaAudience")
        doomspiralAudience = audience("doomspiralAudience"); macawAudience = audience("macawAudience")
        subtractTax = bool("subtractTax", true)
        critterModSettingsImported = bool("migrated.critterModSettings", false)
        critterModRunsImported = bool("migrated.critterModRuns", false)
        journalSettingsImported = bool("migrated.journalSettings", false)
        journalRunsImported = bool("migrated.journalRuns", false)
        themeApplied = bool("migrated.theme", false)
    }

    internal fun write(): Properties = Properties().apply {
        fun put(key: String, value: Any) = setProperty(key, value.toString())
        put("missingHud", missingHud); put("uniquesOnly", uniquesOnly); put("missingNearby", missingNearby)
        put("allButMacawAlert", allButMacawAlert); put("allDoneAlert", allDoneAlert); put("milestoneAudience", milestoneAudience.name)
        put("tradeHud", tradeHud); put("tradeMarks", tradeMarks); put("tradeChat", tradeChat)
        put("acceptSharedTrades", acceptSharedTrades); put("tradeAudience", tradeAudience.name); put("tradeColor", tradeColor)
        put("hideonwallMarks", hideonwallMarks); put("hideonwallColor", hideonwallColor); put("hideonwallFoundColor", hideonwallFoundColor)
        put("recatchMarks", recatchMarks); put("recatchColor", recatchColor)
        put("gemzieAudience", gemzieAudience.name); put("wumpaAudience", wumpaAudience.name)
        put("doomspiralAudience", doomspiralAudience.name); put("macawAudience", macawAudience.name)
        put("subtractTax", subtractTax)
        put("migrated.critterModSettings", critterModSettingsImported); put("migrated.critterModRuns", critterModRunsImported)
        put("migrated.journalSettings", journalSettingsImported); put("migrated.journalRuns", journalRunsImported)
        put("migrated.theme", themeApplied)
    }

    fun save() {
        val path = file ?: return
        runCatching {
            Files.createDirectories(path.parent)
            val temp = path.resolveSibling(path.fileName.toString() + ".tmp")
            Files.newBufferedWriter(temp).use { write().store(it, "Hunting Helper hunting features") }
            Files.move(temp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        }.onFailure { logger.warn("Could not save hunting settings", it) }
    }

    /** Restores every default except the migration markers. */
    internal fun resetForTests() {
        val markers = listOf(critterModSettingsImported, critterModRunsImported, journalSettingsImported, journalRunsImported, themeApplied)
        read(Properties())
        critterModSettingsImported = markers[0]; critterModRunsImported = markers[1]
        journalSettingsImported = markers[2]; journalRunsImported = markers[3]; themeApplied = markers[4]
    }
}

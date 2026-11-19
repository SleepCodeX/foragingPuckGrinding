// Hunting Helper: one-time import of standalone CritterMod and Hunting Helper 1.6 field journal data.
package net.johnceo.sparklingmutuals.hunting

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.SafariEspConfig
import net.johnceo.sparklingmutuals.history.RunHistory
import net.johnceo.sparklingmutuals.history.RunRecord
import net.johnceo.sparklingmutuals.safari.SafariRoster
import org.slf4j.LoggerFactory

/**
 * Reads `config/crittermod.json` / `crittermod-runs.json` (standalone CritterMod) and
 * `config/hunting-helper-field-journal.json` / `hunting-helper-field-runs.json` (Hunting Helper 1.6's journal) once.
 * Settings map onto the matching hunting settings; runs are merged into Sparkling Mutuals' history, de-duplicated by
 * its own merge. Each source has its own persisted marker. Unreadable or partial files are skipped field by field.
 */
object HuntingImport {
    private val logger = LoggerFactory.getLogger("hunting-helper/import")
    const val CM_SETTINGS = "crittermod.json"
    const val CM_RUNS = "crittermod-runs.json"
    const val JOURNAL_SETTINGS = "hunting-helper-field-journal.json"
    const val JOURNAL_RUNS = "hunting-helper-field-runs.json"

    /** What applying one settings file changed. */
    data class SettingsResult(val read: Boolean, val fields: Int, val smChanged: Boolean)

    private fun parse(text: String?): JsonElement? = text?.let { runCatching { JsonParser.parseString(it) }.getOrNull() }
    private fun JsonObject.obj(key: String) = get(key)?.takeIf { it.isJsonObject }?.asJsonObject
    private fun JsonObject.bool(key: String) = get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean
    private fun JsonObject.int(key: String) = get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.let { runCatching { it.asInt }.getOrNull() }
    private fun JsonObject.string(key: String) = get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
    private fun JsonObject.long(key: String) = get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.let { runCatching { it.asLong }.getOrNull() }

    /** CritterMod stores `chroma:alpha:r:g:b` (the format Sparkling Mutuals uses); the journal stored `#AARRGGBB[@period]`. */
    internal fun color(value: String?, fallback: String): String? {
        value ?: return null
        val hex = Regex("#([0-9a-fA-F]{8})(?:@\\d{1,2})?").matchEntire(value.trim())
        if (hex != null) {
            val argb = hex.groupValues[1].toLong(16)
            return "0:${argb shr 24 and 255}:${argb shr 16 and 255}:${argb shr 8 and 255}:${argb and 255}"
        }
        return SafariEspConfig.validColor(value, fallback).takeIf { it == value.trim() }
    }

    /** Applies one CritterMod-shaped settings file. Returns null-safe counts; never throws on bad input. */
    fun applySettings(text: String?): SettingsResult {
        val root = parse(text)?.takeIf { it.isJsonObject }?.asJsonObject ?: return SettingsResult(false, 0, false)
        var fields = 0
        var sm = false
        fun <T> set(value: T?, write: (T) -> Unit) { if (value != null) { write(value); fields++ } }
        root.obj("display")?.let { d ->
            set(d.bool("uniqueOnly")) { HuntingConfig.uniquesOnly = it }
            set(d.bool("showMissing")) { HuntingConfig.missingHud = it }
            set(d.bool("showTrades")) { HuntingConfig.tradeHud = it }
            set(d.bool("highlightTrades")) { HuntingConfig.tradeMarks = it }
            set(d.bool("highlightHideonwalls")) { HuntingConfig.hideonwallMarks = it }
            set(d.bool("recatchHelper")) { HuntingConfig.recatchMarks = it }
            set(d.bool("highlightTroodonWalls")) { HuntingConfig.troodonWallMarks = it }
            set(d.bool("showTroodonWalls")) { HuntingConfig.troodonWallCount = it }
            set(color(d.string("troodonWallColour"), HuntingConfig.troodonWallColor)) { HuntingConfig.troodonWallColor = it }
            set(color(d.string("tradeColour"), HuntingConfig.tradeColor)) { HuntingConfig.tradeColor = it }
            set(color(d.string("hideonwallColour"), HuntingConfig.hideonwallColor)) { HuntingConfig.hideonwallColor = it }
            set(color(d.string("hideonwallFoundColour"), HuntingConfig.hideonwallFoundColor)) { HuntingConfig.hideonwallFoundColor = it }
            set(color(d.string("recatchColour"), HuntingConfig.recatchColor)) { HuntingConfig.recatchColor = it }
            set(d.bool("removeDarkness")) { if (it && !ConfigManager.removeDarkness) { ConfigManager.removeDarkness = true; sm = true } }
        }
        root.obj("alerts")?.let { a ->
            set(a.bool("allButMacawNotify")) { HuntingConfig.allButMacawAlert = it }
            set(a.bool("allDoneNotify")) { HuntingConfig.allDoneAlert = it }
            set(a.bool("traderAlerts")) { HuntingConfig.tradeChat = it }
        }
        root.obj("party")?.let { p ->
            set(p.int("gemzieBroadcast")?.let(Audience::of)) { HuntingConfig.gemzieAudience = it }
            set(p.int("wumpaBroadcast")?.let(Audience::of)) { HuntingConfig.wumpaAudience = it }
            set(p.int("doomspiralBroadcast")?.let(Audience::of)) { HuntingConfig.doomspiralAudience = it }
            set(p.int("milestoneBroadcast")?.let(Audience::of)) { HuntingConfig.milestoneAudience = it }
            set(p.int("macawBroadcast")?.let(Audience::of)) { HuntingConfig.macawAudience = it }
            set(p.int("traderBroadcast")?.let(Audience::of)) { HuntingConfig.tradeAudience = it }
            set(p.bool("acceptSharedTrades")) { HuntingConfig.acceptSharedTrades = it }
        }
        root.obj("profit")?.let { p ->
            set(p.bool("subtractTax")) { HuntingConfig.subtractTax = it }
            set(p.int("priceSource")) { val insta = it != 1; if (ConfigManager.runCoinsInstaSell != insta) { ConfigManager.runCoinsInstaSell = insta; sm = true } }
        }
        return SettingsResult(true, fields, sm)
    }

    /** CritterMod / journal run records as Sparkling Mutuals history rows. Malformed rows are skipped. */
    fun convertRuns(text: String?): List<RunRecord> {
        val array = parse(text)?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            runCatching { element.takeIf { it.isJsonObject }?.asJsonObject?.let(::convert) }.getOrNull()
        }
    }

    private fun counts(o: JsonObject, key: String): Map<String, Int> {
        val map = o.obj(key) ?: return emptyMap()
        return buildMap {
            map.entrySet().forEach { (name, value) ->
                val critter = SafariRoster.named(name)?.name ?: return@forEach
                val count = value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt ?: return@forEach
                if (count > 0) put(critter, count)
            }
        }
    }

    private fun convert(o: JsonObject): RunRecord? {
        val start = o.long("started")?.takeIf { it > 0 } ?: return null
        val end = o.long("ended") ?: start
        val own = counts(o, "own")
        val shared = counts(o, "shared")
        if (own.isEmpty() && shared.isEmpty()) return null
        val species = own.keys + shared.keys
        return RunRecord(start = start, length = (end - start).coerceAtLeast(0), mode = "UNIQUE", modeEstimated = true,
            party = species.size, you = own.size, caught = own.values.sum(), lootShares = shared.values.sum(),
            shards = LinkedHashMap(counts(o, "shards")), catches = LinkedHashMap(own),
            macaws = (own["Macaw"] ?: 0) + (shared["Macaw"] ?: 0), imported = true)
    }

    /** Both run files, without the copies the journal made of CritterMod's file. */
    fun collectRuns(vararg texts: String?): List<RunRecord> = texts.flatMap(::convertRuns).distinctBy { it.start }

    private fun read(path: Path): String? = if (Files.isRegularFile(path)) runCatching { Files.readString(path) }
        .onFailure { logger.warn("Could not read {}", path, it) }.getOrNull() else null

    /** Runs each import whose marker is not set yet. Returns a short summary for chat, or null when nothing ran. */
    fun runOnce(directory: Path = FabricLoader.getInstance().configDir): String? {
        val parts = mutableListOf<String>()
        var smChanged = false
        fun settings(file: String, done: Boolean, mark: () -> Unit) {
            if (done) return
            val text = read(directory.resolve(file)) ?: return
            val result = applySettings(text)
            if (!result.read) logger.warn("{} is not valid JSON; its settings were skipped", file)
            else parts += "${result.fields} settings from $file"
            smChanged = smChanged || result.smChanged
            mark()
        }
        settings(CM_SETTINGS, HuntingConfig.critterModSettingsImported) { HuntingConfig.critterModSettingsImported = true }
        settings(JOURNAL_SETTINGS, HuntingConfig.journalSettingsImported) { HuntingConfig.journalSettingsImported = true }
        val sources = listOfNotNull(
            CM_RUNS.takeUnless { HuntingConfig.critterModRunsImported },
            JOURNAL_RUNS.takeUnless { HuntingConfig.journalRunsImported })
            .associateWith { read(directory.resolve(it)) }.filterValues { it != null }
        if (sources.isNotEmpty()) {
            val found = collectRuns(*sources.values.toTypedArray())
            val added = if (found.isEmpty()) 0 else runCatching { RunHistory.importRuns(found) }
                .onFailure { logger.warn("Could not merge CritterMod run history", it) }.getOrNull()
            if (added != null) {
                if (CM_RUNS in sources) HuntingConfig.critterModRunsImported = true
                if (JOURNAL_RUNS in sources) HuntingConfig.journalRunsImported = true
                parts += "$added of ${found.size} runs"
            }
        }
        if (smChanged) ConfigManager.save()
        HuntingConfig.save()
        return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    fun runManual() {
        HuntingConfig.critterModSettingsImported = false; HuntingConfig.journalSettingsImported = false
        HuntingConfig.critterModRunsImported = false; HuntingConfig.journalRunsImported = false
        val summary = runOnce()
        HuntingOutput.local(summary?.let { "Imported $it." } ?: "No CritterMod or field journal files found in the config folder.")
    }
}

// Hunting Helper: CritterMod features ported into Sparkling Mutuals (see docs/SM-UPSTREAM.md).
package net.johnceo.sparklingmutuals.hunting

import net.johnceo.sparklingmutuals.compat.smHideGui
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.hud.UiIcons
import net.johnceo.sparklingmutuals.safari.DoomspiralLottery
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

/** The second HUD ("Missing in this area") and the Hunter trades box, drawn with Sparkling Mutuals' panels. */
object HuntingHuds {
    private val DIM = 0xFF8C9099.toInt()
    private val DONE = 0xFF55FF55.toInt()
    private val ITEM = 0xFFFFFF55.toInt()

    /** Live critters near you that are in line of sight, by species. */
    fun nearby(): Map<String, Int> = CritterEntities.all().filter { it.mob() != null && it.unobstructed() }
        .groupingBy { it.critter().name }.eachCount()

    /**
     * CritterMod's missing list for [biome]: a "N left" title, one row per species still owed. A species with a quota
     * reads "Gazer 2/4" until the quota is reached (or after one catch with Uniques only), then drops off the list.
     */
    fun missingPanel(biome: SafariBiome, source: HuntingCompletion.Source, uniquesOnly: Boolean,
                     nearby: Map<String, Int> = emptyMap(), attempts: (String) -> Int = { 0 }): HudPanel {
        val area = HuntingCompletion.area(biome, source, uniquesOnly)
        val rows = mutableListOf<HudRow>()
        area.missing.forEach { entry ->
            val name = entry.critter.name
            val notes = buildList {
                entry.note.takeIf { !uniquesOnly }?.let(::add)
                nearby[name]?.takeIf { it > 0 }?.let { add("$it near") }
                entry.located?.takeIf { it > 0 }?.let { add("$it located") }
                if (isEmpty()) attempts(name).takeIf { it > 0 }?.let { add("$it tried") }
            }
            rows += HudRow(name, notes.joinToString(" · ").ifEmpty { null }, entry.critter.color, DIM, icon = null)
        }
        if (area.unavailable.isNotEmpty()) {
            if (rows.isNotEmpty()) rows += HudRow(icon = null)
            area.unavailable.forEach { rows += HudRow(it.name, "none this run", DIM, DIM, icon = null) }
        }
        // Done for the list, but creatures the run located are still shards to take.
        fun extra(entry: HuntingCompletion.Entry) = listOfNotNull(nearby[entry.critter.name]?.takeIf { it > 0 }?.let { "$it near" },
            entry.located?.takeIf { it > 0 }?.let { "$it located" }).joinToString(" · ")
        val alsoHere = area.done.filter { extra(it).isNotEmpty() }
        if (alsoHere.isNotEmpty()) {
            rows += HudRow(icon = null)
            rows += HudRow("also here", null, DIM, DIM, icon = null)
            alsoHere.forEach { rows += HudRow(it.critter.name, extra(it), DONE, DIM, icon = null) }
        }
        val left = area.missing.size
        return when {
            left > 0 -> HudPanel("${biome.label} Biome — $left left", biome.color, rows, UiIcons.forLabel(biome.label))
            rows.isEmpty() -> HudPanel("${biome.label} Biome", biome.color,
                listOf(HudRow("all ${biome.critters.size} caught", null, DONE, DONE, complete = true)), UiIcons.forLabel(biome.label))
            else -> HudPanel("${biome.label} Biome — all caught", DONE, rows, UiIcons.forLabel(biome.label))
        }
    }

    fun missingPanel(run: SafariRun?, biome: SafariBiome): HudPanel {
        val panel = missingPanel(biome, HuntingCompletion.of(run), HuntingConfig.uniquesOnly,
            if (HuntingConfig.missingNearby) nearby() else emptyMap()) { HuntingRuns.attempts(run, it) }
        if (biome != SafariBiome.ICY || !HuntingConfig.troodonWallCount) return panel
        val walls = TroodonWalls.walls().takeIf { it.isNotEmpty() } ?: return panel
        val intact = walls.count { it.state() == TroodonWalls.State.INTACT }
        val unknown = walls.count { it.state() == TroodonWalls.State.UNKNOWN }
        val row = if (intact == 0 && unknown == 0) HudRow("Troodon walls all broken", null, DONE, DONE, icon = null)
            else HudRow("Troodon walls to break", intact.toString() + if (unknown > 0) " (+$unknown?)" else "", 0xFFFFAA00.toInt(), DIM, icon = null)
        return HudPanel(panel.title, panel.titleColor, panel.rows + HudRow(icon = null) + row, panel.titleIcon)
    }

    /** Trades found this run, nearest first; null when there are none. */
    fun tradesPanel(trades: List<TraderWatch.Trade> = TraderWatch.found()): HudPanel? {
        if (trades.isEmpty()) return null
        fun distance(trade: TraderWatch.Trade) = trade.spot()?.distanceFromPlayer()?.takeIf { it >= 0 } ?: Double.MAX_VALUE
        val rows = trades.sortedBy(::distance).flatMap { trade ->
            val spot = trade.spot()
            val where = when {
                spot == null -> trade.npc()
                spot.distanceFromPlayer() < 0 -> spot.describe()
                else -> "${spot.describe()} · ${Math.round(spot.distanceFromPlayer())}m"
            }
            listOf(HudRow("${trade.critter().name} ← ${trade.item()}", null, trade.critter().color, ITEM, icon = null),
                HudRow("  $where", null, DIM, DIM, icon = null))
        }
        return HudPanel("Hunter Trades", HudRow.GOLD, rows, null)
    }

    fun previews(): Map<SafariHud, HudPanel> {
        val sample = object : HuntingCompletion.Source {
            override fun caught(name: String) = when (name) { "Gazer" -> 2; "Areita", "Bloodbat", "Duplico", "Solsnatcher" -> 1; else -> 0 }
            override fun unavailable(name: String) = false
        }
        val trades = HudPanel("Hunter Trades", HudRow.GOLD, listOf(
            HudRow("Nozzlenose ← Yogi Berry", null, 0xFF5555FF.toInt(), ITEM, icon = null),
            HudRow("  Icy -106 87 -7 · 34m", null, DIM, DIM, icon = null)), null)
        return mapOf(SafariHud.HUNTING_MISSING to missingPanel(SafariBiome.HAUNTED, sample, false, mapOf("Litterbug" to 2)),
            SafariHud.HUNTING_TRADES to trades)
    }

    fun render(graphics: GuiGraphicsExtractor) {
        val client = Minecraft.getInstance()
        if (client.player == null || client.options.smHideGui || client.smScreen is ContestGui || DoomspiralLottery.active) return
        if (!SafariAssist.inSafari) return
        val run = SafariTracking.ledger.current ?: SafariTracking.run
        if (HuntingConfig.missingHud) SafariAssist.biome?.let { missingPanel(run, it).draw(graphics, SafariHud.HUNTING_MISSING) }
        if (HuntingConfig.tradeHud && HuntingOutput.trades()) tradesPanel()?.draw(graphics, SafariHud.HUNTING_TRADES)
    }
}

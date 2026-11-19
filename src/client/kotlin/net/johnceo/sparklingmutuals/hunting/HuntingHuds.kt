// Hunting Helper: CritterMod features ported into Sparkling Mutuals (see docs/SM-UPSTREAM.md).
package net.johnceo.sparklingmutuals.hunting

import net.johnceo.sparklingmutuals.compat.smHideGui
import net.johnceo.sparklingmutuals.compat.smScreen
import net.johnceo.sparklingmutuals.contest.ContestGui
import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.hud.SafariHud
import net.johnceo.sparklingmutuals.safari.DoomspiralLottery
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

/** The Hunter trades box, drawn with Sparkling Mutuals' panels. The missing list lives in SM's panel (MissingExtras). */
object HuntingHuds {
    private val DIM = 0xFF8C9099.toInt()
    private val ITEM = 0xFFFFFF55.toInt()

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

    fun previews(): Map<SafariHud, HudPanel> = mapOf(SafariHud.HUNTING_TRADES to HudPanel("Hunter Trades", HudRow.GOLD, listOf(
        HudRow("Nozzlenose ← Yogi Berry", null, 0xFF5555FF.toInt(), ITEM, icon = null),
        HudRow("  Icy -106 87 -7 · 34m", null, DIM, DIM, icon = null)), null))

    fun render(graphics: GuiGraphicsExtractor) {
        val client = Minecraft.getInstance()
        if (client.player == null || client.options.smHideGui || client.smScreen is ContestGui || DoomspiralLottery.active) return
        if (!SafariAssist.inSafari) return
        if (HuntingConfig.tradeHud && HuntingOutput.trades()) tradesPanel()?.draw(graphics, SafariHud.HUNTING_TRADES)
    }
}

// Hunting Helper: extras on Sparkling Mutuals' missing panel (installed through SafariPanels.missingExtras).
package net.johnceo.sparklingmutuals.hunting

import net.johnceo.sparklingmutuals.hud.HudPanel
import net.johnceo.sparklingmutuals.hud.HudRow
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear

/**
 * Decorates SM's own missing list instead of drawing a second one. SM's rows, wording and completion states stay as
 * they are; on top of them:
 *
 * - **Quota rows** (Uniques only off): Gemzie, Troodon, Gazer and Scrappy stay listed until their roster quota, as
 *   "Gazer 2/4", even where SM's mode already dropped them, and the "N left" title counts them. Where SM already shows
 *   its own counter (Full Clear's "3/4 captured", read from the same captureProgress with the same totals), SM's text
 *   is kept and no second counter is added. With Uniques only on, the species list is exactly SM's.
 * - **"· N located"** per row, from the run's located creatures ([HuntingCompletion.located]).
 * - **"also here"** rows, gray, for species already done that still have located creatures.
 * - In Icy, the Troodon walls still standing.
 *
 * Sparkling mode keeps SM's panel exactly as upstream.
 */
object MissingExtras {
    private val DIM = 0xFF8C9099.toInt()
    data class Options(val uniquesOnly: Boolean, val located: Boolean, val alsoHere: Boolean, val troodonWalls: Boolean = false)

    fun install() {
        net.johnceo.sparklingmutuals.safari.ui.SafariPanels.missingExtras = { run, biome, panel -> decorate(run, biome, panel) }
    }

    /** Live settings and run; SM's panel comes back untouched when extras are off or in Sparkling mode. */
    fun decorate(run: SafariRun?, biome: SafariBiome, panel: HudPanel): HudPanel {
        if (!HuntingConfig.missingExtras) return panel
        val options = Options(HuntingConfig.uniquesOnly, HuntingConfig.missingLocated, HuntingConfig.missingAlsoHere, HuntingConfig.troodonWallCount)
        return decorate(panel, biome, HuntingCompletion.of(run), options, SafariFullClear.mode,
            if (options.troodonWalls) troodonRow(biome) else null)
    }

    fun decorate(panel: HudPanel, biome: SafariBiome, source: HuntingCompletion.Source, options: Options,
                 mode: SafariMode, extraRow: HudRow? = null): HudPanel {
        if (mode == SafariMode.SPARKLING) return panel
        val speciesNames = biome.critters.map { it.name }.toSet()
        val smRows = panel.rows.filter { it.label in speciesNames }.associateBy { it.label }
        fun located(name: String) = if (!options.located) null
            else SafariRoster.named(name)?.let { HuntingCompletion.located(it, source) }?.takeIf { it > 0 }
        fun annotate(text: String?, name: String) = located(name)?.let { n -> listOfNotNull(text, "$n located").joinToString(" · ") } ?: text
        val species = mutableListOf<HudRow>()
        for (critter in biome.critters) {
            val name = critter.name
            val sm = smRows[name]
            val target = HuntingCompletion.target(critter, options.uniquesOnly)
            val caught = source.caught(name)
            val quotaOwed = !options.uniquesOnly && target > 1 && caught < target && !HuntingCompletion.unavailable(critter, source)
            when {
                // SM's own counter wins over ours (same source, same totals); otherwise show the quota.
                sm != null -> species += sm.copy(value = annotate(sm.value ?: if (quotaOwed) "$caught/$target" else null, name))
                quotaOwed -> species += HudRow(name, annotate("$caught/$target", name), critter.color, HudRow.GRAY, icon = null)
            }
        }
        val alsoHere = if (!options.alsoHere) emptyList() else biome.critters.filter { critter ->
            species.none { it.label == critter.name } && located(critter.name) != null
        }.map { HudRow(it.name, "${located(it.name)} located", DIM, DIM, icon = null) }
        if (species.map { it.label } == smRows.keys.toList() && species.map { it.value } == smRows.values.map { it.value } &&
            alsoHere.isEmpty() && extraRow == null) return panel
        val rows = mutableListOf<HudRow>()
        // No species owed: keep SM's completion row ("All caught!", "All caught and checked!").
        if (species.isEmpty()) rows += panel.rows.filter { it.label !in speciesNames } else rows += species
        if (alsoHere.isNotEmpty()) { rows += HudRow(icon = null); rows += HudRow("also here", null, DIM, DIM, icon = null); rows += alsoHere }
        extraRow?.let { rows += HudRow(icon = null); rows += it }
        val title = if (species.size == smRows.size) panel.title else panel.title.replace(Regex("— \\d+ left$"), "— ${species.size} left")
        return HudPanel(title, panel.titleColor, rows, panel.titleIcon)
    }

    private fun troodonRow(biome: SafariBiome): HudRow? {
        if (biome != SafariBiome.ICY) return null
        val walls = TroodonWalls.walls().takeIf { it.isNotEmpty() } ?: return null
        val intact = walls.count { it.state() == TroodonWalls.State.INTACT }
        val unknown = walls.count { it.state() == TroodonWalls.State.UNKNOWN }
        return if (intact == 0 && unknown == 0) HudRow("Troodon walls all broken", null, 0xFF55FF55.toInt(), 0xFF55FF55.toInt(), icon = null)
            else HudRow("Troodon walls to break", intact.toString() + if (unknown > 0) " (+$unknown?)" else "", 0xFFFFAA00.toInt(), DIM, icon = null)
    }
}

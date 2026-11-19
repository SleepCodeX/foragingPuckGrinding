// Hunting Helper: CritterMod's "done with a species" rule, counted from the Sparkling Mutuals run ledger.
package net.johnceo.sparklingmutuals.hunting

import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCritter
import net.johnceo.sparklingmutuals.safari.SafariMode
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun

/**
 * One source and one rule for the "Missing in this area" HUD and the whole-run announcements.
 *
 * Count: the run ledger's party-wide captures ([SafariRun.captureProgress]: your catches, loot shares and
 * party-sync captures). Target: [SafariCritter.required], Sparkling Mutuals' roster quota (Gemzie 3, Troodon 3,
 * Gazer 4, Scrappy 3, everything else 1), or 1 with "Uniques only". The roster quotas come from CritterMod's table;
 * Sparkling Mutuals adds Scrappy. A species the run can no longer produce ([SafariRun.uniqueUnavailable]) counts
 * as done while none was caught ("none this run").
 */
object HuntingCompletion {
    /** What a run knows about a species. */
    interface Source {
        fun caught(name: String): Int
        fun unavailable(name: String): Boolean
        fun sharedDone(name: String): Boolean = false
    }

    data class Entry(val critter: SafariCritter, val caught: Int, val target: Int) {
        val note get() = if (target > 1) "$caught/$target" else null
    }
    data class Area(val biome: SafariBiome, val missing: List<Entry>, val unavailable: List<SafariCritter>, val done: List<Entry>)

    fun of(run: SafariRun?): Source = object : Source {
        override fun caught(name: String) = run?.captureProgress(name) ?: 0
        override fun unavailable(name: String) = run?.uniqueUnavailable(name) == true
        override fun sharedDone(name: String) = run?.sharedCritterComplete(name, SafariMode.UNIQUE) == true
    }

    fun target(critter: SafariCritter, uniquesOnly: Boolean) = critter.required(uniquesOnly)

    fun unavailable(critter: SafariCritter, source: Source) = source.caught(critter.name) == 0 && source.unavailable(critter.name)

    fun done(critter: SafariCritter, source: Source, uniquesOnly: Boolean): Boolean {
        val caught = source.caught(critter.name)
        if (caught >= target(critter, uniquesOnly)) return true
        if (caught == 0 && source.unavailable(critter.name)) return true
        return uniquesOnly && source.sharedDone(critter.name)
    }

    fun area(biome: SafariBiome, source: Source, uniquesOnly: Boolean): Area {
        val missing = mutableListOf<Entry>()
        val gone = mutableListOf<SafariCritter>()
        val done = mutableListOf<Entry>()
        biome.critters.forEach { critter ->
            val entry = Entry(critter, source.caught(critter.name), target(critter, uniquesOnly))
            when {
                unavailable(critter, source) -> gone += critter
                done(critter, source, uniquesOnly) -> done += entry
                else -> missing += entry
            }
        }
        return Area(biome, missing, gone, done)
    }

    /** Every species done except, possibly, the Macaw. */
    fun allButMacaw(source: Source, uniquesOnly: Boolean) =
        SafariRoster.all.all { it.name == "Macaw" || done(it, source, uniquesOnly) }

    fun allDone(source: Source, uniquesOnly: Boolean) = SafariRoster.all.all { done(it, source, uniquesOnly) }
}

/** The two whole-run milestones, once each per run, with CritterMod's SessionManager suppression. */
class MilestoneTracker {
    enum class Milestone(val text: String) { ALL_BUT_MACAW("Everything except Macaw done!"), ALL_DONE("Everything Done!") }
    var allButMacawAnnounced = false
        private set
    var allDoneAnnounced = false
        private set

    /**
     * The milestone reached since the last call, or null. They are mutually exclusive: when one catch completes
     * everything at once, only "Everything Done!" fires and the weaker one is marked as already announced.
     */
    fun update(source: HuntingCompletion.Source, uniquesOnly: Boolean): Milestone? {
        if (!allDoneAnnounced && HuntingCompletion.allDone(source, uniquesOnly)) {
            allDoneAnnounced = true; allButMacawAnnounced = true
            return Milestone.ALL_DONE
        }
        if (!allButMacawAnnounced && HuntingCompletion.allButMacaw(source, uniquesOnly)) {
            allButMacawAnnounced = true
            return Milestone.ALL_BUT_MACAW
        }
        return null
    }
}

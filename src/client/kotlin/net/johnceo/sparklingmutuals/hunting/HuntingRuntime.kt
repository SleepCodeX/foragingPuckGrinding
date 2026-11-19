// Hunting Helper: CritterMod features ported into Sparkling Mutuals (see docs/SM-UPSTREAM.md).
package net.johnceo.sparklingmutuals.hunting

import java.util.WeakHashMap
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.johnceo.sparklingmutuals.history.RunHistory
import net.johnceo.sparklingmutuals.hud.BannerKind
import net.johnceo.sparklingmutuals.hud.SafariBanners
import net.johnceo.sparklingmutuals.safari.SafariAssist
import net.johnceo.sparklingmutuals.safari.SafariBiome
import net.johnceo.sparklingmutuals.safari.SafariCatch
import net.johnceo.sparklingmutuals.safari.SafariMessages
import net.johnceo.sparklingmutuals.safari.SafariRoster
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules
import net.minecraft.client.Minecraft

/** Capsules thrown and escapes per species, for one run. Catches themselves stay in the run ledger. */
class RunStats {
    val attempts = linkedMapOf<String, Int>()
    val escapes = linkedMapOf<String, Int>()
}

/** Per-run extras keyed by the Sparkling Mutuals run they belong to, so they reset exactly when its ledger does. */
object HuntingRuns {
    private val stats = WeakHashMap<SafariRun, RunStats>()
    fun of(run: SafariRun?): RunStats? = run?.let { stats.getOrPut(it, ::RunStats) }
    fun attempts(run: SafariRun?, species: String) = run?.let { stats[it]?.attempts?.get(species) } ?: 0
    fun escapes(run: SafariRun?, species: String) = run?.let { stats[it]?.escapes?.get(species) } ?: 0

    /** Who caught [species] this run and how often, from the ledger's own catches (you, then loot shares). */
    fun catchers(run: SafariRun?, species: String, self: String): List<Pair<String, Int>> {
        val counts = linkedMapOf<String, Int>()
        run?.catches?.filter { it.critter.name == species }?.forEach { catch ->
            val name = if (catch.personal) self else catch.catcherName ?: "?"
            val key = counts.keys.firstOrNull { it.equals(name, true) } ?: name
            counts.merge(key, 1, Int::plus)
        }
        return counts.toList()
    }
}

/**
 * Reads chat for the ported features: capsule throws and escapes, Hunter dialogue and shared trades, Hideonwall
 * and recatch retirement, encounter broadcasts. It counts no catches: the run ledger owns those. Both delivery
 * paths are read, so lines hidden by Sparkling Mutuals' chat helper or cancelled by other mods still arrive.
 */
object HuntingChat {
    fun register() {
        ClientReceiveMessageEvents.GAME.register { message, overlay ->
            if (!overlay) Minecraft.getInstance().execute { receive(message.string) }
        }
        // GAME and GAME_CANCELED are mutually exclusive. Visible and display-filtered lines already reach the run
        // ledger and history through GAME; forward captures and shards from the canceled path too.
        ClientReceiveMessageEvents.GAME_CANCELED.register { message, overlay ->
            if (overlay) return@register
            Minecraft.getInstance().execute {
                SafariMessages.lines(message.string).forEach { line ->
                    if (SafariCatch.parse(line) != null) SafariTracking.receive(line)
                }
                RunHistory.receiveCanceled(message.string)
                receive(message.string)
            }
        }
    }

    fun receive(raw: String) {
        val run = SafariTracking.ledger.current
        // A player can type a literal "\n": judge the whole message first, so typed text is never split into
        // pieces that look like game or party lines (fake trades, waypoints, broadcasts).
        val whole = PlayerChat.clean(raw)
        if (PlayerChat.said(whole.lineSequence().first())) {
            if (SafariAssist.inSafari || run != null) TraderWatch.onChatMessage(whole)
            return
        }
        for (line in PlayerChat.lines(raw)) {
            // Anything a player can type, a player can quote: only the shared-trade reader wants these.
            if (PlayerChat.said(line)) {
                if (SafariAssist.inSafari || run != null) TraderWatch.onChatMessage(line)
                continue
            }
            if (!SafariAssist.inSafari && run == null) continue
            SafariEspRules.thrownSpecies(line)?.let(SafariRoster::named)?.let { critter ->
                HuntingRuns.of(run)?.attempts?.merge(critter.name, 1, Int::plus)
                RecatchSpots.attempt(critter)
            }
            SafariEspRules.escapedSpecies(line)?.let(SafariRoster::named)?.let { critter ->
                HuntingRuns.of(run)?.escapes?.merge(critter.name, 1, Int::plus)
                RecatchSpots.escaped(critter)
            }
            SafariCatch.parse(line)?.let { catch ->
                if (catch.personal) RecatchSpots.caught(catch.critter)
                HideonwallSpots.caught(catch.critter.name)
            }
            HuntingBroadcasts.line(line)
            TraderWatch.onChatMessage(line)
        }
    }
}

/**
 * CritterMod's encounter broadcasts. Sparkling Mutuals already shows the banners and announces sparklings and
 * hotspots, so only the party/all-chat lines it has no equivalent for are sent from here, each to its own audience.
 */
object HuntingBroadcasts {
    enum class Stage { READY, STARTED, DONE }
    private const val COOLDOWN = 20_000L
    private const val MACAW_COOLDOWN = 60_000L
    private val lastFired = mutableMapOf<String, Long>()
    private var owner: SafariRun? = null
    private var gemzieBase: Int? = null
    private var wumpa = 0
    private var doomspiral = 0
    private var macawLoaded = false
    private var macawAt = 0L
    private val birdfeeder = Regex("^\\S+ ([A-Za-z ]+?)s? (?:were|was) attracted to the Birdfeeder!$")

    fun reset() { lastFired.clear(); owner = null; gemzieBase = null; wumpa = 0; doomspiral = 0; macawLoaded = false; macawAt = 0 }

    fun line(line: String) {
        val run = SafariTracking.ledger.current
        when {
            line.startsWith("A rumbling sound can be heard") -> { gemzieBase = run?.count("Gemzie") ?: 0; fire("Gemzie", Stage.READY) }
            line.startsWith("You hear the sound of massive footsteps") -> fire("Wumpa", Stage.READY)
            line.startsWith("The Wumpa has awoken") -> fire("Wumpa", Stage.STARTED)
            line.startsWith("The cave opens up again") -> fire("Wumpa", Stage.DONE)
            line.startsWith("You used the Soothing Incense to light the candle") -> fire("Doomspiral", Stage.READY)
            line.startsWith("Your ritual summoned a Doomspiral") -> fire("Doomspiral", Stage.STARTED)
            line.startsWith("The Doomspiral retreats back underground") -> fire("Doomspiral", Stage.DONE)
        }
        birdfeeder.matchEntire(line)?.let { if (SafariRoster.named(it.groupValues[1].trim())?.name == "Macaw") macaw(null) }
    }

    /** Catches end Wumpa and Doomspiral; three Gemzies after a chamber opens end Gemzie. Read off the run ledger. */
    fun tick(run: SafariRun?) {
        if (run !== owner) { owner = run; gemzieBase = null; wumpa = run?.count("Wumpa") ?: 0; doomspiral = run?.count("Doomspiral") ?: 0 }
        run ?: return
        val w = run.count("Wumpa"); if (w > wumpa) fire("Wumpa", Stage.DONE); wumpa = w
        val d = run.count("Doomspiral"); if (d > doomspiral) fire("Doomspiral", Stage.DONE); doomspiral = d
        gemzieBase?.let { if (run.count("Gemzie") - it >= 3) { gemzieBase = null; fire("Gemzie", Stage.DONE) } }
        if (HuntingConfig.macawAudience != Audience.NOBODY) {
            val seen = CritterEntities.all().firstOrNull { it.visible() && it.critter().name == "Macaw" }
            if (seen != null && !macawLoaded) macaw(seen.body().blockPosition())
            macawLoaded = seen != null
        }
    }

    private fun macaw(at: net.minecraft.core.BlockPos?) {
        val now = System.currentTimeMillis()
        if (now - macawAt < MACAW_COOLDOWN) return
        macawAt = now
        val where = at?.let { " (${SafariBiome.FOREST.label} ${it.x} ${it.y} ${it.z})" }.orEmpty()
        HuntingOutput.post(HuntingConfig.macawAudience, "Macaw spawned!$where")
    }

    private fun audience(boss: String) = when (boss) {
        "Gemzie" -> HuntingConfig.gemzieAudience
        "Wumpa" -> HuntingConfig.wumpaAudience
        else -> HuntingConfig.doomspiralAudience
    }

    private fun fire(boss: String, stage: Stage) {
        // In a biome-each split only the player standing there should call it; unknown position fails open.
        val home = SafariRoster.named(boss)?.biome
        val here = SafariAssist.biome
        if (home != null && here != null && home != here) return
        val now = System.currentTimeMillis()
        val key = "$boss:$stage"
        if (boss != "Gemzie" && now - (lastFired[key] ?: Long.MIN_VALUE / 2) < COOLDOWN) return
        lastFired[key] = now
        HuntingOutput.post(audience(boss), "$boss ${stage.name.lowercase()}")
    }
}

/** "Everything except Macaw done!" and "Everything Done!", once each per run, counted like the second HUD. */
object HuntingAnnouncer {
    private val trackers = WeakHashMap<SafariRun, MilestoneTracker>()
    /** The last announcement shown, for the client game tests. */
    @JvmStatic var last: String? = null
        private set

    fun tick(run: SafariRun?) {
        run ?: return
        val reached = trackers.getOrPut(run, ::MilestoneTracker).update(HuntingCompletion.of(run), HuntingConfig.uniquesOnly) ?: return
        val wanted = when (reached) {
            MilestoneTracker.Milestone.ALL_DONE -> HuntingConfig.allDoneAlert
            MilestoneTracker.Milestone.ALL_BUT_MACAW -> HuntingConfig.allButMacawAlert
        }
        if (wanted) announce(reached.text)
    }

    fun announce(text: String) {
        last = text
        // Shown through Sparkling Mutuals' banner queue, so it never overlaps its own alerts.
        SafariBanners.fire(BannerKind.RUN_COMPLETE, text = text, preview = true)
        HuntingOutput.local(text, 0xFF55FF55.toInt())
        HuntingOutput.post(HuntingConfig.milestoneAudience, text)
    }
}

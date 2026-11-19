// Hunting Helper: CritterMod features ported into Sparkling Mutuals (see docs/SM-UPSTREAM.md).
package net.johnceo.sparklingmutuals.hunting

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.johnceo.sparklingmutuals.config.ConfigManager
import net.johnceo.sparklingmutuals.config.ConfigStorage
import net.johnceo.sparklingmutuals.safari.SafariRun
import net.johnceo.sparklingmutuals.safari.SafariTracking
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

/** Starts the hunting features. Called by Hunting Helper's client entrypoint, after Sparkling Mutuals initialised. */
object HuntingFeatures {
    private val logger = LoggerFactory.getLogger("hunting-helper")
    private var ticks = 0
    private var run: SafariRun? = null

    @JvmStatic fun register() {
        HuntingConfig.init(ConfigStorage.directory)
        applyThemeOnce()
        HuntingOutput.warnIfDoubled()
        HuntingChat.register()
        MissingExtras.install()
        ClientTickEvents.END_CLIENT_TICK.register(::tick)
        ClientPlayConnectionEvents.JOIN.register { _, _, _ -> reset() }
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ -> reset() }
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("foragingpuckgrinding", "hunting_huds")) { graphics, _ -> HuntingHuds.render(graphics) }
        WaypointRenderer.register()
        runCatching { HuntingImport.runOnce() }.onSuccess { summary ->
            if (summary != null) logger.info("Imported CritterMod / field journal data: {}", summary)
        }.onFailure { logger.warn("CritterMod import failed", it) }
    }

    /** Sparkling Mutuals' settings screens use Hunting Helper's matte charcoal palette from the first start on. */
    private fun applyThemeOnce() {
        if (HuntingConfig.themeApplied) return
        ConfigManager.settingsTheme = 0
        runCatching { ConfigManager.save() }
        HuntingConfig.themeApplied = true
        HuntingConfig.save()
    }

    private fun reset() {
        ChatQueue.clear()
        TraderWatch.reset()
        HideonwallSpots.reset()
        RecatchSpots.reset()
        TroodonWalls.reset()
        HuntingBroadcasts.reset()
    }

    private fun tick(client: Minecraft) {
        val current = SafariTracking.ledger.current
        // A new run starts the run-scoped helpers afresh, as CritterMod's session start did; leaving keeps them.
        if (current != null && current !== run) {
            if (run != null) { TraderWatch.reset(); HideonwallSpots.reset(); RecatchSpots.reset(); TroodonWalls.reset() }
            run = current
        }
        CritterEntities.tick()
        HideonwallSpots.tick()
        RecatchSpots.tick()
        TroodonWalls.tick()
        TraderWatch.tick()
        if (++ticks % 10 == 0) {
            HuntingBroadcasts.tick(current)
            HuntingAnnouncer.tick(current)
        }
        ChatQueue.tick(client)
    }
}

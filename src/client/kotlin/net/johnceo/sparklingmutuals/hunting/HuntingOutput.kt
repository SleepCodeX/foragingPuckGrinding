// Hunting Helper: CritterMod features ported into Sparkling Mutuals (see docs/SM-UPSTREAM.md).
package net.johnceo.sparklingmutuals.hunting

import net.fabricmc.loader.api.FabricLoader
import net.johnceo.sparklingmutuals.config.SafariTheme
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.slf4j.LoggerFactory

/**
 * Gates and outgoing chat for the ported CritterMod features. When the standalone CritterMod is also installed it
 * already draws trades and marks and posts the same broadcasts, so ours stay off to avoid doubles.
 */
object HuntingOutput {
    private val logger = LoggerFactory.getLogger("hunting-helper")
    @JvmStatic val critterModLoaded: Boolean by lazy { runCatching { FabricLoader.getInstance().isModLoaded("crittermod") }.getOrDefault(false) }

    fun warnIfDoubled() {
        if (critterModLoaded) logger.warn("CritterMod is also installed: Hunting Helper's Hunter trade HUD, trade/perch/recatch marks " +
            "and CritterMod-style broadcasts are disabled to avoid doubles. Remove one of the two mods to use the other's.")
    }

    @JvmStatic fun trades() = !critterModLoaded
    @JvmStatic fun tradeMarks() = !critterModLoaded && HuntingConfig.tradeMarks
    @JvmStatic fun hideonwallMarks() = !critterModLoaded && HuntingConfig.hideonwallMarks
    @JvmStatic fun recatchMarks() = !critterModLoaded && HuntingConfig.recatchMarks
    @JvmStatic fun troodonWallMarks() = !critterModLoaded && HuntingConfig.troodonWallMarks

    /** `[HH] ` in the Sparkling Mutuals chat accent. */
    @JvmStatic fun prefix(): MutableComponent = Component.literal("[HH] ").withStyle { it.withColor(SafariTheme.CHAT_ACCENT) }

    @JvmStatic @JvmOverloads fun local(text: String, color: Int = SafariTheme.CHAT_TEXT) {
        val client = Minecraft.getInstance()
        if (client.player == null) return
        client.gui.hud.chat.addClientSystemMessage(prefix().append(Component.literal(text).withStyle { it.withColor(color) }))
    }

    /** Sends one line to whoever [to] names, or nowhere. Queued, so a burst cannot trip Hypixel's chat limit. */
    @JvmStatic fun post(to: Audience, message: String) {
        val command = to.command ?: return
        if (critterModLoaded) return
        ChatQueue.enqueue("$command $message")
    }
}

/** Outgoing commands, one at a time with a gap: Hypixel swallows chat sent in bursts. */
object ChatQueue {
    private const val GAP_MILLIS = 1200L
    private const val MAX_LENGTH = 250
    private val pending = ArrayDeque<String>()
    private var nextSend = 0L

    @JvmStatic fun enqueue(command: String) { pending.addLast(command.take(MAX_LENGTH)) }
    @JvmStatic fun pendingCount() = pending.size
    @JvmStatic fun clear() = pending.clear()

    fun tick(client: Minecraft) {
        if (pending.isEmpty()) return
        val now = System.currentTimeMillis()
        if (now < nextSend) return
        val connection = client.player?.connection ?: run { pending.clear(); return }
        connection.sendCommand(pending.removeFirst())
        nextSend = now + GAP_MILLIS
    }
}

package dev.sleepcodex.foragingpuckgrinding;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.johnceo.sparklingmutuals.safari.SafariCatch;
import net.johnceo.sparklingmutuals.safari.SafariMessages;
import net.johnceo.sparklingmutuals.safari.SafariTracking;
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class VexTracerClient implements ClientModInitializer {
    static final String MOD_ID = "foragingpuckgrinding";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private final TracerConfig config = new TracerConfig(
            FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".properties"));
    private enum Page { HOME, CUSTOM, JOURNAL, REMAINING }
    private Page requestedPage;

    @Override
    public void onInitializeClient() {
        try {
            config.load();
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("Could not read Vex tracer settings; using OFF", exception);
        }
        // Full-clear mode swaps in Hunting Helper's coverage and remaining panels.
        SafariPanels.fullClearProgress = FullClearProgress::progress;
        SafariPanels.fullClearMissing = FullClearProgress::missing;
        // GAME and GAME_CANCELED are mutually exclusive. Visible and display-filtered lines already
        // reach SafariTracking through GAME. Forward only valid captures from the canceled path.
        ClientReceiveMessageEvents.GAME_CANCELED.register((message, overlay) -> {
            if (overlay) return;
            Minecraft.getInstance().execute(() -> {
                for (String line : SafariMessages.INSTANCE.lines(message.getString(), null, null)) {
                    if (SafariCatch.Companion.parse(line) != null)
                        SafariTracking.INSTANCE.receive(line);
                }
            });
        });
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "tracers"));
        KeyMapping toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.foragingpuckgrinding.toggle_vex_tracers", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, category));
        KeyMapping settings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.foragingpuckgrinding.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
        // Register the tint pipeline at startup instead of mid-frame when the first glow appears.
        java.util.Objects.requireNonNull(Highlight.FILL);
        VexTracerRenderer renderer = new VexTracerRenderer(config);
        TrackedOutlines.install(renderer);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggle.consumeClick()) setEnabled(client, !config.enabled());
            while (settings.consumeClick()) requestMenu(Page.HOME);
            Page page = requestedPage;
            requestedPage = null;
            if (page == Page.REMAINING) {
                client.gui.setScreen(new FullClearScreen(null));
            } else if (page != null) {
                var screen = new TracerSettingsScreen(config, toggle,
                        enabled -> setEnabled(client, enabled), targets -> setTargets(client, targets),
                        (target, glow, color) -> setColor(client, target, glow, color));
                var home = new HuntingHomeScreen(screen, config);
                screen.home = home;
                if (page == Page.CUSTOM) home.openCustom();
                else if (page == Page.JOURNAL) client.gui.setScreen(new FieldJournalScreen(home));
                else client.gui.setScreen(home);
            }
            renderer.tick(client);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                literal("vextracers")
                        .executes(context -> setEnabled(Minecraft.getInstance(), !config.enabled()))
                        .then(literal("on").executes(context -> setEnabled(Minecraft.getInstance(), true)))
                        .then(literal("off").executes(context -> setEnabled(Minecraft.getInstance(), false)))
                        .then(literal("menu").executes(context -> requestMenu(Page.HOME))));
            for (String name : new String[] {"huntinghelper", "hh"}) {
                dispatcher.register(literal(name).executes(context -> requestMenu(Page.HOME))
                        .then(literal("remaining").executes(context -> requestMenu(Page.REMAINING)))
                        .then(literal("journal").executes(context -> requestMenu(Page.JOURNAL)))
                        .then(literal("custom").executes(context -> requestMenu(Page.CUSTOM)))
                        .then(literal("debug").executes(context -> toggleDebug())));
            }
        });
        LevelExtractionEvents.END_EXTRACTION.register(renderer::extract);
        LevelRenderEvents.COLLECT_SUBMITS.register(renderer::submit);
    }

    private int toggleDebug() {
        boolean on = HuntingDebug.toggle();
        var player = Minecraft.getInstance().player;
        if (player != null) player.sendSystemMessage(Component.literal("Hunting Helper debug: " + (on
                ? "ON - writes a summary to latest.log every 5 seconds (search for [Hunting Helper])" : "OFF")));
        return 1;
    }

    private int requestMenu(Page page) {
        requestedPage = page;
        return 1;
    }

    private void setTargets(Minecraft client, java.util.Set<TracerTarget> targets) {
        try {
            config.setTargets(targets);
        } catch (IOException exception) {
            LOGGER.warn("Hunting Helper target changed, but could not be saved", exception);
            if (client.player != null) client.player.sendSystemMessage(
                    Component.literal("Hunting Helper setting could not be saved; see the game log."));
        }
    }

    private void setColor(Minecraft client, TracerTarget target, boolean glow, OverlayColor color) {
        try {
            config.setColor(target, glow, color);
        } catch (IOException exception) {
            LOGGER.warn("Hunting Helper color changed, but could not be saved", exception);
            if (client.player != null) client.player.sendSystemMessage(
                    Component.literal("Hunting Helper color could not be saved; see the game log."));
        }
    }

    private int setEnabled(Minecraft client, boolean enabled) {
        try {
            config.setEnabled(enabled);
        } catch (IOException exception) {
            LOGGER.warn("Vex tracers changed, but the setting could not be saved", exception);
            if (client.player != null) client.player.sendSystemMessage(
                    Component.literal("Vex tracer setting could not be saved; see the game log."));
        }
        if (client.player != null) client.player.sendOverlayMessage(Component.translatable(
                "message.foragingpuckgrinding." + (enabled ? "enabled" : "disabled")));
        return 1;
    }
}

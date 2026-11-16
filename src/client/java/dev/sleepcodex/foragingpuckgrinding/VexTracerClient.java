package dev.sleepcodex.foragingpuckgrinding;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
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
    private boolean menuRequested;

    @Override
    public void onInitializeClient() {
        try {
            config.load();
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("Could not read Vex tracer settings; using OFF", exception);
        }
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "tracers"));
        KeyMapping toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.foragingpuckgrinding.toggle_vex_tracers", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, category));
        VexTracerRenderer renderer = new VexTracerRenderer(config);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggle.consumeClick()) setEnabled(client, !config.enabled());
            if (menuRequested) {
                menuRequested = false;
                client.gui.setScreen(new TracerSettingsScreen(config, toggle,
                        enabled -> setEnabled(client, enabled), target -> setTarget(client, target)));
            }
            renderer.tick(client);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                literal("vextracers")
                        .executes(context -> setEnabled(Minecraft.getInstance(), !config.enabled()))
                        .then(literal("on").executes(context -> setEnabled(Minecraft.getInstance(), true)))
                        .then(literal("off").executes(context -> setEnabled(Minecraft.getInstance(), false)))
                        .then(literal("menu").executes(context -> requestMenu())));
            dispatcher.register(literal("huntinghelper").executes(context -> requestMenu()));
        });
        LevelExtractionEvents.END_EXTRACTION.register(renderer::extract);
        LevelRenderEvents.COLLECT_SUBMITS.register(renderer::submit);
    }

    private int requestMenu() {
        menuRequested = true;
        return 1;
    }

    private void setTarget(Minecraft client, TracerTarget target) {
        try {
            config.setTarget(target);
        } catch (IOException exception) {
            LOGGER.warn("Hunting Helper target changed, but could not be saved", exception);
            if (client.player != null) client.player.sendSystemMessage(
                    Component.literal("Hunting Helper setting could not be saved; see the game log."));
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

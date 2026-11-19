package dev.sleepcodex.foragingpuckgrinding.journal;

import dev.sleepcodex.foragingpuckgrinding.journal.client.BallDiagnostic;
import dev.sleepcodex.foragingpuckgrinding.journal.client.BazaarPrices;
import dev.sleepcodex.foragingpuckgrinding.journal.client.ChatQueue;
import dev.sleepcodex.foragingpuckgrinding.journal.client.CritterCommand;
import dev.sleepcodex.foragingpuckgrinding.journal.client.CritterHud;
import dev.sleepcodex.foragingpuckgrinding.journal.client.CritterEntities;
import dev.sleepcodex.foragingpuckgrinding.journal.client.CritterSpotter;
import dev.sleepcodex.foragingpuckgrinding.journal.client.DarknessFilter;
import dev.sleepcodex.foragingpuckgrinding.journal.client.HideonwallSpots;
import dev.sleepcodex.foragingpuckgrinding.journal.client.HotspotWatch;
import dev.sleepcodex.foragingpuckgrinding.journal.client.BirdfeederWatch;
import dev.sleepcodex.foragingpuckgrinding.journal.client.MissingHud;
import dev.sleepcodex.foragingpuckgrinding.journal.client.MoundTracker;
import dev.sleepcodex.foragingpuckgrinding.journal.client.RecatchSpots;
import dev.sleepcodex.foragingpuckgrinding.journal.client.SafariLocation;
import dev.sleepcodex.foragingpuckgrinding.journal.client.FullScreenAlert;
import dev.sleepcodex.foragingpuckgrinding.journal.client.SparklingWatch;
import dev.sleepcodex.foragingpuckgrinding.journal.client.TradeHud;
import dev.sleepcodex.foragingpuckgrinding.journal.client.TraderWatch;
import dev.sleepcodex.foragingpuckgrinding.journal.client.WallTracker;
import dev.sleepcodex.foragingpuckgrinding.journal.client.WaypointRenderer;
import dev.sleepcodex.foragingpuckgrinding.journal.client.EncounterAlerts;
import dev.sleepcodex.foragingpuckgrinding.journal.parse.ChatParser;
import dev.sleepcodex.foragingpuckgrinding.journal.session.RunHistory;
import dev.sleepcodex.foragingpuckgrinding.journal.session.SessionManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Registers the journal's observations, reports and local display controls. */
public class CritterMod implements ClientModInitializer {

	public static final String MOD_ID = "foragingpuckgrinding";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static java.util.function.Supplier<net.minecraft.client.gui.screens.Screen> nextScreen;

	public static void openNextTick(java.util.function.Supplier<net.minecraft.client.gui.screens.Screen> factory) {
		nextScreen = factory;
	}

	@Override
	public void onInitializeClient() {
		// Exactly one delivery path fires, including when Safari chat filters hide the line.
		ClientReceiveMessageEvents.GAME.register(CritterMod::receive);
		ClientReceiveMessageEvents.GAME_CANCELED.register(CritterMod::receive);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (nextScreen != null) {
				var factory = nextScreen;
				nextScreen = null;
				client.gui.setScreen(factory.get());
			}
			// First, and only here: everything below asks it where the player is.
			SafariLocation.tick();
			// One sweep of the world's critters, for everything below that wants them.
			CritterEntities.tick();
			SessionManager.tick();
			CritterSpotter.tick();
			// Every tick, not on an interval: a capsule crosses a perch inside one.
			HideonwallSpots.tick();
			BallDiagnostic.tick();
			WallTracker.tick();
			BirdfeederWatch.tick();
			SparklingWatch.tick();
			RecatchSpots.tick();
			DarknessFilter.tick();
			TraderWatch.tick();
			// Off-thread, at most every two minutes, and only where a price is shown.
			BazaarPrices.tick();
			ChatQueue.tick();
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SessionManager.finish());

		// Hypixel never says you have left the Safari, but moving island reconnects, so
		// this is the one moment the chat-driven flag is known to be stale.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			ChatQueue.clear();
			RecatchSpots.reset();
			TraderWatch.reset();
			HideonwallSpots.reset();
			SafariLocation.onWorldChange();
			SessionManager.onWorldChange();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ChatQueue.clear();
			RecatchSpots.reset();
			TraderWatch.reset();
			HideonwallSpots.reset();
			SafariLocation.onWorldChange();
			SessionManager.onWorldChange();
		});

		ClientCommandRegistrationCallback.EVENT.register(
			(dispatcher, registryAccess) -> CritterCommand.register(dispatcher));

		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(MOD_ID, "safari_progress"),
			new CritterHud());
		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(MOD_ID, "safari_missing"),
			new MissingHud());
		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(MOD_ID, "hunter_trades"),
			new TradeHud());
		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(MOD_ID, "encounter_alerts"),
			new EncounterAlerts());
		HudElementRegistry.attachElementBefore(
			VanillaHudElements.CHAT,
			Identifier.fromNamespaceAndPath(MOD_ID, "full_screen_alert"),
			new FullScreenAlert());

		WaypointRenderer.register();

		// Past runs live next to the settings, as plain JSON, so they survive updates
		// and can be read or thrown away by hand.
		var configDir = FabricLoader.getInstance().getConfigDir();
		RunHistory.load(configDir.resolve("hunting-helper-field-runs.json"), configDir.resolve("crittermod-runs.json"));

		LOGGER.info("Safari field journal ready");
	}

	private static void receive(net.minecraft.network.chat.Component message, boolean overlay) {
		if (overlay) return;
		// Hypixel sends banners such as the "entered Critter Safari!" notice as a
		// single multi-line component, so each line has to be handled separately
		// or the interesting one never matches on its own.
		for (String part : message.getString().split("\\r?\\n|\\\\n")) {
			String line = ChatParser.clean(part);
			if (line.isEmpty()) continue;

			// Anything a player can type, a player can quote. Only the shared-trade
			// reader wants what people say — it is looking for this mod's own
			// announcement — so everything else sees the game's messages alone.
			if (ChatParser.playerSaid(line)) {
				if (SafariLocation.inside()) TraderWatch.onChatMessage(line);
				continue;
			}

			SafariLocation.onChatMessage(line);
			SessionManager.onChatMessage(line);
			if (!SafariLocation.inside() && SessionManager.current() == null) continue;
			EncounterAlerts.onChatMessage(line);
			TraderWatch.onChatMessage(line);
			MoundTracker.onChatMessage(line);
			RecatchSpots.onChatMessage(line);
			BirdfeederWatch.onChatMessage(line);
			HotspotWatch.onChatMessage(line);
			HideonwallSpots.onChatMessage(line);
		}
	}
}

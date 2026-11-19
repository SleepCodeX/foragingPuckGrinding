package dev.sleepcodex.foragingpuckgrinding.journal.client;

import dev.sleepcodex.foragingpuckgrinding.journal.data.Critter;
import dev.sleepcodex.foragingpuckgrinding.journal.data.Critters;
import dev.sleepcodex.foragingpuckgrinding.journal.data.SafariBiome;
import dev.sleepcodex.foragingpuckgrinding.journal.session.SafariSession;
import dev.sleepcodex.foragingpuckgrinding.journal.session.SessionManager;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Map;

/**
 * Top-left overview panel: party and personal dex progress for this run, a bar per
 * biome, and who is covering which biome.
 */
public final class CritterHud implements HudElement {

	private static final int HEADER = 0xFFFFAA00;
	private static final int LABEL = 0xFFBBBBBB;
	private static final int DIM = 0xFF888888;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int DONE = 0xFF55FF55;
	/** Gold, as coins are everywhere else in SkyBlock. */
	private static final int COINS = 0xFFFFD700;

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		CritterConfig config = ConfigManager.get();
		if (!config.display.hudEnabled || !config.display.showCoverage) return;

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gui.hud.isHidden()) return;
		if (!config.display.showsHere()) return;

		HudPanel panel = buildPanel();
		if (panel == null) return;

		HudBox box = HudBox.PROGRESS;
		panel.render(graphics, client.font,
			box.pixelX(graphics.guiWidth(), panel, client.font),
			box.pixelY(graphics.guiHeight(), panel, client.font),
			box.scale());
	}

	/** Builds the panel for the current run, or {@code null} when there is nothing to show. */
	static HudPanel buildPanel() {
		CritterConfig config = ConfigManager.get();

		SafariSession session = SessionManager.currentOrLast();
		if (session == null || session.isEmpty()) return null;
		HudPanel panel = new HudPanel();
		panel.title("Party field notes", HEADER);
		// Under the biome bars: what the run has actually earned so far. Left off until
		// there is a shard and a price to put on it — "0 coins" says nothing true.
		if (config.profit.enabled && config.profit.showOnHud
			&& session.totalShards() > 0 && BazaarPrices.known()) {
			panel.pair("Value", BazaarPrices.coinsText(session), LABEL, COINS);
		}

		if (config.display.showPerPlayer) {
			Map<String, Map<SafariBiome, Integer>> perPlayer = session.uniquePerPlayer();
			if (!perPlayer.isEmpty()) {
				panel.blank();
				perPlayer.forEach((player, counts) ->
					panel.pair(player, describe(counts), 0xFF55FFFF, DIM));
			}
		}

		return panel;
	}

	/** A player's coverage as {@code "Icy 9, Haunted 2"}, busiest biome first. */
	private static String describe(Map<SafariBiome, Integer> counts) {
		return counts.entrySet().stream()
			.sorted(Map.Entry.<SafariBiome, Integer>comparingByValue().reversed())
			.map(e -> e.getKey().displayName() + " " + e.getValue())
			.reduce((a, b) -> a + ", " + b)
			.orElse("-");
	}

	static String formatDuration(long millis) {
		long seconds = millis / 1000;
		return "%d:%02d".formatted(seconds / 60, seconds % 60);
	}

	static int rarityColour(Critter critter) {
		return 0xFF000000 | critter.rarity().colour();
	}
}

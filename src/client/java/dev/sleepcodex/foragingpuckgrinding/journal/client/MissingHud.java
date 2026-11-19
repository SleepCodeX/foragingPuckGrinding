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

import java.util.List;

/**
 * Top-right panel listing what is still uncaught <em>in the biome you are standing
 * in</em> — the working list for whoever is assigned that biome.
 *
 * <p>Species already caught by a partymate count as done, since the run's goal is
 * party-wide coverage. A caught-by-you marker distinguishes the two.
 */
public final class MissingHud implements HudElement {

	private static final int DONE = 0xFF73BB8C;
	private static final int DIM = 0xFF888888;
	private static final int WALL = 0xFFFFAA00;

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		CritterConfig config = ConfigManager.get();
		if (!config.display.hudEnabled || !config.display.showMissing) return;
		if (!config.display.showsHere()) return;

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.gui.hud.isHidden()) return;

		SafariBiome biome = SafariLocation.biome();
		if (biome == null) return;

		HudPanel panel = buildPanel(biome, SessionManager.currentOrLast());
		HudBox box = HudBox.MISSING;
		panel.render(graphics, client.font,
			box.pixelX(graphics.guiWidth(), panel, client.font),
			box.pixelY(graphics.guiHeight(), panel, client.font),
			box.scale());
	}

	/** Builds the list for {@code biome}; {@code session} may be null before the first catch. */
	static HudPanel buildPanel(SafariBiome biome, SafariSession session) {
		HudPanel panel = new HudPanel();
		panel.title(biome.displayName() + " field notes", 0xFF000000 | biome.colour());
		if (session != null) {
			for (Critter critter : Critters.inBiome(biome)) {
				int nearby = session.nearby(critter);
				int attempts = session.attempts(critter);
				int failures = session.failures(critter);
				List<String> notes = new java.util.ArrayList<>();
				if (nearby > 0 && ConfigManager.get().display.countSpawns) notes.add(nearby + " near");
				if (attempts > 0) notes.add(attempts + " tried");
				if (failures > 0) notes.add(failures + " missed");
				if (session.isUnavailable(critter)) notes.add("none this run");
				if (!notes.isEmpty()) panel.pair(critter.name(), String.join(" · ", notes), CritterHud.rarityColour(critter), DIM);
			}
		}

		appendWalls(panel, biome, WallTracker.TROODON, ConfigManager.get().display.showTroodonWalls);
		appendMounds(panel, biome);
		return panel;
	}

	/**
	 * Counts the mounds still standing near you, and optionally how many have been
	 * broken this run.
	 *
	 * <p>The standing count is of mounds <em>you have looked at</em> and not yet seen
	 * broken, not of mounds in the Cavern — so it is left out entirely when the count is
	 * zero rather than shown as a zero that would read as "none left".
	 */
	private static void appendMounds(HudPanel panel, SafariBiome biome) {
		if (biome != SafariBiome.CAVERN) return;

		// Only about a fifth of mounds hold a Rockmite, so the number of rolls taken is
		// worth having on its own.
		if (!ConfigManager.get().advanced.showMounds) return;
		if (MoundTracker.broken() == 0) return;

		panel.blank();
		panel.pair("Mounds broken", "%d → %d Rockmite".formatted(
			MoundTracker.broken(), MoundTracker.rockmites()), WALL, DIM);
	}

	/**
	 * Counts one set of walls still to break, while you are in the biome they are in.
	 * They want doing every run to check behind them, and being blocks rather than
	 * entities they can be counted exactly.
	 *
	 * <p>A wall in an unloaded chunk is added as {@code +n?} rather than counted:
	 * air and out-of-range look identical from here.
	 */
	private static void appendWalls(HudPanel panel, SafariBiome biome, WallTracker tracker, boolean show) {
		if (biome != tracker.biome() || !show) return;

		List<WallTracker.Wall> walls = tracker.walls();
		if (walls.isEmpty()) return;

		long intact = walls.stream().filter(w -> w.state() == WallTracker.State.INTACT).count();
		long unknown = walls.stream().filter(w -> w.state() == WallTracker.State.UNKNOWN).count();
		if (intact == 0 && unknown == 0) {
			panel.blank();
			panel.line(tracker.name() + " walls all broken", DONE);
			return;
		}

		panel.blank();
		panel.pair(tracker.name() + " walls to break",
			intact + (unknown > 0 ? " (+" + unknown + "?)" : ""), WALL, DIM);
	}
}

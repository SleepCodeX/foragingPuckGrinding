package dev.sleepcodex.foragingpuckgrinding.journal.client;

import dev.sleepcodex.foragingpuckgrinding.journal.data.SafariBiome;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** The positions worth highlighting, each behind its own setting. */
public final class Markers {

	/** How a marker is drawn. */
	public enum Style {
		/** Through the terrain, with its name and distance floating above it. */
		WAYPOINT,
		/** Depth-tested and unnamed: a box on the thing, seen only when the thing is. */
		HIGHLIGHT
	}

	/**
	 * One thing worth walking to.
	 *
	 * <p>A box rather than a position: most of these are blocks and so are exactly a
	 * block big, but a pinned critter is whatever size that critter is, and drawing a
	 * full block around a Rockmite would be pointing at the wrong thing.
	 */
	public record Marker(AABB box, String label, int colour, Style style) {
	}

	private Markers() {
	}

	public static List<Marker> collect() {
		CritterConfig.DisplayConfig display = ConfigManager.get().display;
		SafariBiome biome = SafariLocation.biome();
		List<Marker> markers = new ArrayList<>();

		// Everything but the trades belongs to one biome, so it is only marked while you
		// are in that biome. Boxes floating through the terrain of a biome they have
		// nothing to do with are just noise.
		if (display.highlightTroodonWalls) {
			addWalls(markers, WallTracker.TROODON, biome,
				Colours.argb(display.troodonWallColour, 0xFF55AAFF));
		}

		if (display.highlightTrades) {
			for (TraderWatch.Trade trade : TraderWatch.found()) {
				TraderWatch.Spot spot = trade.spot();
				if (spot == null) continue;
				markers.add(block(new BlockPos(spot.x(), spot.y(), spot.z()),
					trade.critter().name(), Colours.argb(display.tradeColour, 0xFF55FFFF)));
			}
		}

		// Fixed positions, but the colour is read from the world: an empty perch and an
		// occupied one look the same from a distance, so the mark means "check here" until
		// a Hideonwall is actually seen on it, and then it says so.
		if (display.highlightHideonwalls && biome == SafariBiome.HAUNTED) {
			for (HideonwallSpots.Perch perch : HideonwallSpots.perches()) {
				boolean here = perch.state() == HideonwallSpots.State.OCCUPIED;
				markers.add(block(perch.pos(), here ? "Hideonwall" : "Hideonwall perch",
					here ? Colours.argb(display.hideonwallFoundColour, 0xFF55FF55)
						: Colours.argb(display.hideonwallColour, 0xFFAA55FF)));
			}
		}

		// Not gated on the biome: it is pinned by a capsule you threw, so it is wherever
		// you were standing when you threw it.
		if (display.recatchHelper && RecatchSpots.pinned() != null) {
			markers.add(new Marker(RecatchSpots.pinned(), RecatchSpots.pinnedCritter().name(),
				Colours.argb(display.recatchColour, 0xFFFFFF55), Style.WAYPOINT));
		}

		return markers;
	}

	/** A marker filling one block, which is what everything read off the map wants. */
	private static Marker block(BlockPos pos, String label, int colour) {
		return new Marker(new AABB(pos), label, colour, Style.WAYPOINT);
	}

	/**
	 * Marks the walls of one set that have not been seen to be gone.
	 *
	 * <p>Everything but {@code BROKEN}, rather than only {@code INTACT}: the positions are
	 * preset, so a mark on one costs nothing even before its chunk has loaded, and the
	 * only thing that should take a mark away is having looked at the wall and found it
	 * broken. Marking only confirmed-standing walls used to hide the ones a partymate had
	 * broken out of sight, which is the same as being told about it.
	 */
	private static void addWalls(List<Marker> markers, WallTracker walls, SafariBiome biome, int colour) {
		if (biome != walls.biome()) return;
		for (WallTracker.Wall wall : walls.walls()) {
			if (wall.state() == WallTracker.State.BROKEN) continue;
			markers.add(block(wall.pos(), walls.name() + " wall", colour));
		}
	}
}

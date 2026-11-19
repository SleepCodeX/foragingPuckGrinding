// Hunting Helper: ported from CritterMod (MIT) via the Hunting Helper field journal; see docs/SM-UPSTREAM.md.
package net.johnceo.sparklingmutuals.hunting;

import net.johnceo.sparklingmutuals.config.SettingsColor;
import net.johnceo.sparklingmutuals.safari.SafariBiome;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * The positions worth marking in the world, each behind its own setting: Hunter trades, Hideonwall perches and
 * the recatch pin. Sparkling Mutuals draws its own nest, wall, mound and floor-drop marks, so those are not here.
 */
public final class Markers {

	/** How a marker is drawn. */
	public enum Style {
		/** Through the terrain, with its name and distance floating above it. */
		WAYPOINT,
		/** Depth-tested and unnamed: a box on the thing, seen only when the thing is. */
		HIGHLIGHT
	}

	/**
	 * One thing worth walking to. A box rather than a position: a pinned critter is whatever size that critter is.
	 */
	public record Marker(AABB box, String label, int colour, Style style) {
	}

	private Markers() {
	}

	private static int colour(String value) {
		return SettingsColor.INSTANCE.argb(value, System.currentTimeMillis());
	}

	public static List<Marker> collect() {
		HuntingConfig config = HuntingConfig.INSTANCE;
		SafariBiome biome = Where.biome();
		List<Marker> markers = new ArrayList<>();

		if (HuntingOutput.tradeMarks()) {
			for (TraderWatch.Trade trade : TraderWatch.found()) {
				TraderWatch.Spot spot = trade.spot();
				if (spot == null) continue;
				markers.add(block(new BlockPos(spot.x(), spot.y(), spot.z()),
					trade.critter().getName(), colour(config.getTradeColor())));
			}
		}

		// Fixed positions, but the colour is read from the world: an empty perch and an occupied one look the same
		// from a distance, so the mark means "check here" until a Hideonwall is actually seen on it.
		if (HuntingOutput.hideonwallMarks() && biome == SafariBiome.HAUNTED) {
			for (HideonwallSpots.Perch perch : HideonwallSpots.perches()) {
				boolean here = perch.state() == HideonwallSpots.State.OCCUPIED;
				markers.add(block(perch.pos(), here ? "Hideonwall" : "Hideonwall perch",
					colour(here ? config.getHideonwallFoundColor() : config.getHideonwallColor())));
			}
		}

		if (HuntingOutput.troodonWallMarks() && biome == SafariBiome.ICY) {
			for (TroodonWalls.Wall wall : TroodonWalls.walls()) {
				if (wall.state() != TroodonWalls.State.BROKEN)
					markers.add(block(wall.pos(), "Troodon wall", colour(config.getTroodonWallColor())));
			}
		}

		// Not gated on the biome: it is pinned by a capsule you threw, wherever you were standing.
		if (HuntingOutput.recatchMarks() && RecatchSpots.pinned() != null) {
			markers.add(new Marker(RecatchSpots.pinned(), RecatchSpots.pinnedCritter().getName(),
				colour(config.getRecatchColor()), Style.WAYPOINT));
		}
		return markers;
	}

	/** A marker filling one block, which is what everything read off the map wants. */
	private static Marker block(BlockPos pos, String label, int colour) {
		return new Marker(new AABB(pos), label, colour, Style.WAYPOINT);
	}
}

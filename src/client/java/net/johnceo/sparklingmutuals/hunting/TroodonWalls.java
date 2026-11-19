// Hunting Helper: ported from CritterMod (MIT) via the Hunting Helper field journal; see docs/SM-UPSTREAM.md.
package net.johnceo.sparklingmutuals.hunting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * The three breakable Icy walls that hide Troodons. Sparkling Mutuals tracks the Cavern Snooper walls itself; these
 * are the ones it does not. A wall counts as broken only once it has been seen gone: air in an unloaded or unseen
 * spot is "unknown", not broken.
 */
public final class TroodonWalls {
	private static final BlockPos[] WALLS = {
		new BlockPos(-104, 80, -95), new BlockPos(-131, 78, -61), new BlockPos(-109, 89, -27),
	};
	private static final int CONFIRM_INTERVAL_TICKS = 10;

	public enum State { INTACT, BROKEN, UNKNOWN }
	public record Wall(BlockPos pos, State state) { }

	private static final Set<BlockPos> confirmedBroken = new HashSet<>();
	private static int ticks;

	private TroodonWalls() { }

	public static void tick() {
		if (++ticks < CONFIRM_INTERVAL_TICKS) return;
		ticks = 0;
		Minecraft client = Minecraft.getInstance();
		if (!Where.inSafari() || client.level == null || client.player == null) return;
		for (BlockPos pos : WALLS) {
			if (confirmedBroken.contains(pos) || !client.level.isLoaded(pos)) continue;
			if (client.level.getBlockState(pos).isAir() && Visibility.canSee(pos)) confirmedBroken.add(pos);
		}
	}

	public static List<Wall> walls() {
		Minecraft client = Minecraft.getInstance();
		List<Wall> result = new ArrayList<>();
		if (client.level == null) return result;
		for (BlockPos pos : WALLS) {
			State state = confirmedBroken.contains(pos) ? State.BROKEN
				: client.level.isLoaded(pos) && !client.level.getBlockState(pos).isAir() ? State.INTACT : State.UNKNOWN;
			result.add(new Wall(pos, state));
		}
		return result;
	}

	public static void reset() { confirmedBroken.clear(); }
}

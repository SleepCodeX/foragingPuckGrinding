package dev.sleepcodex.foragingpuckgrinding;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in diagnostics ({@code /hh debug}): every 5 seconds one summary block in latest.log, prefixed
 * "[Hunting Helper]". Shows location detection, what was found per target (by nametag or by mob type),
 * nametag pairings, and how many glows actually reached the renderer (entity culling mods can skip them).
 */
public final class HuntingDebug {
    public static final Logger LOG = LoggerFactory.getLogger("HuntingHelper");
    /** Render states given a glow / tint fills submitted since the last summary (render thread). */
    public static final AtomicInteger STATES = new AtomicInteger();
    public static final AtomicInteger FILLS = new AtomicInteger();
    private static final int PERIOD_TICKS = 100, MAX_LINES = 8;
    private static volatile boolean enabled;
    private static int ticks;
    private static boolean modEnabled, inTorrhus;
    private static String matched = "";
    private static final List<String> lines = new ArrayList<>();

    private HuntingDebug() { }

    static boolean toggle() {
        enabled = !enabled;
        ticks = 0;
        STATES.set(0);
        FILLS.set(0);
        LOG.info("[Hunting Helper] Debug {}", enabled ? "on" : "off");
        return enabled;
    }

    static boolean enabled() { return enabled; }

    public static void recordState() { if (enabled) STATES.incrementAndGet(); }
    public static void recordFill() { if (enabled) FILLS.incrementAndGet(); }

    /** True on the one tick per period that writes a summary; detail logging only happens then. */
    static boolean due() { return enabled && ticks % PERIOD_TICKS == 0; }

    static void beginTick(boolean mod, boolean torrhus, String label) {
        if (!enabled) return;
        ticks++;
        modEnabled = mod;
        inTorrhus = torrhus;
        matched = label;
        lines.clear();
    }

    /** A target nametag and the mob it was paired with, or the closest mobs when none qualified. */
    static void nametag(Entity tag, TracerTarget target, Entity mob, List<Entity> mobs) {
        if (lines.size() >= MAX_LINES) return;
        StringBuilder line = new StringBuilder(String.format("  Nametag '%s' (%s) at %s -> ", name(tag), target.label(), pos(tag)));
        if (mob != null) {
            line.append(type(mob)).append(" at ").append(pos(mob));
        } else {
            line.append("NO mob below (the tracer points at the nametag). Closest mobs:");
            mobs.stream().filter(m -> m.distanceToSqr(tag) < 36)
                    .sorted((a, b) -> Double.compare(a.distanceToSqr(tag), b.distanceToSqr(tag))).limit(3)
                    .forEach(m -> line.append(' ').append(type(m)).append('@').append(pos(m))
                            .append(String.format(" (dy=%.1f)", tag.getY() - m.getY())));
        }
        lines.add(line.toString());
    }

    /** Named stands up to 6 blocks above a type-detected mob, to learn Hypixel's tag names for them. */
    static void nametagsNear(Entity mob, List<Entity> tags) {
        for (Entity tag : tags) {
            if (lines.size() >= MAX_LINES) return;
            double dx = tag.getX() - mob.getX(), dz = tag.getZ() - mob.getZ(), dy = tag.getY() - mob.getY();
            if (dy < 0 || dy > 6 || dx * dx + dz * dz > 4) continue;
            lines.add(String.format("  Nametag near %s: '%s'", type(mob), name(tag)));
        }
    }

    static void endTick(Map<Entity, TracerTarget> resolved, Set<Entity> byNametag, TracerConfig config) {
        if (!due()) return;
        Map<TracerTarget, int[]> counts = new EnumMap<>(TracerTarget.class);
        resolved.forEach((entity, target) -> {
            int[] c = counts.computeIfAbsent(target, k -> new int[2]);
            c[0]++;
            if (byNametag.contains(entity)) c[1]++;
        });
        StringBuilder out = new StringBuilder("[Hunting Helper] Debug: mod ").append(modEnabled ? "ON" : "OFF")
                .append(" | Torrhus: ").append(inTorrhus ? "yes (" + matched + ")" : "no")
                .append(" | found: ");
        if (counts.isEmpty()) out.append("nothing ");
        counts.forEach((t, c) -> out.append(t.label()).append('=').append(c[0])
                .append(" (nametag ").append(c[1]).append(", type ").append(c[0] - c[1])
                .append(", glow ").append(config.glowColor(t) == 0 ? "off" : "on").append(") "));
        out.append("| rendered in the last 5s: ").append(STATES.getAndSet(0)).append(" glowing states, ")
                .append(FILLS.getAndSet(0)).append(" tint fills");
        LOG.info(out.toString());
        for (String line : lines) LOG.info(line);
    }

    private static String name(Entity tag) {
        return tag.getCustomName() == null ? "?" : tag.getCustomName().getString();
    }

    private static String type(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
    }

    private static String pos(Entity entity) {
        return String.format("%.0f/%.0f/%.0f", entity.getX(), entity.getY(), entity.getZ());
    }
}

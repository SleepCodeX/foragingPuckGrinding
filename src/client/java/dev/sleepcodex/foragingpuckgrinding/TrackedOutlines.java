package dev.sleepcodex.foragingpuckgrinding;

import net.minecraft.world.entity.Entity;

/** Read-only bridge into the native outline pass; no entity flags or scoreboard teams are changed. */
public final class TrackedOutlines {
    private static VexTracerRenderer renderer;
    private TrackedOutlines() { }
    static void install(VexTracerRenderer value) { renderer = value; }
    /** The glow color of a mob resolved this tick, or 0 when it should not glow (including glow OFF). */
    public static TracerTarget targetFor(Entity entity) { return renderer == null ? null : renderer.trackedTarget(entity); }
    public static int colorFor(Entity entity) { return renderer == null ? 0 : renderer.glowColor(entity); }
}

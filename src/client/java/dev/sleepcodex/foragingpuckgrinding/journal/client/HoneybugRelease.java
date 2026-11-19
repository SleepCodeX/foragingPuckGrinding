package dev.sleepcodex.foragingpuckgrinding.journal.client;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.johnceo.sparklingmutuals.safari.structures.BeeNests;
import net.johnceo.sparklingmutuals.safari.structures.BeeNestSurvey;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** A nearby punch can miss the nest block. Only a new Honeybug at that site confirms it. */
public final class HoneybugRelease {
    private static final Set<UUID> present = new HashSet<>();
    private static BlockPos armed;
    private static long deadline;

    private HoneybugRelease() {}

    public static void attack(BlockPos hit) {
        var level = Minecraft.getInstance().level;
        if (!SafariLocation.inside() || level == null || level.getBlockState(hit).is(Blocks.BEE_NEST)) return;
        BlockPos nearest = null;
        double distance = 2.5 * 2.5;
        for (var site : BeeNestSurvey.Companion.getPositions()) {
            var pos = new BlockPos(site.getFirst(), site.getSecond(), site.getThird());
            double squared = pos.distSqr(hit);
            if (squared <= distance && level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.BEE_NEST)) {
                nearest = pos;
                distance = squared;
            }
        }
        if (nearest == null) return;
        armed = nearest;
        deadline = System.nanoTime() + 3_000_000_000L;
        present.clear();
        for (var sight : CritterEntities.all())
            if (sight.critter().name().equals("Honeybug")) present.add(sight.label().getUUID());
    }

    public static void tick() {
        if (armed == null) return;
        var level = Minecraft.getInstance().level;
        if (level == null || !SafariLocation.inside() || System.nanoTime() >= deadline) { reset(); return; }
        for (var sight : CritterEntities.all()) {
            if (sight.critter().name().equals("Honeybug") && !present.contains(sight.label().getUUID())
                    && sight.body().position().distanceToSqr(Vec3.atCenterOf(armed)) <= 36) {
                BeeNests.INSTANCE.clicked(level, armed);
                reset();
                return;
            }
        }
    }

    public static void reset() { armed = null; present.clear(); }
}

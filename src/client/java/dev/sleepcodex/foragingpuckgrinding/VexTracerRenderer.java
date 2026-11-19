package dev.sleepcodex.foragingpuckgrinding;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

final class VexTracerRenderer {
    private static final int STRIDE = 12;
    private static final RenderStateDataKey<Frame> LINES = RenderStateDataKey.create(() -> "Hunting Helper lines and boxes");
    private static final Frame EMPTY = new Frame(0, 0, 0, new float[0], new int[0], 0);
    private static final RenderType RENDER_TYPE = RenderType.create("hunting_helper_tracers",
            RenderSetup.builder(RenderPipelines.register(RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(VexTracerClient.MOD_ID, "pipeline/tracers"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
                    .withPrimitiveTopology(PrimitiveTopology.LINES)
                    .withDepthStencilState(Optional.empty()).withCull(false).build())).createRenderSetup());
    private final TracerConfig config;
    private final TorrhusContext torrhus = new TorrhusContext();
    private final Vector3f look = new Vector3f();
    private ClientLevel cachedLevel;
    private Entity[] candidates = new Entity[0];
    private TracerTarget[] candidateTypes = new TracerTarget[0];
    // Resolved once per tick, so glow lookups also cover nametag-detected mobs; reused between ticks.
    private final IdentityHashMap<Entity, TracerTarget> resolved = new IdentityHashMap<>();
    private final Set<Entity> byNametag = Collections.newSetFromMap(new IdentityHashMap<>());
    private final ArrayList<Entity> tags = new ArrayList<>(), mobs = new ArrayList<>();
    private int candidateCount;
    private TracerRange.Bounds range;
    private int rangeChunkX, rangeChunkZ, rangeChunks;
    private boolean activeTorrhus;


    VexTracerRenderer(TracerConfig config) { this.config = config; }

    void tick(Minecraft client) {
        if (cachedLevel != client.level) {
            clearCandidates();
            cachedLevel = client.level;
            range = null;
        }
        boolean tracking = config.enabled() && config.hasTargets() && client.player != null && cachedLevel != null;
        boolean area = torrhus.update(client, tracking && torrhusTargetSelected());
        activeTorrhus = area;
        HuntingDebug.beginTick(config.enabled(), area, torrhus.matched());
        if (!tracking) {
            clearCandidates();
            HuntingDebug.endTick(resolved, byNametag, config);
            return;
        }
        updateRange(client);
        resolved.clear();
        byNametag.clear();
        // Inspect every loaded entity once per tick and sort it into nametag stands and mobs.
        // Reuse discovery storage and resolve each mob here, never in the render loop.
        for (Entity entity : cachedLevel.entitiesForRendering()) {
            if (!entity.isAlive() || entity.isRemoved()) continue;
            if (entity instanceof ArmorStand) {
                if (entity.hasCustomName()) tags.add(entity);
            } else if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                mobs.add(entity);
            }
        }
        boolean debug = HuntingDebug.due();
        // Hypixel names every SkyBlock mob with a custom-named armor stand about two blocks above it.
        for (Entity tag : tags) {
            TracerTarget target = TracerTarget.fromNametag(tag.getCustomName().getString());
            if (target == null || !tracked(target, area)) continue;
            Entity mob = mobBelow(tag, target);
            if (debug) HuntingDebug.nametag(tag, target, mob, mobs);
            // Without a mob below, name-only targets still get a tracer to the nametag itself. Targets with
            // type detection skip it: the type pass below finds the mob, and one creature never gets two tracers.
            if (mob == null && target.typeDetected()) continue;
            Entity entity = mob != null ? mob : tag;
            if (range.contains(entity.getX(), entity.getZ()) && resolved.putIfAbsent(entity, target) == null) byNametag.add(entity);
        }
        // The original three keep their type detection for mobs without a matching nametag.
        // Parched and Pollendart are nametag-only: plain bees and skeleton variants never match.
        for (Entity mob : mobs) {
            TracerTarget type = typeOf(mob);
            if (type == null) continue;
            if (debug) HuntingDebug.nametagsNear(mob, tags);
            if (tracked(type, area) && range.contains(mob.getX(), mob.getZ())) resolved.putIfAbsent(mob, type);
        }
        tags.clear();
        mobs.clear();
        int count = 0;
        for (var entry : resolved.entrySet()) {
            if (count == candidates.length) {
                int capacity = Math.max(16, count * 2);
                candidates = Arrays.copyOf(candidates, capacity);
                candidateTypes = Arrays.copyOf(candidateTypes, capacity);
            }
            candidates[count] = entry.getKey();
            candidateTypes[count++] = entry.getValue();
        }
        if (count < candidateCount) {
            Arrays.fill(candidates, count, candidateCount, null);
            Arrays.fill(candidateTypes, count, candidateCount, null);
        }
        candidateCount = count;
        HuntingDebug.endTick(resolved, byNametag, config);
    }

    private boolean torrhusTargetSelected() {
        for (TracerTarget target : TracerTarget.values()) if (target.torrhusOnly() && config.isSelected(target)) return true;
        return false;
    }

    private boolean tracked(TracerTarget target, boolean area) {
        return config.isSelected(target) && (!target.torrhusOnly() || area);
    }

    /** The mob a nametag floats over: the closest living mob just below it (only bees for Pollendart). */
    private Entity mobBelow(Entity tag, TracerTarget target) {
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity mob : mobs) {
            if (target == TracerTarget.POLLENDART && !(mob instanceof Bee)) continue;
            double dx = mob.getX() - tag.getX(), dz = mob.getZ() - tag.getZ(), dy = tag.getY() - mob.getY();
            if (dy < -0.5 || dy > mob.getBbHeight() + 2.5 || dx * dx + dz * dz > 2.25) continue;
            double distance = dx * dx + dz * dz + dy * dy;
            if (distance < bestDistance) { bestDistance = distance; best = mob; }
        }
        return best;
    }

    private void clearCandidates() {
        resolved.clear();
        byNametag.clear();
        Arrays.fill(candidates, 0, candidateCount, null);
        Arrays.fill(candidateTypes, 0, candidateCount, null);
        candidateCount = 0;
    }

    private void updateRange(Minecraft client) {
        int x = (int) Math.floor(client.player.getX() / 16);
        int z = (int) Math.floor(client.player.getZ() / 16);
        int chunks = client.options.getEffectiveRenderDistance();
        if (range == null || x != rangeChunkX || z != rangeChunkZ || chunks != rangeChunks) {
            range = TracerRange.aroundChunk(x, z, chunks);
            rangeChunkX = x;
            rangeChunkZ = z;
            rangeChunks = chunks;
        }
    }

    private static TracerTarget typeOf(Entity entity) {
        if (entity instanceof Vex) return TracerTarget.VEX;
        if (entity.getType() == EntityTypes.GHAST) return TracerTarget.HIVE_THIEF;
        if (entity instanceof Parrot bird && bird.getVariant() == Parrot.Variant.BLUE && !bird.isTame()) return TracerTarget.BLUE_JAY;
        return null;
    }

    /** The target this entity was resolved to this tick, or null. */
    TracerTarget trackedTarget(Entity entity) { return resolved.get(entity); }

    int glowColor(Entity entity) {
        // Called by several render mixins for every entity each frame; untracked mobs stop at this lookup.
        TracerTarget type = resolved.isEmpty() ? null : resolved.get(entity);
        // A nametag stand used as tracer fallback gets the line and box, never a glowing armor-stand silhouette.
        if (type == null || entity instanceof ArmorStand) return 0;
        Minecraft client = Minecraft.getInstance();
        if (!config.enabled() || client.player == null || client.level != cachedLevel || range == null
                || client.gui.hud.isHidden() || !entity.isAlive() || entity.isRemoved()
                || entity.level() != cachedLevel || !range.contains(entity.getX(), entity.getZ())) return 0;
        if (!config.isSelected(type) || (type.torrhusOnly() && !activeTorrhus)) return 0;
        return config.glowColor(type);
    }

    void extract(LevelExtractionContext context) {
        context.levelState().setData(LINES, EMPTY);
        Minecraft client = Minecraft.getInstance();
        if (!config.enabled() || !config.hasTargets() || candidateCount == 0 || context.level() != cachedLevel
                || client.player == null || client.gui.hud.isHidden()) return;
        var camera = context.camera().position();
        look.set(0, 0, -1).rotate(context.camera().rotation());
        float ox = look.x * 2, oy = look.y * 2, oz = look.z * 2;
        float partial = context.deltaTracker().getGameTimeDeltaPartialTick(false);
        updateRange(client);
        float[] targets = new float[candidateCount * STRIDE];
        int[] colors = new int[candidateCount];
        int count = 0;
        for (int candidate = 0; candidate < candidateCount; candidate++) {
            Entity entity = candidates[candidate];
            if (!entity.isAlive() || entity.isRemoved()) continue;
            TracerTarget type = candidateTypes[candidate];
            if (type == null || !config.isSelected(type)) continue;
            double px = Mth.lerp(partial, entity.xOld, entity.getX());
            double py = Mth.lerp(partial, entity.yOld, entity.getY());
            double pz = Mth.lerp(partial, entity.zOld, entity.getZ());
            if (!range.contains(px, pz)) continue;
            var box = entity.getBoundingBox();
            double dx = px - entity.getX() - camera.x, dy = py - entity.getY() - camera.y, dz = pz - entity.getZ() - camera.z;
            int at = count * STRIDE;
            targets[at] = (float) (px - camera.x);
            targets[at + 1] = (float) (py + entity.getBbHeight() * .5 - camera.y);
            targets[at + 2] = (float) (pz - camera.z);
            targets[at + 3] = (float) (box.minX + dx - .03);
            targets[at + 4] = (float) (box.minY + dy - .03);
            targets[at + 5] = (float) (box.minZ + dz - .03);
            targets[at + 6] = (float) (box.maxX + dx + .03);
            targets[at + 7] = (float) (box.maxY + dy + .03);
            targets[at + 8] = (float) (box.maxZ + dz + .03);
            float nx = targets[at] - ox, ny = targets[at + 1] - oy, nz = targets[at + 2] - oz;
            double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length < 1e-5) { nx = 0; ny = 1; nz = 0; length = 1; }
            targets[at + 9] = (float) (nx / length);
            targets[at + 10] = (float) (ny / length);
            targets[at + 11] = (float) (nz / length);
            colors[count++] = config.lineColor(type);
        }
        // The two privately owned arrays are never mutated after publishing this frame.
        if (count > 0) context.levelState().setData(LINES, new Frame(ox, oy, oz, targets, colors, count));
    }

    void submit(LevelRenderContext context) {
        Frame frame = context.levelState().getData(LINES);
        if (frame == null || frame.count() == 0) return;
        context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RENDER_TYPE, (pose, buffer) -> {
            float[] a = frame.targets();
            for (int target = 0; target < frame.count(); target++) {
                int at = target * STRIDE, color = frame.colors()[target];
                vertex(buffer, pose, frame.ox(), frame.oy(), frame.oz(), a[at + 9], a[at + 10], a[at + 11], color, 2);
                vertex(buffer, pose, a[at], a[at + 1], a[at + 2], a[at + 9], a[at + 10], a[at + 11], color, 2);
                for (int edge = 0; edge < BoxEdges.CORNERS.length; edge += 2) {
                    int from = BoxEdges.CORNERS[edge], to = BoxEdges.CORNERS[edge + 1];
                    float nx = (to & 1) - (from & 1), ny = ((to >> 1) & 1) - ((from >> 1) & 1), nz = ((to >> 2) & 1) - ((from >> 2) & 1);
                    corner(buffer, pose, a, at, from, nx, ny, nz, color);
                    corner(buffer, pose, a, at, to, nx, ny, nz, color);
                }
            }
        });
    }

    private static void corner(VertexConsumer buffer, PoseStack.Pose pose, float[] a, int at, int corner,
            float nx, float ny, float nz, int color) {
        vertex(buffer, pose, a[at + ((corner & 1) == 0 ? 3 : 6)], a[at + ((corner & 2) == 0 ? 4 : 7)],
                a[at + ((corner & 4) == 0 ? 5 : 8)], nx, ny, nz, color, 1.5f);
    }
    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z,
            float nx, float ny, float nz, int color, float width) {
        buffer.addVertex(pose, x, y, z).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width);
    }
    private record Frame(float ox, float oy, float oz, float[] targets, int[] colors, int count) { }
}

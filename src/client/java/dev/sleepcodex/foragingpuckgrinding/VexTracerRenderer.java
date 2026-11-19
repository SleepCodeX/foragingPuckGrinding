package dev.sleepcodex.foragingpuckgrinding;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.Optional;
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
    private int candidateCount;
    private TracerRange.Bounds range;
    private int rangeChunkX, rangeChunkZ, rangeChunks;


    VexTracerRenderer(TracerConfig config) { this.config = config; }

    void tick(Minecraft client) {
        if (cachedLevel != client.level) {
            clearCandidates();
            cachedLevel = client.level;
            range = null;
        }
        boolean tracking = config.enabled() && config.hasTargets() && client.player != null && cachedLevel != null;
        boolean area = torrhus.update(client, tracking &&
                (config.isSelected(TracerTarget.BLUE_JAY) || config.isSelected(TracerTarget.HIVE_THIEF)));
        if (!tracking) {
            clearCandidates();
            return;
        }
        updateRange(client);
        int count = 0;
        // Inspect every loaded entity in the complete chunk footprint once per tick.
        // Reuse discovery storage and classify each mob here, never in the render loop.
        for (Entity entity : cachedLevel.entitiesForRendering()) {
            if (!entity.isAlive() || entity.isRemoved() || !range.contains(entity.getX(), entity.getZ())) continue;
            TracerTarget type = typeOf(entity);
            if (type == null || !config.isSelected(type) || (type != TracerTarget.VEX && !area)) continue;
            if (count == candidates.length) {
                int capacity = Math.max(16, count * 2);
                candidates = Arrays.copyOf(candidates, capacity);
                candidateTypes = Arrays.copyOf(candidateTypes, capacity);
            }
            candidates[count] = entity;
            candidateTypes[count++] = type;
        }
        if (count < candidateCount) {
            Arrays.fill(candidates, count, candidateCount, null);
            Arrays.fill(candidateTypes, count, candidateCount, null);
        }
        candidateCount = count;
    }

    private void clearCandidates() {
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
            colors[count++] = type.color();
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

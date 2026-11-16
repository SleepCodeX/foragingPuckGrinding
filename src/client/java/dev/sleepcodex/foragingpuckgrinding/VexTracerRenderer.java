package dev.sleepcodex.foragingpuckgrinding;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

final class VexTracerRenderer {
    private static final RenderStateDataKey<Frame> LINES = RenderStateDataKey.create(() -> "Hunting Helper lines");
    private static final Frame EMPTY = new Frame(0, 0, 0, new float[0], 0);
    private static final RenderType RENDER_TYPE = RenderType.create("vex_tracers",
            RenderSetup.builder(RenderPipelines.register(RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(VexTracerClient.MOD_ID, "pipeline/vex_tracers"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
                    .withPrimitiveTopology(PrimitiveTopology.LINES)
                    .withDepthStencilState(Optional.empty())
                    .withCull(false)
                    .build())).createRenderSetup());
    private static final int COLOR = 0xFFCC55FF;
    private final TracerConfig config;
    private final TorrhusContext torrhus = new TorrhusContext();
    private final Vector3f look = new Vector3f();
    private ClientLevel cachedLevel;
    private List<Entity> candidates = List.of();

    VexTracerRenderer(TracerConfig config) {
        this.config = config;
    }

    void tick(Minecraft client) {
        cachedLevel = client.level;
        boolean blueJayArea = torrhus.update(client, config.enabled() && config.target().includesBlueJay());
        if (!config.enabled() || cachedLevel == null) {
            candidates = List.of();
            return;
        }
        // Discover targets once per client tick, rather than rescanning every rendered frame.
        var selected = new ArrayList<Entity>();
        for (Entity entity : cachedLevel.entitiesForRendering()) {
            if (!entity.isAlive() || entity.isRemoved()) continue;
            boolean vex = config.target().includesVex() && entity instanceof Vex;
            // Skyblocker's Torrhus Blue Jay detector confirms Parrot.BLUE in this location.
            // Other parrot colors, tamed pets, and blue parrots outside Torrhus are excluded.
            boolean blueJay = config.target().includesBlueJay() && blueJayArea && entity instanceof Parrot parrot
                    && parrot.getVariant() == Parrot.Variant.BLUE && !parrot.isTame();
            if (vex || blueJay) selected.add(entity);
        }
        candidates = selected.isEmpty() ? List.of() : List.copyOf(selected);
    }

    void extract(LevelExtractionContext context) {
        // Always replace this frame's data, including when OFF or no Vexes remain.
        context.levelState().setData(LINES, EMPTY);
        Minecraft client = Minecraft.getInstance();
        if (!config.enabled() || candidates.isEmpty() || context.level() != cachedLevel
                || client.player == null || client.gui.hud.isHidden()) return;
        var camera = context.camera().position();
        look.set(0, 0, -1).rotate(context.camera().rotation());
        // Moving the origin forward avoids near-plane clipping and anchors the line at the crosshair.
        float ox = look.x * 2, oy = look.y * 2, oz = look.z * 2;
        float partialTick = context.deltaTracker().getGameTimeDeltaPartialTick(false);
        int chunks = client.options.getEffectiveRenderDistance();
        float[] lines = new float[candidates.size() * 6];
        int count = 0;
        for (Entity entity : candidates) {
            if (!entity.isAlive() || entity.isRemoved()) continue;
            double px = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double pz = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            if (!TracerRange.contains(camera.x, camera.z, px, pz, chunks)) continue;
            float tx = (float) (px - camera.x);
            float ty = (float) (Mth.lerp(partialTick, entity.yOld, entity.getY()) + entity.getBbHeight() * 0.5 - camera.y);
            float tz = (float) (pz - camera.z);
            float dx = tx - ox, dy = ty - oy, dz = tz - oz;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1.0e-5) continue;
            lines[count++] = tx;
            lines[count++] = ty;
            lines[count++] = tz;
            lines[count++] = (float) (dx / length);
            lines[count++] = (float) (dy / length);
            lines[count++] = (float) (dz / length);
        }
        // This privately owned array is never changed after publishing the frame.
        if (count != 0) context.levelState().setData(LINES, new Frame(ox, oy, oz, lines, count));
    }

    void submit(LevelRenderContext context) {
        Frame frame = context.levelState().getData(LINES);
        if (frame == null || frame.used() == 0) return;
        context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RENDER_TYPE, (pose, buffer) -> {
            float[] lines = frame.lines();
            for (int i = 0; i < frame.used(); i += 6) {
                vertex(buffer, pose, frame.ox(), frame.oy(), frame.oz(), lines[i + 3], lines[i + 4], lines[i + 5]);
                vertex(buffer, pose, lines[i], lines[i + 1], lines[i + 2], lines[i + 3], lines[i + 4], lines[i + 5]);
            }
        });
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z,
            float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z)
                .setColor(COLOR)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(2.0f);
    }

    private record Frame(float ox, float oy, float oz, float[] lines, int used) { }
}

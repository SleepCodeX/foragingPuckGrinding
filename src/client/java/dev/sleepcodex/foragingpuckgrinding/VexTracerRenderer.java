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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class VexTracerRenderer {
    private static final RenderStateDataKey<List<Line>> LINES = RenderStateDataKey.create(() -> "Vex tracer lines");
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

    VexTracerRenderer(TracerConfig config) {
        this.config = config;
    }

    void extract(LevelExtractionContext context) {
        // Always replace this frame's data, including when OFF or no Vexes remain.
        context.levelState().setData(LINES, List.of());
        Minecraft client = Minecraft.getInstance();
        if (!config.enabled() || client.player == null || client.gui.hud.isHidden()) return;
        Vec3 camera = context.camera().position();
        Vector3f look = new Vector3f(0, 0, -1).rotate(context.camera().rotation());
        // Moving the origin forward avoids near-plane clipping and anchors the line at the crosshair.
        Vec3 origin = new Vec3(look.x, look.y, look.z).scale(2.0);
        float partialTick = context.deltaTracker().getGameTimeDeltaPartialTick(false);
        int chunks = client.options.getEffectiveRenderDistance();
        List<Line> lines = new ArrayList<>();
        for (Entity entity : context.level().entitiesForRendering()) {
            if (!(entity instanceof Vex) || !entity.isAlive() || entity.isRemoved()) continue;
            Vec3 position = entity.getPosition(partialTick);
            if (!TracerRange.contains(camera.x, camera.z, position.x, position.z, chunks)) continue;
            Vec3 target = position.add(0, entity.getBbHeight() * 0.5, 0).subtract(camera);
            Vec3 direction = target.subtract(origin);
            if (direction.lengthSqr() < 1.0e-10) continue;
            lines.add(new Line(origin, target, direction.normalize()));
        }
        // Only immutable camera-relative coordinates cross into the drawing phase.
        context.levelState().setData(LINES, List.copyOf(lines));
    }

    void submit(LevelRenderContext context) {
        List<Line> lines = context.levelState().getData(LINES);
        if (lines == null || lines.isEmpty()) return;
        context.submitNodeCollector().submitCustomGeometry(context.poseStack(), RENDER_TYPE, (pose, buffer) -> {
            for (Line line : lines) {
                vertex(buffer, pose, line.start(), line.normal());
                vertex(buffer, pose, line.end(), line.normal());
            }
        });
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vec3 point, Vec3 normal) {
        buffer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(COLOR)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z)
                .setLineWidth(2.0f);
    }

    private record Line(Vec3 start, Vec3 end, Vec3 normal) { }
}

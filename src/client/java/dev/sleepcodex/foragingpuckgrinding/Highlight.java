package dev.sleepcodex.foragingpuckgrinding;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Optional;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/** The light see-through tint painted under the model outline of a glowing mob. */
public final class Highlight {
    /** Fill opacity (about 19%, a light tint); the outline itself stays fully opaque. */
    public static final int FILL_ALPHA = 0x30;
    public static final int FULL_BRIGHT = 0xF000F0;

    /** Entity translucent pipeline without a depth test, so the tint shows through blocks like the outline. */
    private static final RenderPipeline FILL_PIPELINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VexTracerClient.MOD_ID, "pipeline/highlight_fill"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1f)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withDepthStencilState(Optional.empty())
            .build());
    public static final RenderType FILL = RenderType.create("hunting_helper_fill",
            RenderSetup.builder(FILL_PIPELINE)
                    .withTexture("Sampler0", Identifier.fromNamespaceAndPath(VexTracerClient.MOD_ID, "textures/misc/white.png"))
                    .useLightmap()
                    .useOverlay()
                    .sortOnUpload()
                    .createRenderSetup());

    private Highlight() { }

    /** The tint color for a glow color: same hue, light alpha. */
    public static int fillColor(int glow) { return (FILL_ALPHA << 24) | (glow & 0xFFFFFF); }
}

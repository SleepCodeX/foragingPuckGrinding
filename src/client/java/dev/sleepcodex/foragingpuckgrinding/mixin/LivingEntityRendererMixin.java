package dev.sleepcodex.foragingpuckgrinding.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.sleepcodex.foragingpuckgrinding.Highlight;
import dev.sleepcodex.foragingpuckgrinding.HighlightState;
import dev.sleepcodex.foragingpuckgrinding.HuntingDebug;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Light tint under the outline: the body model is submitted a second time with a see-through fill. */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private void huntingHelperFill(SubmitNodeCollector collector, Model model, Object state, PoseStack pose, RenderType type,
            int light, int overlay, int tint, TextureAtlasSprite sprite, int outline,
            ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original) {
        original.call(collector, model, state, pose, type, light, overlay, tint, sprite, outline, crumbling);
        int color = state instanceof HighlightState highlight ? highlight.huntingHelper$glow() : 0;
        if (color == 0) return;
        HuntingDebug.FILLS.incrementAndGet();
        collector.submitModel(model, state, pose, Highlight.FILL, Highlight.FULL_BRIGHT, overlay,
                Highlight.fillColor(color), null, 0, null);
    }
}

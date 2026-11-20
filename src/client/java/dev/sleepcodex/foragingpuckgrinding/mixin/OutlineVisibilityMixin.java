package dev.sleepcodex.foragingpuckgrinding.mixin;

import dev.sleepcodex.foragingpuckgrinding.TrackedOutlines;
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelExtractor.class)
abstract class OutlineVisibilityMixin {
    @Shadow @Final private LevelRenderer levelRenderer;

    @Inject(method = "isEntityVisible", at = @At("HEAD"), cancellable = true)
    private void huntingHelperVisibleOutline(Entity entity, Frustum frustum, double x, double y, double z,
            CallbackInfoReturnable<Boolean> cir) {
        if (TrackedOutlines.colorFor(entity) != 0 || SafariEsp.outlinedThroughWalls(entity)) {
            // Keep frustum culling (the size-based distance cull is lifted by EntityRenderDistanceMixin),
            // but include outlined mobs in occluded sections.
            cir.setReturnValue(levelRenderer.entityRenderDispatcher().shouldRender(entity, frustum, x, y, z));
        }
    }
}

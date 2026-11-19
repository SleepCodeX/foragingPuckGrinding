package dev.sleepcodex.foragingpuckgrinding.mixin;

import dev.sleepcodex.foragingpuckgrinding.TrackedOutlines;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
abstract class EntityOutlineMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("TAIL"))
    private void huntingHelperOutline(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        int color = TrackedOutlines.colorFor(entity);
        if (color != 0) state.outlineColor = color;
    }
}

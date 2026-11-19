package dev.sleepcodex.foragingpuckgrinding.mixin;

import dev.sleepcodex.foragingpuckgrinding.TrackedOutlines;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla stops rendering small mobs after a size-based distance (a bee or Vex vanishes after ~40-60 blocks),
 * while tracer lines reach the whole render distance. Glowing mobs skip only that distance check, so the glow
 * reaches as far as the line; frustum culling in EntityRenderer still applies.
 */
@Mixin(Entity.class)
abstract class EntityRenderDistanceMixin {
    @Inject(method = "shouldRender(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void huntingHelperRenderFar(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (TrackedOutlines.colorFor((Entity) (Object) this) != 0) cir.setReturnValue(true);
    }
}

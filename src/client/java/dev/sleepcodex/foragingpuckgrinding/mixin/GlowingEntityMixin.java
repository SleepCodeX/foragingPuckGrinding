package dev.sleepcodex.foragingpuckgrinding.mixin;

import dev.sleepcodex.foragingpuckgrinding.TrackedOutlines;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Glowing tracked mobs count as glowing for the client. The EntityCulling mod never culls entities for which
 * this returns true, so they keep rendering (and outlining) behind walls with it installed.
 */
@Mixin(Minecraft.class)
abstract class GlowingEntityMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void huntingHelperGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (TrackedOutlines.colorFor(entity) != 0) cir.setReturnValue(true);
    }
}

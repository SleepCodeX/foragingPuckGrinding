package net.johnceo.sparklingmutuals.mixin;

import net.johnceo.sparklingmutuals.safari.esp.SafariEsp;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Outlined critters count as glowing for the client. EntityCulling and Sodium's entity culling never cull glowing
 * entities, so the critter glow stays visible behind walls instead of only inside the player's room.
 * SafariShinyMixin still replaces the outline color afterwards.
 */
@Mixin(Minecraft.class)
abstract class SafariCullingBypassMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void sm$outlinedCritterGlows(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (SafariEsp.outlinedThroughWalls(entity)) cir.setReturnValue(true);
    }
}

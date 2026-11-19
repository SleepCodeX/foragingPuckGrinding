package dev.sleepcodex.foragingpuckgrinding.mixin;

import net.johnceo.sparklingmutuals.party.PartyManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Safari-shaped chat can be replayed in any world. Only a Hypixel connection can answer roster requests. */
@Mixin(value = PartyManager.class, remap = false)
abstract class SafariPartyConnectionMixin {
    @Inject(method = {"requestPartyInfo", "refreshPartyInfo"}, at = @At("HEAD"), cancellable = true)
    private void huntingHelper$checkConnection(CallbackInfo ci) {
        if (!HypixelSessionAccess.huntingHelper$isOnHypixel()) ci.cancel();
    }
}

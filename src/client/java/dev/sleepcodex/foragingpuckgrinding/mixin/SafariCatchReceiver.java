package dev.sleepcodex.foragingpuckgrinding.mixin;

import net.johnceo.sparklingmutuals.safari.SafariTracking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = SafariTracking.class, remap = false)
public interface SafariCatchReceiver {
    @Invoker("receive")
    void huntingHelper$receive(String line);
}

package dev.sleepcodex.foragingpuckgrinding.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Use the exact connection check used by the nested HM API transport. */
@Pseudo
@Mixin(targets = "net.azureaaron.hmapi.utils.Utils", remap = false)
public interface HypixelSessionAccess {
    @Invoker("isOnHypixel")
    static boolean huntingHelper$isOnHypixel() { throw new AssertionError(); }
}

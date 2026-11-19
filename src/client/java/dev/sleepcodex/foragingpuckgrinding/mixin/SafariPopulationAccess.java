package dev.sleepcodex.foragingpuckgrinding.mixin;

import java.util.Map;
import net.johnceo.sparklingmutuals.safari.SafariRun;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only view of the run's capture-adjusted population high-water marks. */
@Mixin(value = SafariRun.class, remap = false)
public interface SafariPopulationAccess {
    @Accessor("knownPopulation")
    Map<String, Integer> huntingHelper$populations();
}

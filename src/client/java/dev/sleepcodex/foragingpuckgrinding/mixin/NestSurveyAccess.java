package dev.sleepcodex.foragingpuckgrinding.mixin;

import net.johnceo.sparklingmutuals.safari.structures.BeeNests;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Feed a confirmed nearby Honeybug release into the same survey as an exact nest click. */
@Mixin(value = BeeNests.class, remap = false)
public interface NestSurveyAccess {
    @Invoker("clicked")
    void huntingHelper$confirmNest(Level level, BlockPos nest);
}

package dev.sleepcodex.foragingpuckgrinding.mixin;

import dev.sleepcodex.foragingpuckgrinding.HighlightState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
abstract class EntityRenderStateMixin implements HighlightState {
    @Unique private int huntingHelper$glow;

    @Override public int huntingHelper$glow() { return huntingHelper$glow; }

    @Override public void huntingHelper$setGlow(int color) { huntingHelper$glow = color; }
}

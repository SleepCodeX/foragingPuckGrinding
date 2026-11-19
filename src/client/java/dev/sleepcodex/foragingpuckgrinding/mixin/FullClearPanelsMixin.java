package dev.sleepcodex.foragingpuckgrinding.mixin;

import dev.sleepcodex.foragingpuckgrinding.FullClearProgress;
import net.johnceo.sparklingmutuals.hud.HudPanel;
import net.johnceo.sparklingmutuals.safari.PartySparklingState;
import net.johnceo.sparklingmutuals.safari.SafariBiome;
import net.johnceo.sparklingmutuals.safari.SafariMode;
import net.johnceo.sparklingmutuals.safari.SafariRun;
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear;
import net.johnceo.sparklingmutuals.safari.structures.WallSummary;
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SafariPanels.class, remap = false)
abstract class FullClearPanelsMixin {
    @Inject(method = "progress", at = @At("RETURN"), cancellable = true)
    private void huntingHelper$coverage(SafariRun run, boolean unique, long now, PartySparklingState party,
                                        CallbackInfoReturnable<HudPanel> cir) {
        if (SafariFullClear.INSTANCE.getMode() == SafariMode.FULL_CLEAR)
            cir.setReturnValue(FullClearProgress.progress(run, cir.getReturnValue()));
    }

    @Inject(method = "missing", at = @At("HEAD"), cancellable = true)
    private void huntingHelper$remaining(SafariRun run, SafariBiome area, boolean unique, int nests,
                                         int mounds, WallSummary walls, PartySparklingState party,
                                         CallbackInfoReturnable<HudPanel> cir) {
        if (SafariFullClear.INSTANCE.getMode() == SafariMode.FULL_CLEAR)
            cir.setReturnValue(FullClearProgress.missing(run, area));
    }
}

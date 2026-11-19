package dev.sleepcodex.foragingpuckgrinding;

/** Added to every entity render state by a mixin: the resolved glow color, 0 when the mob is not tracked. */
public interface HighlightState {
    int huntingHelper$glow();

    void huntingHelper$setGlow(int color);
}

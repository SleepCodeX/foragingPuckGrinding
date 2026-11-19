package dev.sleepcodex.foragingpuckgrinding.mixin;

import net.johnceo.sparklingmutuals.history.BazaarPrices;
import net.johnceo.sparklingmutuals.history.BazaarPriceCache;
import net.johnceo.sparklingmutuals.skyblock.market.QuoteSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the pinned Safari suite's existing top-of-book snapshot and uses its shared refresh throttle. */
@Mixin(value = BazaarPrices.class, remap = false)
public interface MarketCacheAccess {
    @Accessor("guidanceQuotes")
    static QuoteSnapshot quotes() { throw new AssertionError(); }
    @Accessor("cache")
    static BazaarPriceCache cache() { throw new AssertionError(); }
}

package dev.sleepcodex.foragingpuckgrinding.journal.client;

import dev.sleepcodex.foragingpuckgrinding.journal.data.Critter;
import dev.sleepcodex.foragingpuckgrinding.journal.data.Critters;
import dev.sleepcodex.foragingpuckgrinding.journal.session.RunRecord;
import dev.sleepcodex.foragingpuckgrinding.journal.session.SafariSession;
import java.util.List;
import java.util.Map;

/** Side-by-side field valuations share the Safari suite's public Bazaar request and order book. */
public final class BazaarPrices {
    private BazaarPrices() {}
    private static int ticks;
    private static final net.johnceo.sparklingmutuals.history.BazaarPrices SUITE =
            net.johnceo.sparklingmutuals.history.BazaarPrices.INSTANCE;

    public static void tick() {
        if (++ticks < 200) return;
        ticks = 0;
        if (SafariLocation.inSafari()) refreshIfStale();
    }
    public static void refreshIfStale() {
        if (ConfigManager.get().profit.enabled) SUITE.marketCache().ensureFresh(120_000L);
    }
    public static boolean known() { return SUITE.guidanceQuotes() != null; }
    public static long ageMillis() {
        var quote = SUITE.guidanceQuotes();
        return quote == null ? -1 : Math.max(0, System.currentTimeMillis() - quote.getServerAt());
    }
    public static double price(Critter critter, CritterConfig.PriceSource source) {
        var snapshot = SUITE.guidanceQuotes();
        var quote = snapshot == null ? null : snapshot.getQuotes().get(critter.bazaarId());
        if (quote == null) return 0;
        double raw = source == CritterConfig.PriceSource.SELL_OFFER ? quote.getAsk() : quote.getBid();
        return ConfigManager.get().profit.subtractTax ? raw * .9875 : raw;
    }

	/** The same, at whichever side the settings pick — the figure everything shows. */
	public static double price(Critter critter) {
		return price(critter, ConfigManager.get().profit.priceSource());
	}

	/** What a pile of shards is worth. Species with no price simply add nothing. */
	public static long valueOf(Map<Critter, Integer> shards, CritterConfig.PriceSource source) {
		double total = 0;
		for (Map.Entry<Critter, Integer> entry : shards.entrySet()) {
			total += price(entry.getKey(), source) * entry.getValue();
		}
		return Math.round(total);
	}

	/** What the shards of a live or just-finished run are worth. */
	public static long valueOf(SafariSession session, CritterConfig.PriceSource source) {
		return session == null ? 0 : valueOf(session.shardCounts(), source);
	}

	public static long valueOf(SafariSession session) {
		return valueOf(session, ConfigManager.get().profit.priceSource());
	}

	/** What a saved run's shards are worth, or {@code 0} if it kept no breakdown. */
	public static long valueOf(RunRecord run) {
		if (run == null || !run.hasShardData()) return 0;
		CritterConfig.PriceSource source = ConfigManager.get().profit.priceSource();
		double total = 0;
		for (Map.Entry<String, Integer> entry : run.shards.entrySet()) {
			// A saved run holds species by name, so one dropped from the roster since is
			// simply skipped — the same thing the rest of the history does with it.
			Critter critter = Critters.byName(entry.getKey());
			if (critter != null) total += price(critter, source) * entry.getValue();
		}
		return Math.round(total);
	}

	/**
	 * A run's value as it is written out, in the one place that decides the wording.
	 *
	 * <p>One figure normally — instant sell unless the settings say otherwise. With
	 * <b>Show both prices</b> on, the other side follows it, named, because the two are
	 * far apart: selling into the buy orders now and listing an offer and waiting are
	 * different amounts of money for the same shards, and the gap is the point.
	 */
	public static String coinsText(SafariSession session) {
		CritterConfig.PriceSource chosen = ConfigManager.get().profit.priceSource();
		String text = format(valueOf(session, chosen)) + " coins";
		if (!ConfigManager.get().profit.showBothPrices) return text;

		CritterConfig.PriceSource other = chosen == CritterConfig.PriceSource.SELL_OFFER
			? CritterConfig.PriceSource.INSTANT_SELL : CritterConfig.PriceSource.SELL_OFFER;
		return "%s  ·  %s %s".formatted(text, format(valueOf(session, other)), name(other));
	}

	/** How a price side is referred to in a sentence. */
	public static String name(CritterConfig.PriceSource source) {
		return source == CritterConfig.PriceSource.SELL_OFFER ? "as sell offers" : "instant sell";
	}

	/** Every priceable run added up. Runs with no breakdown are left out, not zeroed. */
	public static long totalValue(List<RunRecord> runs) {
		long total = 0;
		for (RunRecord run : runs) total += valueOf(run);
		return total;
	}

	/**
	 * Coins as they are said out loud: {@code 1.2M}, {@code 340k}, {@code 8,120}.
	 *
	 * <p>A run is worth millions and the screen has a column's width to say so in.
	 */
	public static String format(long coins) {
		long value = Math.abs(coins);
		if (value >= 1_000_000_000L) return "%.2fB".formatted(coins / 1_000_000_000.0);
		if (value >= 1_000_000L) return "%.2fM".formatted(coins / 1_000_000.0);
		if (value >= 10_000L) return "%.0fk".formatted(coins / 1_000.0);
		return "%,d".formatted(coins);
	}
}

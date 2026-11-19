package dev.sleepcodex.foragingpuckgrinding.journal.client;

/** Preferences for field-journal tools. Shared Safari highlights and HUDs live in Safari settings. */
public final class CritterConfig {
    public enum Broadcast {
        NONE, PARTY, ALL;
        public String command() { return switch (this) { case PARTY -> "pc"; case ALL -> "ac"; default -> null; }; }
    }
    public enum Scope {
        SAFARI, SAFARI_AND_ENTRANCE, EVERYWHERE;
        public boolean covers(SafariLocation.Where where) {
            return this == EVERYWHERE || where == SafariLocation.Where.INSIDE
                    || this == SAFARI_AND_ENTRANCE && where == SafariLocation.Where.ENTRANCE;
        }
    }
    public enum PriceSource { INSTANT_SELL, SELL_OFFER }

    public DisplayConfig display = new DisplayConfig();
    public AlertConfig alerts = new AlertConfig();
    public PartyConfig party = new PartyConfig();
    public ProfitConfig profit = new ProfitConfig();
    public AdvancedConfig advanced = new AdvancedConfig();

    public static final class DisplayConfig {
        public boolean hudEnabled = true, showTrades = true, showCoverage, showMissing, showPerPlayer = true;
        public boolean uniqueOnly, countSpawns, highlightTrades, highlightTroodonWalls, highlightHideonwalls;
        public boolean recatchHelper, removeDarkness, showTroodonWalls = true;
        public int showWhere;
        public float progressScale = HudBox.DEFAULT_SCALE, missingScale = HudBox.DEFAULT_SCALE, tradesScale = HudBox.DEFAULT_SCALE;
        public float progressX = .004f, progressY = .006f, missingX = .78f, missingY = .006f, tradesX = .006f, tradesY = .62f;
        public String tradeColour = "#FF55FFFF", troodonWallColour = "#FF55AAFF", hideonwallColour = "#FFAA55FF";
        public String hideonwallFoundColour = "#FF55FF55", recatchColour = "#FFFFFF55";
        public Scope scope() { return Scope.values()[Math.clamp(showWhere, 0, 2)]; }
        public boolean showsHere() { return scope().covers(SafariLocation.where()); }
    }
    public static final class AlertConfig {
        public boolean alertSound, encountersInBiomeOnly = true, gemzieAlert, wumpaAlert, doomspiralAlert;
        public boolean biomeDoneNotify, allButMacawNotify, allDoneNotify, birdfeederAlert, traderAlerts = true;
    }
    public static final class PartyConfig {
        public int gemzieBroadcast, wumpaBroadcast, doomspiralBroadcast, milestoneBroadcast;
        public int macawBroadcast, sparklingBroadcast, hotspotBroadcast, traderBroadcast, shareChannel;
        public boolean acceptSharedTrades = true;
        public String shareCommand() { return switch (shareChannel) { case 1 -> "ac"; case 2 -> ""; default -> "pc"; }; }
        private static Broadcast channel(int value) { return Broadcast.values()[Math.clamp(value, 0, 2)]; }
        public Broadcast gemzie() { return channel(gemzieBroadcast); }
        public Broadcast wumpa() { return channel(wumpaBroadcast); }
        public Broadcast doomspiral() { return channel(doomspiralBroadcast); }
        public Broadcast milestones() { return channel(milestoneBroadcast); }
        public Broadcast macaw() { return channel(macawBroadcast); }
        public Broadcast sparkling() { return channel(sparklingBroadcast); }
        public Broadcast hotspot() { return channel(hotspotBroadcast); }
        public Broadcast trades() { return channel(traderBroadcast); }
    }
    public static final class ProfitConfig {
        public boolean enabled = true, showBothPrices = true, subtractTax = true, showOnHud = true, importedOnce;
        public int priceSource;
        public PriceSource priceSource() { return priceSource == 1 ? PriceSource.SELL_OFFER : PriceSource.INSTANT_SELL; }
    }
    public static final class AdvancedConfig { public boolean debugCommands, showMounds; }
}

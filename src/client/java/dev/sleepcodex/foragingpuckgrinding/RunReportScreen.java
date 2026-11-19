package dev.sleepcodex.foragingpuckgrinding;

import java.util.Map;
import net.johnceo.sparklingmutuals.config.ConfigManager;
import net.johnceo.sparklingmutuals.history.BazaarPrices;
import net.johnceo.sparklingmutuals.history.RunHistory;
import net.johnceo.sparklingmutuals.hunting.HuntingCompletion;
import net.johnceo.sparklingmutuals.hunting.HuntingConfig;
import net.johnceo.sparklingmutuals.hunting.HuntingRuns;
import net.johnceo.sparklingmutuals.safari.SafariBiome;
import net.johnceo.sparklingmutuals.safari.SafariRoster;
import net.johnceo.sparklingmutuals.safari.SafariRun;
import net.johnceo.sparklingmutuals.safari.SafariTracking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/**
 * The current (or last) Safari run by area: what is caught out of the quota, who caught it, capsules thrown and
 * escaped, shards received and what they are worth. Everything is read from Sparkling Mutuals' run ledger and history.
 */
final class RunReportScreen extends TrackerPage {
    private static final double TAX = .9875;
    private SafariBiome area;

    RunReportScreen(Screen parent) {
        super("Safari run", parent);
        var current = SafariTracking.INSTANCE.getDisplayBiome();
        area = current == null ? SafariBiome.FOREST : current;
        BazaarPrices.INSTANCE.ensureFresh();
    }

    @Override int preferredWidth() { return 560; }
    @Override int preferredHeight() { return 320; }

    private static SafariRun run() { return SafariTracking.INSTANCE.getRun(); }

    @Override String subtitle() {
        SafariRun run = run();
        if (run == null) return "No run yet  ·  Quotas per area";
        var source = HuntingCompletion.INSTANCE.of(run);
        int done = 0;
        for (var critter : SafariRoster.INSTANCE.getAll()) if (HuntingCompletion.INSTANCE.done(critter, source, HuntingConfig.INSTANCE.getUniquesOnly())) done++;
        long seconds = run.elapsed(System.currentTimeMillis()) / 1000;
        return (run.getEndedAt() == null ? "Current run" : "Last run") + "  ·  " + done + " / " + SafariRoster.INSTANCE.getAll().size()
                + " species done  ·  " + String.format("%d:%02d", seconds / 60, seconds % 60)
                + (HuntingConfig.INSTANCE.getUniquesOnly() ? "  ·  Uniques only" : "");
    }

    @Override void layout() {
        int x = left + 15;
        int tabWidth = (w - 42) / 4;
        for (var tab : SafariBiome.values()) {
            button(tab.getLabel(), x, top + 52, tabWidth, 20, tab == area ? Tone.PRIMARY : Tone.NORMAL, () -> { area = tab; refresh(); });
            x += tabWidth + 4;
        }
        button("Back", left + 15, footer, 61, 23, Tone.NORMAL, this::onClose);
    }

    private static double price(String species) {
        Double quote = BazaarPrices.INSTANCE.cachedShardPrice(species, ConfigManager.INSTANCE.getRunCoinsInstaSell());
        if (quote == null) return 0;
        return HuntingConfig.INSTANCE.getSubtractTax() ? quote * TAX : quote;
    }

    @Override void content(GuiGraphicsExtractor g, MenuCanvas c, int mx, int my, float delta) {
        SafariRun run = run();
        var source = HuntingCompletion.INSTANCE.of(run);
        boolean unique = HuntingConfig.INSTANCE.getUniquesOnly();
        Map<String, Integer> shards = run == null ? Map.of() : RunHistory.INSTANCE.liveShards(run);
        String self = Minecraft.getInstance().getUser().getName();
        int x = left + 18, right = left + w - 18, y = top + 80;
        int caughtX = x + 98, byX = x + 150, triedX = right - 168, escapedX = right - 128, shardsX = right - 88;
        double head = .72;
        MenuFont.text(g, "Critter", x, y, head, MenuTheme.MUTED);
        MenuFont.text(g, "Caught", caughtX, y, head, MenuTheme.MUTED);
        MenuFont.text(g, "By", byX, y, head, MenuTheme.MUTED);
        MenuFont.text(g, "Tried", triedX, y, head, MenuTheme.MUTED);
        MenuFont.text(g, "Esc.", escapedX, y, head, MenuTheme.MUTED);
        MenuFont.text(g, "Shards", shardsX, y, head, MenuTheme.MUTED);
        MenuFont.text(g, "Value", right - MenuFont.width("Value") * head, y, head, MenuTheme.MUTED);
        y += 13;
        var critters = area.getCritters();
        int step = Math.max(10, Math.min(15, (footer - 22 - y) / Math.max(1, critters.size())));
        double scale = Math.min(.8, step / 15.0 * .82);
        for (var critter : critters) {
            String name = critter.getName();
            int caught = source.caught(name);
            int target = HuntingCompletion.INSTANCE.target(critter, unique);
            boolean gone = HuntingCompletion.INSTANCE.unavailable(critter, source);
            boolean done = HuntingCompletion.INSTANCE.done(critter, source, unique);
            MenuFont.text(g, name, x, y, scale, done ? MenuTheme.TEXT : MenuTheme.MUTED);
            String count = gone ? "none this run" : target > 1 ? caught + "/" + target : Integer.toString(caught);
            MenuFont.text(g, count, caughtX, y, scale, done ? 0xFF7FD6A0 : MenuTheme.TEXT);
            StringBuilder by = new StringBuilder();
            for (var entry : HuntingRuns.INSTANCE.catchers(run, name, self)) {
                if (!by.isEmpty()) by.append(", ");
                by.append(entry.getFirst().equals(self) ? "You" : entry.getFirst());
                if (entry.getSecond() > 1) by.append(" ×").append(entry.getSecond());
            }
            MenuFont.text(g, MenuFont.ellipsis(by.toString(), (triedX - byX - 8) / scale), byX, y, scale, MenuTheme.ACCENT);
            int tried = HuntingRuns.INSTANCE.attempts(run, name), escaped = HuntingRuns.INSTANCE.escapes(run, name);
            MenuFont.text(g, tried == 0 ? "–" : Integer.toString(tried), triedX, y, scale, MenuTheme.TEXT);
            MenuFont.text(g, escaped == 0 ? "–" : Integer.toString(escaped), escapedX, y, scale, MenuTheme.TEXT);
            int amount = shards.getOrDefault(name, 0);
            MenuFont.text(g, amount == 0 ? "–" : Integer.toString(amount), shardsX, y, scale, MenuTheme.TEXT);
            String value = amount == 0 ? "–" : BazaarPrices.INSTANCE.format(Math.round(price(name) * amount));
            MenuFont.text(g, value, right - MenuFont.width(value) * scale, y, scale, 0xFFE6D08A);
            y += step;
        }
        long total = 0;
        boolean priced = BazaarPrices.INSTANCE.ready();
        for (var entry : shards.entrySet()) total += Math.round(price(entry.getKey()) * entry.getValue());
        String side = ConfigManager.INSTANCE.getRunCoinsInstaSell() ? "instant sell" : "sell offer";
        String summary = !priced ? "Run value: loading Bazaar prices…"
                : "Run value " + BazaarPrices.INSTANCE.format(total) + "  ·  " + side + (HuntingConfig.INSTANCE.getSubtractTax() ? ", after tax" : "");
        MenuFont.text(g, summary, left + 86, footer + 7, .78, MenuTheme.MUTED);
    }
}

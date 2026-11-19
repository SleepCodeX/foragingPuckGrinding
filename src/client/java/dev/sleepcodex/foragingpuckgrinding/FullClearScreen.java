package dev.sleepcodex.foragingpuckgrinding;

import net.johnceo.sparklingmutuals.safari.SafariBiome;
import net.johnceo.sparklingmutuals.safari.SafariRoster;
import net.johnceo.sparklingmutuals.safari.SafariTracking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/** Inspect any area on the boat, in another biome or after leaving the run. */
final class FullClearScreen extends TrackerPage {
    private SafariBiome area;

    FullClearScreen(Screen parent) {
        super("Remaining critters", parent);
        var current = SafariTracking.INSTANCE.getDisplayBiome();
        area = current == null ? SafariBiome.FOREST : current;
    }

    @Override int preferredHeight() { return 292; }
    @Override String subtitle() {
        var run = SafariTracking.INSTANCE.getRun();
        if (run == null) return "Before entry  ·  Expected spawns by area";
        String state = run.getEndedAt() == null ? "Current run" : "Last run";
        return state + "  ·  " + FullClearProgress.coverage(run, SafariRoster.INSTANCE.getAll())
                + " / " + SafariRoster.INSTANCE.getAll().size() + " species caught";
    }

    @Override void layout() {
        int x = left + 15;
        int tabWidth = (w - 42) / 4;
        for (var tab : SafariBiome.values()) {
            button(tab.getLabel(), x, top + 52, tabWidth, 20, tab == area ? Tone.PRIMARY : Tone.NORMAL,
                    () -> { area = tab; refresh(); });
            x += tabWidth + 4;
        }
        button("Back", left + 15, footer, 61, 23, Tone.NORMAL, this::onClose);
    }

    @Override void content(GuiGraphicsExtractor g, MenuCanvas c, int mx, int my, float delta) {
        var run = SafariTracking.INSTANCE.getRun();
        var rows = FullClearProgress.remaining(run, area);
        int x = left + 18, right = left + w - 18, y = top + 79;
        double scale = Math.min(.86, Math.max(.58, (footer - y - 20) / 122.0));
        int step = Math.max(9, Math.min(16, (footer - y - 16) / Math.max(1, rows.size())));
        MenuFont.text(g, "Critter", x, y, .78, MenuTheme.MUTED);
        MenuFont.text(g, "Caught", left + w * .52, y, .78, MenuTheme.MUTED);
        MenuFont.text(g, "Remaining", right - MenuFont.width("Remaining") * .78, y, .78, MenuTheme.MUTED);
        y += 14;
        if (rows.isEmpty()) MenuFont.text(g, "Area cleared", x, y, .9, MenuTheme.TEXT);
        for (var item : rows) {
            MenuFont.text(g, item.species().getName(), x, y, scale, MenuTheme.TEXT);
            MenuFont.text(g, Integer.toString(item.caught()), left + w * .52, y, scale,
                    item.caught() > 0 ? MenuTheme.TEXT : MenuTheme.MUTED);
            MenuFont.text(g, item.amount(), right - MenuFont.width(item.amount()) * scale, y, scale, MenuTheme.ACCENT);
            y += step;
        }
    }
}

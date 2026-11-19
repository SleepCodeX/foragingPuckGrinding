package dev.sleepcodex.foragingpuckgrinding;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.UUID;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

final class CustomLibraryScreen extends TrackerPage {
    private final TracerConfig config;
    private int page, perPage, pages;
    private UUID pendingRemoval;

    CustomLibraryScreen(Screen parent, TracerConfig config) { super("Custom trackers", parent);
        this.config = config; }
    @Override int preferredWidth() { return 620; }
    @Override int preferredHeight() { return Math.clamp(148 + config.custom().all.size() * 40, 230, 380); }
    @Override String subtitle() { return status.isEmpty() ? "Your nametag and mob type rules" : status; }

    @Override void layout() {
        perPage = Math.max(1, (footer - (top + 55) - 21) / 40);
        var rules = config.custom().all;
        pages = Math.max(1, Math.ceilDiv(rules.size(), perPage));
        page = Math.clamp(page, 0, pages - 1);
        for (int i = 0; i < perPage && page * perPage + i < rules.size(); i++) {
            var rule = rules.get(page * perPage + i);
            int y = top + 54 + i * 40;
            button(rule.enabled() ? "On" : "Off", left + w - 172, y + 7, 38, 21,
                    Tone.NORMAL, () -> toggle(rule));
            button("Edit", left + w - 129, y + 7, 44, 21, Tone.NORMAL, () -> edit(rule));
            button(rule.id().equals(pendingRemoval) ? "Confirm" : "Remove", left + w - 80, y + 7, 59, 21,
                    Tone.DANGER, () -> remove(rule));
        }
        if (pages > 1) {
            button("Prev", left + w - 98, footer - 22, 36, 17, Tone.NORMAL, () -> move(-1)).active = page > 0;
            button("Next", left + w - 57, footer - 22, 38, 17, Tone.NORMAL, () -> move(1)).active = page + 1 < pages;
        }
        button("Back", left + 15, footer, 65, 23, Tone.NORMAL, this::onClose);
        button("Add tracker", left + w - 112, footer, 97, 23, Tone.PRIMARY, () -> edit(null));
    }

    private void remove(CustomTracker rule) {
        if (!rule.id().equals(pendingRemoval)) {
            pendingRemoval = rule.id();
            refresh();
            return;
        }
        try { config.removeCustom(rule.id()); pendingRemoval = null; status = ""; refresh(); }
        catch (IOException e) { saveFailed(e); }
    }
    private void toggle(CustomTracker rule) {
        try { config.putCustom(rule.toggled()); status = ""; refresh(); }
        catch (IOException e) { saveFailed(e); }
    }
    private void edit(CustomTracker rule) {
        pendingRemoval = null;
        Minecraft.getInstance().gui.setScreen(new CustomEditorScreen(this, config, rule));
    }
    void reveal(CustomTracker rule) {
        int index = config.custom().all.indexOf(rule);
        if (index >= 0) page = index / Math.max(1, perPage);
    }

    @Override void content(GuiGraphicsExtractor g, MenuCanvas c, int mx, int my, float delta) {
        var rules = config.custom().all;
        if (rules.isEmpty()) {
            int mid = (top + 45 + footer) / 2;
            MenuFont.text(g, "Add your first tracker", left + 18, mid - 12, 1.08, MenuTheme.TEXT);
            MenuFont.text(g, "Try Revenant Sycophant or wither_skeleton.", left + 18, mid + 6, .84, MenuTheme.MUTED);
        }
        for (int i = 0; i < perPage && page * perPage + i < rules.size(); i++) {
            var rule = rules.get(page * perPage + i);
            int y = top + 54 + i * 40;
            c.round(left + 15, y, w - 30, 35, 6, MenuTheme.CARD);
            c.round(left + 22, y + 9, 3, 17, 1, rule.enabled() ? rule.line().argb() : MenuTheme.BORDER);
            MenuFont.text(g, MenuFont.ellipsis(rule.label(), w - 220), left + 32, y + 6, 1, MenuTheme.TEXT);
            MenuFont.text(g, rule.mode() == CustomTracker.Mode.NAMETAG ? "Nametag contains" : "Mob type",
                    left + 32, y + 21, .73, MenuTheme.MUTED);
        }
        if (pages > 1) MenuFont.text(g, (page + 1) + " / " + pages, left + 18, footer - 18, .8, MenuTheme.MUTED);
    }

    private void move(int step) {
        int next = Math.clamp(page + step, 0, pages - 1);
        if (next != page) { page = next; pendingRemoval = null; refresh(); }
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (pages > 1 && vertical != 0) { move(vertical > 0 ? -1 : 1); return true; }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == 266 || event.key() == 267) { move(event.key() == 266 ? -1 : 1); return true; }
        return super.keyPressed(event);
    }
}

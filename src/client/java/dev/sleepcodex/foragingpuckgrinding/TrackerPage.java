package dev.sleepcodex.foragingpuckgrinding;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shared panel, focus treatment and controls for the native custom tracker screens. */
abstract class TrackerPage extends Screen {
    enum Tone { NORMAL, PRIMARY, DANGER }
    private record Action(Button button, Tone tone) { }
    private final List<Action> actions = new ArrayList<>();
    final TracerConfig config;
    final Screen parent;
    int left, top, w, h, footer;
    String status = "";
    private long entered;

    TrackerPage(String title, Screen parent, TracerConfig config) {
        super(Component.literal(title));
        this.parent = parent;
        this.config = config;
    }

    @Override protected final void init() {
        if (entered == 0) entered = System.nanoTime();
        actions.clear();
        w = Math.min(462, width - 20);
        h = Math.min(preferredHeight(), height - 16);
        left = (width - w) / 2;
        top = (height - h) / 2;
        footer = top + h - 34;
        layout();
    }

    void refresh() { clearWidgets(); setFocused(null); init(); }
    abstract int preferredHeight();
    abstract void layout();
    abstract String subtitle();
    abstract void content(GuiGraphicsExtractor g, MenuCanvas c, int mx, int my, float delta);

    Button button(String text, int x, int y, int width, int height, Tone tone, Runnable press) {
        Button result = addRenderableWidget(Button.builder(Component.literal(text), b -> press.run())
                .bounds(x, y, width, height).build());
        actions.add(new Action(result, tone));
        return result;
    }

    static boolean over(Button b, int x, int y) { return b.isFocused() || b.isMouseOver(x, y); }

    @Override public final void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0x9706090C);
        // A short surface fade keeps the controls in their final hit boxes from the first frame.
        double progress = Math.clamp((System.nanoTime() - entered) / 140_000_000.0, 0, 1);
        var c = new MenuCanvas(g, .82 + .18 * progress);
        c.round(left, top + 4, w, h, 13, 0x60000000);
        c.round(left, top, w, h, 13, MenuTheme.BORDER);
        c.round(left + 1, top + 1, w - 2, h - 2, 12, MenuTheme.PANEL);
        c.round(left + 16, top + 1, w - 32, 1, 0, 0x527B8794);
        MenuFont.text(g, getTitle().getString(), left + 16, top + 11, 1.32, MenuTheme.TEXT);
        MenuFont.text(g, MenuFont.ellipsis(subtitle(), (w - 32) / .8), left + 16, top + 30, .8, MenuTheme.MUTED);
        c.round(left + 15, top + 45, w - 30, 1, 0, MenuTheme.BORDER);
        content(g, c, mx, my, delta);
        for (Action action : actions) {
            Button b = action.button();
            if (!b.visible) continue;
            boolean hover = over(b, mx, my);
            int fill = action.tone() == Tone.PRIMARY ? 0xFFBFCEDB : MenuTheme.CONTROL;
            if (hover && b.active) fill = action.tone() == Tone.PRIMARY ? 0xFFDBE7EF : MenuTheme.CONTROL_HOVER;
            if (b.isFocused()) c.round(b.getX()-1, b.getY()-1, b.getWidth()+2, b.getHeight()+2, 6, MenuTheme.ACCENT);
            c.round(b.getX(), b.getY(), b.getWidth(), b.getHeight(), 5, b.active ? fill : MenuTheme.CARD);
            int ink = !b.active ? MenuTheme.MUTED : action.tone() == Tone.PRIMARY ? 0xFF15191D
                    : action.tone() == Tone.DANGER ? 0xFFFFA5A1 : MenuTheme.TEXT;
            String label = MenuFont.ellipsis(b.getMessage().getString(), (b.getWidth() - 6) / .86);
            MenuFont.text(g, label, b.getX() + (b.getWidth() - MenuFont.width(label) * .86) / 2,
                    b.getY() + (b.getHeight() - 9) / 2., .86, ink);
        }
    }

    void saveFailed(java.io.IOException failure) {
        status = "Could not save. Check the game log and retry.";
        HuntingDebug.LOG.warn("Could not write custom trackers", failure);
    }

    @Override public void onClose() { Minecraft.getInstance().gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}

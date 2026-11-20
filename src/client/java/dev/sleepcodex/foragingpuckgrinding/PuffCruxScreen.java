package dev.sleepcodex.foragingpuckgrinding;

import java.io.IOException;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

final class PuffCruxScreen extends TrackerPage {
    private final TracerConfig config;

    PuffCruxScreen(Screen parent, TracerConfig config) {
        super("Puff Crux", parent);
        this.config = config;
    }

    @Override int preferredWidth() { return 342; }
    @Override int preferredHeight() { return 186; }
    @Override String subtitle() { return "Rift Mountaintop"; }

    @Override void layout() {
        int x = left + w - 91;
        button(config.puffTimer() ? "Timer on" : "Timer off", x, top + 54, 75, 21, Tone.NORMAL,
                () -> save(!config.puffTimer(), config.puffReminder()));
        button(config.puffReminder() ? "Alert on" : "Alert off", x, top + 92, 75, 21, Tone.NORMAL,
                () -> save(config.puffTimer(), !config.puffReminder())).active = config.puffTimer();
        button("Back", left + w - 81, footer, 65, 22, Tone.NORMAL, this::onClose);
    }

    @Override void content(GuiGraphicsExtractor g, MenuCanvas c, int mx, int my, float delta) {
        row(g, 54, "Despawn timer", "Show a countdown above each spotted Puff.");
        row(g, 92, "Pop reminder", "Warn with about " + PuffTimers.WARNING_SECONDS + " seconds left.");
        if (!status.isEmpty()) MenuFont.text(g, status, left + 16, footer + 5, .75, MenuTheme.MUTED);
    }

    private void row(GuiGraphicsExtractor g, int offset, String name, String detail) {
        MenuFont.text(g, name, left + 16, top + offset, 1, MenuTheme.TEXT);
        MenuFont.text(g, MenuFont.ellipsis(detail, (w - 117) / .75), left + 16, top + offset + 14, .75, MenuTheme.MUTED);
    }

    private void save(boolean timer, boolean reminder) {
        try {
            config.setPuffOptions(timer, reminder);
            status = "";
        } catch (IOException e) {
            saveFailed(e);
        }
        refresh();
    }
}

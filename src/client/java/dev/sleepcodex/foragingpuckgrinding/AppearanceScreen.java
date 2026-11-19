package dev.sleepcodex.foragingpuckgrinding;

import java.io.IOException;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

final class AppearanceScreen extends TrackerPage {
    private final Button[] models = new Button[HivethiefStyle.values().length];
    AppearanceScreen(HuntingHomeScreen home, TracerConfig config) { super("Appearance", home, config); }

    @Override int preferredHeight() { return 235; }
    @Override String subtitle() { return status.isEmpty() ? "Choose a Hivethief to replay the opening." : status; }

    @Override void layout() {
        int card = (w - 42) / 3;
        for (var style : HivethiefStyle.values()) {
            int x = left + 15 + style.ordinal() * (card + 6);
            models[style.ordinal()] = addRenderableWidget(Button.builder(Component.literal(style.label), b -> choose(style))
                    .bounds(x, top + 56, card, footer - top - 68).build());
        }
        button("Back", left + 15, footer, 64, 23, Tone.NORMAL, this::onClose);
    }

    private void choose(HivethiefStyle style) {
        try {
            config.setStyle(style);
            ((HuntingHomeScreen) parent).replay();
        } catch (IOException e) { saveFailed(e); }
    }

    @Override void content(GuiGraphicsExtractor g, MenuCanvas c, int mx, int my, float delta) {
        for (var style : HivethiefStyle.values()) {
            Button b = models[style.ordinal()];
            boolean selected = style == config.style();
            int x = b.getX(), y = b.getY(), width = b.getWidth(), height = b.getHeight();
            c.round(x, y, width, height, 7, selected || over(b, mx, my) ? MenuTheme.ACCENT : MenuTheme.BORDER);
            c.round(x + 1, y + 1, width - 2, height - 2, 6, over(b, mx, my) ? MenuTheme.HOVER : MenuTheme.CARD);
            int size = Math.min(72, Math.min(width - 14, height - 34));
            c.hivethief(style, x + width / 2, y + (height - 22) / 2, size);
            MenuFont.text(g, style.label, x + (width - MenuFont.width(style.label)) / 2, b.getBottom() - 21, 1, MenuTheme.TEXT);
            if (selected) c.round(x + width / 2 - 6, b.getBottom() - 7, 12, 2, 1, MenuTheme.ACCENT);
        }
    }
}

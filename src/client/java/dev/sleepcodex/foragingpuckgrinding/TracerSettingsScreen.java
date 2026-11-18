package dev.sleepcodex.foragingpuckgrinding;

import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Two native, keyboard-accessible controls inside a small Trevor-inspired shell. */
public final class TracerSettingsScreen extends Screen {
    private static final long INTRO_NANOS = 650_000_000L;
    private final TracerConfig config;
    private final KeyMapping toggleKey;
    private final Consumer<Boolean> enable;
    private final Consumer<TracerTarget> select;
    private final long openedAt = System.nanoTime();
    private Button enabledButton, targetButton, doneButton;
    private int left, top, panelWidth, panelHeight, rowHeight;

    TracerSettingsScreen(TracerConfig config, KeyMapping toggleKey,
            Consumer<Boolean> enable, Consumer<TracerTarget> select) {
        super(Component.literal("Hunting Helper"));
        this.config = config;
        this.toggleKey = toggleKey;
        this.enable = enable;
        this.select = select;
    }

    @Override
    protected void init() {
        boolean compact = height < 280;
        panelWidth = Math.min(480, width - 24);
        panelHeight = compact ? 218 : 270;
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        rowHeight = compact ? 52 : 60;
        int contentWidth = panelWidth - 32;
        int firstRow = top + (compact ? 52 : 72);
        enabledButton = addRenderableWidget(Button.builder(enabledLabel(), button -> {
            enable.accept(!config.enabled());
            button.setMessage(enabledLabel());
        }).bounds(left + 16, firstRow, contentWidth, rowHeight).build());
        targetButton = addRenderableWidget(Button.builder(targetLabel(), button -> {
            select.accept(config.target().next());
            button.setMessage(targetLabel());
        }).bounds(left + 16, firstRow + rowHeight + (compact ? 12 : 14), contentWidth, rowHeight).build());
        doneButton = addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left + panelWidth - 98, top + panelHeight - 36, 82, 24).build());
    }

    private Component enabledLabel() {
        return Component.literal("Tracers: " + (config.enabled() ? "ON" : "OFF"));
    }

    private Component targetLabel() {
        return Component.literal("Targets: " + config.target().label());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // The short intro changes only visuals; Escape, focus and buttons remain responsive.
        double progress = Math.clamp((System.nanoTime() - openedAt) / (double) INTRO_NANOS, 0, 1);
        double contentOpacity = Math.clamp((progress - 0.25) / 0.6, 0, 1);
        var shell = new MenuCanvas(graphics, 1);
        graphics.fill(0, 0, width, height, 0x8C0A0F17);
        shell.round(left, top + 5, panelWidth, panelHeight, 13, 0x70000000);
        shell.round(left, top, panelWidth, panelHeight, 13, 0xFF3A4354);
        shell.round(left + 1, top + 1, panelWidth - 2, panelHeight - 2, 12, 0xFF111820);

        MenuFont.opacity(contentOpacity);
        try {
            var canvas = new MenuCanvas(graphics, contentOpacity);
            canvas.axe(left + 17, top + 12, 28);
            MenuFont.text(graphics, "Hunting Helper", left + 53, top + 13, 1.45, 0xFFF3EBDD);
            MenuFont.text(graphics, "Simple tracers. Your next hunt.", left + 54, top + 30, 0.8, 0xFFA8B4C8);
            row(graphics, enabledButton, "Tracers", "Lines to your selected targets, through blocks.", mouseX, mouseY, canvas);
            row(graphics, targetButton, "Targets", "Vex, Blue Jay, or both at once.", mouseX, mouseY, canvas);
            boolean hover = hovered(doneButton, mouseX, mouseY) || doneButton.isFocused();
            canvas.round(doneButton.getX(), doneButton.getY(), doneButton.getWidth(), doneButton.getHeight(), 7,
                    hover ? 0xFF8971C3 : 0xFF3A3553);
            MenuFont.text(graphics, "Done", doneButton.getX() + 27, doneButton.getY() + 8, 0.95, 0xFFF2E8FF);
            String shortcut = toggleKey.getTranslatedKeyMessage().getString() + " toggles tracers";
            MenuFont.text(graphics, MenuFont.ellipsis(shortcut, panelWidth - 140), left + 18, top + panelHeight - 26,
                    0.82, 0xFFA8B4C8);
        } finally {
            MenuFont.opacity(1);
        }
        double splashOpacity = 1 - Math.clamp((progress - 0.35) / 0.4, 0, 1);
        if (splashOpacity > 0) {
            new MenuCanvas(graphics, splashOpacity).axe(width / 2 - 24, height / 2 - 47, 48);
            MenuFont.opacity(splashOpacity);
            try {
                String brand = "Hunting Helper";
                MenuFont.text(graphics, brand, (width - MenuFont.width(brand) * 1.8) / 2,
                        height / 2 + 9, 1.8, 0xFFE4D3FF);
            } finally { MenuFont.opacity(1); }
        }
    }

    private void row(GuiGraphicsExtractor graphics, Button button, String label, String help,
            int mouseX, int mouseY, MenuCanvas canvas) {
        boolean hover = hovered(button, mouseX, mouseY) || button.isFocused();
        int x = button.getX(), y = button.getY(), w = button.getWidth();
        canvas.round(x, y + 2, w, rowHeight, 9, 0x50000000);
        canvas.round(x, y, w, rowHeight, 9, hover ? 0xFF9A82C6 : 0xFF34414E);
        canvas.round(x + 1, y + 1, w - 2, rowHeight - 2, 8, hover ? 0xFF283242 : 0xFF1D2632);
        MenuFont.text(graphics, label, x + 13, y + 10, 1.05, 0xFFF3EBDD);
        double helpScale = Math.min(0.8, (w - 26) / Math.max(1, MenuFont.width(help)));
        MenuFont.text(graphics, help, x + 13, y + 31, helpScale, 0xFFA8B4C8);
        if (button == enabledButton) {
            boolean on = config.enabled();
            MenuFont.text(graphics, on ? "On" : "Off", x + w - 69, y + 12, 0.8, 0xFFDCCCF6);
            canvas.round(x + w - 44, y + 9, 31, 16, 8, on ? 0xFF8B6DE2 : 0xFF4C5668);
            canvas.round(x + w - (on ? 27 : 42), y + 11, 12, 12, 6, on ? 0xFFF8F3FF : 0xFFA8B0C2);
        } else {
            String value = config.target().label() + "  >";
            double valueWidth = MenuFont.width(value) * 0.95;
            canvas.round(x + w - (int) valueWidth - 26, y + 7, (int) valueWidth + 15, 20, 6, 0xFF403555);
            MenuFont.text(graphics, value, x + w - valueWidth - 18, y + 12, 0.95, 0xFFE9D8FF);
        }
    }

    private static boolean hovered(Button button, int x, int y) {
        return x >= button.getX() && x < button.getRight() && y >= button.getY() && y < button.getBottom();
    }

    @Override public boolean isPauseScreen() { return false; }
}

package dev.sleepcodex.foragingpuckgrinding;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Independently selectable mob trackers with native mouse/keyboard controls. */
public final class TracerSettingsScreen extends Screen {
    private static final long INTRO_NANOS = 650_000_000L;
    private final TracerConfig config;
    private final KeyMapping toggleKey;
    private final Consumer<Boolean> enable;
    private final Consumer<Set<TracerTarget>> select;
    private final long openedAt = System.nanoTime();
    private final Button[] mobButtons = new Button[TracerTarget.values().length];
    private Button enabledButton, allButton, noneButton, doneButton;
    private int left, top, panelWidth, panelHeight, rowHeight;

    TracerSettingsScreen(TracerConfig config, KeyMapping toggleKey, Consumer<Boolean> enable,
            Consumer<Set<TracerTarget>> select) {
        super(Component.literal("Hunting Helper"));
        this.config = config; this.toggleKey = toggleKey; this.enable = enable; this.select = select;
    }
    @Override protected void init() {
        panelWidth = Math.min(480, width - 24); panelHeight = Math.min(358, height - 16);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        int first = top + (panelHeight < 280 ? 46 : 64), footer = top + panelHeight - 32;
        int gap = panelHeight < 280 ? 5 : 8;
        rowHeight = Math.min(52, (footer - first - 12 - gap * 3) / 4);
        enabledButton = addRenderableWidget(Button.builder(masterLabel(), b -> {
            enable.accept(!config.enabled()); refreshLabels();
        }).bounds(left + 16, first, panelWidth - 32, rowHeight).build());
        for (TracerTarget target : TracerTarget.values()) {
            int index = target.ordinal();
            mobButtons[index] = addRenderableWidget(Button.builder(mobLabel(target), b -> {
                var selected = config.selectedTargets();
                if (!selected.remove(target)) selected.add(target);
                select.accept(selected); refreshLabels();
            }).bounds(left + 16, first + (index + 1) * (rowHeight + gap), panelWidth - 32, rowHeight).build());
        }
        allButton = addRenderableWidget(Button.builder(Component.literal("Select all"), b -> {
            select.accept(EnumSet.allOf(TracerTarget.class)); refreshLabels();
        }).bounds(left + 16, footer, 83, 23).build());
        noneButton = addRenderableWidget(Button.builder(Component.literal("Clear all"), b -> {
            select.accept(EnumSet.noneOf(TracerTarget.class)); refreshLabels();
        }).bounds(left + 105, footer, 83, 23).build());
        doneButton = addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(left + panelWidth - 88, footer, 72, 23).build());
    }
    private Component masterLabel() { return Component.literal("Tracers: " + (config.enabled() ? "ON" : "OFF")); }
    private Component mobLabel(TracerTarget target) { return Component.literal(target.label() + ": " + (config.isSelected(target) ? "ON" : "OFF")); }
    private void refreshLabels() {
        enabledButton.setMessage(masterLabel());
        for (TracerTarget target : TracerTarget.values()) mobButtons[target.ordinal()].setMessage(mobLabel(target));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        double progress = Math.clamp((System.nanoTime() - openedAt) / (double) INTRO_NANOS, 0, 1);
        double opacity = Math.clamp((progress - .25) / .6, 0, 1);
        var shell = new MenuCanvas(g, 1);
        g.fill(0, 0, width, height, 0x8C0A0F17);
        shell.round(left, top + 5, panelWidth, panelHeight, 13, 0x70000000);
        shell.round(left, top, panelWidth, panelHeight, 13, 0xFF3A4354);
        shell.round(left + 1, top + 1, panelWidth - 2, panelHeight - 2, 12, 0xFF111820);
        MenuFont.opacity(opacity);
        try {
            var canvas = new MenuCanvas(g, opacity);
            canvas.axe(left + 16, top + 10, 28);
            double titleScale = Math.min(1.45, (panelWidth - 78) / MenuFont.width("Hunting Helper"));
            MenuFont.text(g, "Hunting Helper", left + 53, top + 12, titleScale, 0xFFF3EBDD);
            MenuFont.text(g, toggleKey.getTranslatedKeyMessage().getString() + " toggles all selected trackers", left + 54, top + 30, .72, 0xFFA8B4C8);
            row(g, enabledButton, "Tracers", "Master switch for lines and boxes", config.enabled(), 0xFF8B6DE2, mx, my, canvas);
            for (TracerTarget target : TracerTarget.values()) {
                String help = switch (target) {
                    case VEX -> "Vex mobs — purple";
                    case BLUE_JAY -> "Blue parrots in Torrhus — cyan";
                    case HIVE_THIEF -> "Ghasts in Torrhus — gold";
                };
                row(g, mobButtons[target.ordinal()], target.label(), help, config.isSelected(target), target.color(), mx, my, canvas);
            }
            for (Button button : new Button[]{allButton, noneButton, doneButton}) {
                canvas.round(button.getX(), button.getY(), button.getWidth(), button.getHeight(), 6,
                        hovered(button, mx, my) || button.isFocused() ? 0xFF8971C3 : 0xFF3A3553);
                String label = button.getMessage().getString();
                MenuFont.text(g, label, button.getX() + (button.getWidth() - MenuFont.width(label) * .85) / 2,
                        button.getY() + 7, .85, 0xFFF2E8FF);
            }
        } finally { MenuFont.opacity(1); }
        double splash = 1 - Math.clamp((progress - .35) / .4, 0, 1);
        if (splash > 0) {
            new MenuCanvas(g, splash).axe(width / 2 - 24, height / 2 - 47, 48);
            MenuFont.opacity(splash);
            try { MenuFont.text(g, "Hunting Helper", (width - MenuFont.width("Hunting Helper") * 1.8) / 2, height / 2 + 9, 1.8, 0xFFE4D3FF); }
            finally { MenuFont.opacity(1); }
        }
    }
    private void row(GuiGraphicsExtractor g, Button button, String label, String help, boolean on, int color,
            int mx, int my, MenuCanvas c) {
        int x = button.getX(), y = button.getY(), w = button.getWidth();
        boolean hover = hovered(button, mx, my) || button.isFocused();
        c.round(x, y, w, rowHeight, 8, hover ? 0xFF9A82C6 : 0xFF34414E);
        c.round(x + 1, y + 1, w - 2, rowHeight - 2, 7, hover ? 0xFF283242 : 0xFF1D2632);
        c.round(x + 9, y + 10, 4, 10, 2, color);
        MenuFont.text(g, label, x + 20, y + 9, 1, 0xFFF3EBDD);
        if (rowHeight >= 42) MenuFont.text(g, help, x + 20, y + 28, Math.min(.75, (w - 40) / Math.max(1, MenuFont.width(help))), 0xFFA8B4C8);
        MenuFont.text(g, on ? "On" : "Off", x + w - 69, y + 11, .8, 0xFFDCCCF6);
        c.round(x + w - 44, y + 8, 31, 16, 8, on ? color : 0xFF4C5668);
        c.round(x + w - (on ? 27 : 42), y + 10, 12, 12, 6, on ? 0xFFF8F3FF : 0xFFA8B0C2);
    }
    private static boolean hovered(Button b, int x, int y) { return x >= b.getX() && x < b.getRight() && y >= b.getY() && y < b.getBottom(); }
    @Override public boolean isPauseScreen() { return false; }
}

package dev.sleepcodex.foragingpuckgrinding;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Responsive tracker cards with native focus/input and a short, nonblocking fade. */
public final class TracerSettingsScreen extends Screen {
    private static final TracerTarget[] TYPES = TracerTarget.values();
    private static final long FADE_NANOS = 160_000_000L;
    private final TracerConfig config;
    private final KeyMapping toggleKey;
    private final Consumer<Boolean> enable;
    private final Consumer<Set<TracerTarget>> select;
    private final long openedAt = System.nanoTime();
    private final Button[] mobButtons = new Button[TYPES.length];
    private Button enabledButton, allButton, noneButton, doneButton;
    private int left, top, panelWidth, panelHeight, shownChunks = -1;
    private boolean cards, displayedEnabled;
    private double titleScale;
    private String shortcut, selectionLabel, rangeLabel;

    TracerSettingsScreen(TracerConfig config, KeyMapping toggleKey, Consumer<Boolean> enable,
            Consumer<Set<TracerTarget>> select) {
        super(Component.literal("Hunting Helper"));
        this.config = config;
        this.toggleKey = toggleKey;
        this.enable = enable;
        this.select = select;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(570, width - 24);
        panelHeight = Math.min(328, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        cards = panelWidth >= 430 && panelHeight >= 300;
        int footer = top + panelHeight - 34;
        int bodyY = top + (cards ? 78 : 64);
        int bodyHeight = footer - 12 - bodyY;
        int gap = cards ? 12 : 6;
        int cardWidth = cards ? (panelWidth - 32 - gap * 2) / 3 : panelWidth - 32;
        int cardHeight = cards ? bodyHeight : (bodyHeight - gap * 2) / 3;
        titleScale = Math.min(1.45, (panelWidth - 180) / MenuFont.width("Hunting Helper"));
        shortcut = MenuFont.ellipsis(toggleKey.getTranslatedKeyMessage().getString() + " toggles selected", panelWidth - 185);

        enabledButton = addRenderableWidget(Button.builder(masterLabel(), button -> {
            enable.accept(!config.enabled());
            refreshLabels();
        }).bounds(left + panelWidth - 112, top + 15, 96, 27).build());
        for (TracerTarget target : TYPES) {
            int index = target.ordinal();
            mobButtons[index] = addRenderableWidget(Button.builder(mobLabel(target), button -> {
                var selected = config.selectedTargets();
                if (!selected.remove(target)) selected.add(target);
                select.accept(selected);
                refreshLabels();
            }).bounds(left + 16 + (cards ? index * (cardWidth + gap) : 0),
                    bodyY + (cards ? 0 : index * (cardHeight + gap)), cardWidth, cardHeight).build());
        }
        allButton = addRenderableWidget(Button.builder(Component.literal("Select all"), button -> {
            select.accept(EnumSet.allOf(TracerTarget.class));
            refreshLabels();
        }).bounds(left + 16, footer, 83, 24).build());
        noneButton = addRenderableWidget(Button.builder(Component.literal("Clear all"), button -> {
            select.accept(EnumSet.noneOf(TracerTarget.class));
            refreshLabels();
        }).bounds(left + 105, footer, 83, 24).build());
        doneButton = addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left + panelWidth - 88, footer, 72, 24).build());
        refreshLabels();
    }

    private Component masterLabel() {
        return Component.literal("Tracers: " + (config.enabled() ? "ON" : "OFF"));
    }

    private Component mobLabel(TracerTarget target) {
        return Component.literal(target.label() + ": " + (config.isSelected(target) ? "ON" : "OFF"));
    }

    private void refreshLabels() {
        displayedEnabled = config.enabled();
        enabledButton.setMessage(masterLabel());
        int count = 0;
        for (TracerTarget target : TYPES) {
            mobButtons[target.ordinal()].setMessage(mobLabel(target));
            if (config.isSelected(target)) count++;
        }
        selectionLabel = count + " selected";
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (displayedEnabled != config.enabled()) refreshLabels();
        double remaining = 1 - Math.clamp((System.nanoTime() - openedAt) / (double) FADE_NANOS, 0, 1);
        double opacity = 1 - remaining * remaining * remaining;
        var canvas = new MenuCanvas(graphics, opacity);
        graphics.fill(0, 0, width, height, ((int) (opacity * 140) << 24) | 0x080D15);
        canvas.round(left, top + 5, panelWidth, panelHeight, 14, 0x70000000);
        canvas.round(left, top, panelWidth, panelHeight, 14, 0xFF3C4659);
        canvas.round(left + 1, top + 1, panelWidth - 2, panelHeight - 2, 13, 0xFF111923);
        MenuFont.opacity(opacity);
        try {
            canvas.axe(left + 16, top + 12, 30);
            MenuFont.text(graphics, "Hunting Helper", left + 55, top + 16, titleScale, 0xFFF3EBDD);
            MenuFont.text(graphics, shortcut, left + 56, top + 36, .7, 0xFF9FAEC3);
            canvas.round(left + 16, top + (cards ? 63 : 54), panelWidth - 32, 1, 0, 0xFF2D394B);
            smallButton(graphics, canvas, enabledButton, "Tracers", mouseX, mouseY);
            toggle(canvas, enabledButton.getRight() - 37, enabledButton.getY() + 6, config.enabled(), 0xFFA38BE7);
            center(graphics, selectionLabel, enabledButton.getX() + 48, top + 46, .66, 0xFF9FAEC3);
            for (TracerTarget target : TYPES) {
                drawTarget(graphics, canvas, target, mobButtons[target.ordinal()], mouseX, mouseY);
            }
            smallButton(graphics, canvas, allButton, "Select all", mouseX, mouseY);
            smallButton(graphics, canvas, noneButton, "Clear all", mouseX, mouseY);
            smallButton(graphics, canvas, doneButton, "Done", mouseX, mouseY);
            if (cards) {
                int chunks = Minecraft.getInstance().options.getEffectiveRenderDistance();
                if (shownChunks != chunks) {
                    shownChunks = chunks;
                    rangeLabel = "Full " + chunks + "-chunk range";
                }
                center(graphics, rangeLabel, left + panelWidth / 2, allButton.getY() + 8, .7, 0xFF9FAEC3);
            }
        } finally {
            MenuFont.opacity(1);
        }
    }

    private void drawTarget(GuiGraphicsExtractor graphics, MenuCanvas canvas, TracerTarget target,
            Button button, int mouseX, int mouseY) {
        int x = button.getX(), y = button.getY(), w = button.getWidth(), h = button.getHeight();
        boolean selected = config.isSelected(target);
        boolean hover = hovered(button, mouseX, mouseY) || button.isFocused();
        canvas.round(x, y + 2, w, h, 10, 0x40000000);
        canvas.round(x, y, w, h, 10, selected ? target.color() : hover ? 0xFF7E8DA8 : 0xFF344255);
        canvas.round(x + 1, y + 1, w - 2, h - 2, 9, hover ? 0xFF253244 : 0xFF1A2533);
        if (cards) {
            canvas.round(x + 13, y + 13, 27, 3, 1, target.color());
            toggle(canvas, x + w - 42, y + 10, selected, target.color());
            canvas.round(x + w / 2 - 32, y + 32, 64, 64, 21, (target.color() & 0xFFFFFF) | 0x22000000);
            MobSymbols.draw(graphics, canvas, target, x + w / 2, y + 63, 49);
            String hint = switch (target) {
                case VEX -> "Vex mobs";
                case BLUE_JAY -> "Blue parrots";
                case HIVE_THIEF -> "Ghasts";
            };
            center(graphics, hint, x + w / 2, y + 111, .75, 0xFF9FAEC3);
            canvas.round(x + 1, y + h - 37, w - 2, 36, 8,
                    selected ? (target.color() & 0xFFFFFF) | 0x48000000 : 0xFF273348);
            center(graphics, target.label(), x + w / 2, y + h - 24, 1.1, 0xFFF3EBDD);
        } else {
            MobSymbols.draw(graphics, canvas, target, x + 22, y + h / 2, Math.min(27, h - 10));
            MenuFont.text(graphics, target.label(), x + 44, y + (h - 9) / 2., 1, 0xFFF3EBDD);
            toggle(canvas, x + w - 44, y + (h - 15) / 2, selected, target.color());
        }
    }

    private static void smallButton(GuiGraphicsExtractor graphics, MenuCanvas canvas, Button button,
            String label, int mouseX, int mouseY) {
        boolean hover = hovered(button, mouseX, mouseY) || button.isFocused();
        canvas.round(button.getX(), button.getY(), button.getWidth(), button.getHeight(), 6,
                hover ? 0xFF7864AB : 0xFF303C53);
        if (label.equals("Tracers")) {
            MenuFont.text(graphics, label, button.getX() + 8, button.getY() + 9, .76, 0xFFF2E8FF);
        } else {
            center(graphics, label, button.getX() + button.getWidth() / 2, button.getY() + 8, .85, 0xFFF2E8FF);
        }
    }

    private static void toggle(MenuCanvas canvas, int x, int y, boolean on, int color) {
        canvas.round(x, y, 29, 15, 7, on ? color : 0xFF4A5668);
        canvas.round(x + (on ? 16 : 2), y + 2, 11, 11, 5, on ? 0xFFF8F6FF : 0xFFA5AEBD);
    }

    private static void center(GuiGraphicsExtractor graphics, String text, int x, double y, double scale, int color) {
        MenuFont.text(graphics, text, x - MenuFont.width(text) * scale / 2, y, scale, color);
    }

    private static boolean hovered(Button button, int x, int y) {
        return x >= button.getX() && x < button.getRight() && y >= button.getY() && y < button.getBottom();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}

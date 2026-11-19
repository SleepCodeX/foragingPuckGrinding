package dev.sleepcodex.foragingpuckgrinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Roomy tracker cards, independently colored overlays, and a nonblocking Hivethief crossfade. */
public final class TracerSettingsScreen extends Screen {
    @FunctionalInterface interface ColorChange { void apply(TracerTarget target, boolean glow, OverlayColor color); }
    private static final TracerTarget[] TYPES = TracerTarget.values();
    private static final OverlayColor[] GLOW_COLORS = OverlayColor.values();
    private static final OverlayColor[] LINE_COLORS = Arrays.stream(GLOW_COLORS).filter(OverlayColor::isSolid).toArray(OverlayColor[]::new);
    private static final int POPUP_ROWS = 5, POPUP_ROW_HEIGHT = 22;
    private static final int COLUMNS = 3, CARD_ROWS = Math.ceilDiv(TYPES.length + 1, COLUMNS);
    private final TracerConfig config;
    private final KeyMapping toggleKey;
    private final Consumer<Boolean> enable;
    private final Consumer<Set<TracerTarget>> select;
    private final ColorChange colorChange;
    private final LongSupplier clock;
    private long openedAt;
    HuntingHomeScreen home;
    void settle() { openedAt = clock.getAsLong() - MenuOpening.DURATION_NANOS; }
    private final Button[] mobButtons = new Button[TYPES.length], lineButtons = new Button[TYPES.length], glowButtons = new Button[TYPES.length];
    private final Button[] popupButtons = new Button[POPUP_ROWS];
    private final String[] lineLabels = new String[TYPES.length], glowLabels = new String[TYPES.length];
    private final List<Button> bodyButtons = new ArrayList<>(), navigation = new ArrayList<>();
    private final IdentityHashMap<Button, Integer> baseY = new IdentityHashMap<>();
    private Button enabledButton, allButton, noneButton, doneButton, homeButton, popupAnchor;
    private int left, top, panelWidth, panelHeight, bodyTop, bodyBottom, contentHeight, scroll, shownChunks = -1;
    private int cardHeight;
    private Button customButton;
    private int popupX, popupY, popupWidth, popupScroll, popupIndex;
    private boolean cards, displayedEnabled, popupGlow, draggingScrollbar;
    private TracerTarget popupTarget;
    private double titleScale;
    private String shortcut, selectionLabel, rangeLabel;

    TracerSettingsScreen(TracerConfig config, KeyMapping toggleKey, Consumer<Boolean> enable,
            Consumer<Set<TracerTarget>> select, ColorChange colorChange) {
        this(config, toggleKey, enable, select, colorChange, System::nanoTime);
    }

    TracerSettingsScreen(TracerConfig config, KeyMapping toggleKey, Consumer<Boolean> enable,
            Consumer<Set<TracerTarget>> select, ColorChange colorChange, LongSupplier clock) {
        super(Component.literal("Hunting Helper"));
        this.config = config;
        this.toggleKey = toggleKey;
        this.enable = enable;
        this.select = select;
        this.colorChange = colorChange;
        this.clock = clock;
        openedAt = clock.getAsLong();
    }

    // A separate screen/clock lets native captures inspect intermediate frames deterministically.
    TracerSettingsScreen withClock(LongSupplier source) {
        return new TracerSettingsScreen(config, toggleKey, enable, select, colorChange, source);
    }

    @Override protected void init() {
        bodyButtons.clear(); navigation.clear(); baseY.clear(); popupTarget = null; draggingScrollbar = false;
        panelWidth = Math.min(600, width - 24);
        // Fit all five cards at the usual 640 x 360 GUI size. Smaller windows retain
        // the same controls in a scrollable single column, including keyboard reveal.
        cards = panelWidth >= 430 && height >= 320;
        panelHeight = Math.min(cards ? 68 + CARD_ROWS * 152 + (CARD_ROWS - 1) * 14 + 12 + 46 : 328, height - 20);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int footer = top + panelHeight - 34;
        bodyTop = top + (cards ? 68 : 64);
        bodyBottom = footer - 12;
        int gap = cards ? 14 : 12;
        int cardWidth = cards ? (panelWidth - 40 - gap * (COLUMNS - 1)) / COLUMNS : panelWidth - 40;
        cardHeight = cards ? Math.clamp((bodyBottom - bodyTop - 12 - gap * (CARD_ROWS - 1)) / CARD_ROWS, 96, 152) : 104;
        contentHeight = (cards ? CARD_ROWS : TYPES.length + 1) * (cardHeight + gap) - gap + 12;
        titleScale = Math.min(1.45, (panelWidth - 180) / MenuFont.width("Hunting Helper"));
        shortcut = MenuFont.ellipsis(toggleKey.getTranslatedKeyMessage().getString() + " toggles selected", panelWidth - 185);
        enabledButton = addRenderableWidget(Button.builder(masterLabel(), b -> {
            enable.accept(!config.enabled()); refreshLabels();
        }).bounds(left + panelWidth - 112, top + 15, 96, 27).build());
        navigation.add(enabledButton);
        for (TracerTarget target : TYPES) {
            int index = target.ordinal(), row = cards ? index / COLUMNS : index, column = cards ? index % COLUMNS : 0;
            // A shorter last row of cards is centered under the full rows.
            int inRow = cards ? Math.min(COLUMNS, TYPES.length + 1 - row * COLUMNS) : 1;
            int x = left + 20 + (cards ? (COLUMNS - inRow) * (cardWidth + gap) / 2 + column * (cardWidth + gap) : 0);
            int y = bodyTop + 6 + row * (cardHeight + gap);
            mobButtons[index] = bodyButton(mobLabel(target), x + 10, y + 8, cardWidth - 20, cards ? cardHeight - 58 : 34, b -> {
                var selected = config.selectedTargets();
                if (!selected.remove(target)) selected.add(target);
                select.accept(selected); refreshLabels();
            });
            lineButtons[index] = bodyButton(Component.empty(), x + 10, y + (cards ? cardHeight - 48 : 51), cardWidth - 20, 19,
                    b -> openPopup(target, false, b));
            glowButtons[index] = bodyButton(Component.empty(), x + 10, y + (cards ? cardHeight - 24 : 78), cardWidth - 20, 19,
                    b -> openPopup(target, true, b));
        }
        int customX = left + 20 + (cards ? (TYPES.length % COLUMNS) * (cardWidth + gap) : 0);
        int customY = bodyTop + 6 + (cards ? TYPES.length / COLUMNS : TYPES.length) * (cardHeight + gap);
        customButton = bodyButton(Component.literal("Custom trackers"), customX, customY, cardWidth, cardHeight,
                b -> openCustom());
        boolean narrowFooter = panelWidth < 470;
        int presetWidth = narrowFooter ? 58 : 81;
        int doneWidth = narrowFooter ? 46 : 68;
        int homeWidth = narrowFooter ? 72 : 99;
        homeButton = addRenderableWidget(Button.builder(Component.literal("Home"), b -> onClose())
                .bounds(left + panelWidth - 16 - doneWidth - 6 - homeWidth, footer, homeWidth, 24).build());
        allButton = addRenderableWidget(Button.builder(Component.literal("All presets"), b -> {
            select.accept(EnumSet.allOf(TracerTarget.class)); refreshLabels();
        }).bounds(left + 16, footer, presetWidth, 24).build());
        noneButton = addRenderableWidget(Button.builder(Component.literal("Clear presets"), b -> {
            select.accept(EnumSet.noneOf(TracerTarget.class)); refreshLabels();
        }).bounds(left + 22 + presetWidth, footer, presetWidth, 24).build());
        doneButton = addRenderableWidget(Button.builder(Component.literal("Done"), b -> Minecraft.getInstance().gui.setScreen(null))
                .bounds(left + panelWidth - 16 - doneWidth, footer, doneWidth, 24).build());
        navigation.add(allButton); navigation.add(noneButton); navigation.add(homeButton); navigation.add(doneButton);
        for (int row = 0; row < POPUP_ROWS; row++) {
            final int index = row;
            popupButtons[row] = addRenderableWidget(Button.builder(Component.empty(), b -> choose(popupScroll + index))
                    .bounds(0, 0, 1, POPUP_ROW_HEIGHT).build());
            popupButtons[row].visible = false;
        }
        positionRows(); refreshLabels();
    }

    void openCustom() { Minecraft.getInstance().gui.setScreen(new CustomLibraryScreen(this, config)); }

    private Button bodyButton(Component label, int x, int y, int width, int height, Button.OnPress action) {
        Button button = addRenderableWidget(Button.builder(label, action).bounds(x, y, width, height).build());
        bodyButtons.add(button); navigation.add(button); baseY.put(button, y);
        return button;
    }

    private Component masterLabel() { return Component.literal("Tracers: " + (config.enabled() ? "ON" : "OFF")); }
    private Component mobLabel(TracerTarget target) { return Component.literal(target.label() + ": " + (config.isSelected(target) ? "ON" : "OFF")); }
    private void refreshLabels() {
        displayedEnabled = config.enabled(); enabledButton.setMessage(masterLabel());
        int count = 0;
        for (TracerTarget target : TYPES) {
            int i = target.ordinal();
            mobButtons[i].setMessage(mobLabel(target));
            lineButtons[i].setMessage(Component.literal(target.label() + " line: " + config.lineChoice(target).label()));
            glowButtons[i].setMessage(Component.literal(target.label() + " glow: " + config.glowChoice(target).label()));
            lineLabels[i] = MenuFont.ellipsis("Line: " + config.lineChoice(target).label(), (lineButtons[i].getWidth() - 38) / .73);
            glowLabels[i] = MenuFont.ellipsis("Glow: " + config.glowChoice(target).label(), (glowButtons[i].getWidth() - 38) / .73);
            if (config.isSelected(target)) count++;
        }
        for (CustomTracker custom : config.custom().all) if (custom.enabled()) count++;
        selectionLabel = count + " selected";
    }

    private int maximumScroll() { return Math.max(0, contentHeight - (bodyBottom - bodyTop)); }
    private void positionRows() {
        scroll = Math.clamp(scroll, 0, maximumScroll());
        for (Button button : bodyButtons) {
            button.setY(baseY.get(button) - scroll);
            button.visible = button.getY() >= bodyTop && button.getBottom() <= bodyBottom;
            if (!button.visible && getFocused() == button) setFocused(null);
        }
    }
    private void reveal(Button button) {
        Integer original = baseY.get(button);
        if (original == null) return;
        if (original - scroll < bodyTop) scroll = original - bodyTop;
        if (original - scroll + button.getHeight() > bodyBottom) scroll = original + button.getHeight() - bodyBottom;
        positionRows();
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (displayedEnabled != config.enabled()) refreshLabels();
        long elapsed = clock.getAsLong() - openedAt;
        double opacity = MenuOpening.panel(elapsed);
        graphics.fill(0, 0, width, height, ((int) (MenuOpening.backdrop(elapsed) * 155) << 24) | 0x08090B);
        var canvas = new MenuCanvas(graphics, opacity);
        canvas.round(left, top + 5, panelWidth, panelHeight, 14, 0x70000000);
        canvas.round(left, top, panelWidth, panelHeight, 14, MenuTheme.BORDER);
        canvas.round(left + 1, top + 1, panelWidth - 2, panelHeight - 2, 13, MenuTheme.PANEL);
        canvas.round(left + 16, top + 1, panelWidth - 32, 1, 0, 0x357E8794);
        MenuFont.opacity(opacity);
        try {
            canvas.hivethief(config.style(), left + 31, top + 27, 34);
            MenuFont.text(graphics, "Hunting Helper", left + 55, top + 16, titleScale, MenuTheme.TEXT);
            MenuFont.text(graphics, shortcut, left + 56, top + 36, .7, MenuTheme.MUTED);
            canvas.round(left + 16, top + (cards ? 57 : 54), panelWidth - 32, 1, 0, MenuTheme.BORDER);
            smallButton(graphics, canvas, enabledButton, "Tracers", mouseX, mouseY);
            toggle(canvas, enabledButton.getRight() - 37, enabledButton.getY() + 6, config.enabled(), MenuTheme.ACCENT);
            center(graphics, selectionLabel, enabledButton.getX() + 48, top + 46, .66, MenuTheme.MUTED);
            graphics.enableScissor(left + 12, bodyTop, left + panelWidth - 12, bodyBottom);
            try {
                for (TracerTarget target : TYPES) drawTarget(graphics, canvas, target, mouseX, mouseY);
                drawCustom(graphics, canvas, mouseX, mouseY);
            }
            finally { graphics.disableScissor(); }
            if (maximumScroll() > 0) {
                int track = bodyBottom - bodyTop;
                int thumb = Math.max(18, track * track / contentHeight);
                canvas.round(left + panelWidth - 11, bodyTop, 3, track, 1, MenuTheme.TRACK);
                canvas.round(left + panelWidth - 11, bodyTop + (track - thumb) * scroll / maximumScroll(), 3, thumb, 1, MenuTheme.ACCENT);
            }
            smallButton(graphics, canvas, allButton, "All presets", mouseX, mouseY);
            smallButton(graphics, canvas, noneButton, "Clear presets", mouseX, mouseY);
            smallButton(graphics, canvas, homeButton, "Home", mouseX, mouseY);
            smallButton(graphics, canvas, doneButton, "Done", mouseX, mouseY);
            if (panelWidth >= 560) {
                int chunks = Minecraft.getInstance().options.getEffectiveRenderDistance();
                if (shownChunks != chunks) { shownChunks = chunks; rangeLabel = chunks + "-chunk render distance"; }
                center(graphics, rangeLabel, (noneButton.getRight() + homeButton.getX()) / 2, allButton.getY() + 8, .7, MenuTheme.MUTED);
            }
            if (popupTarget != null) drawPopup(graphics, canvas, mouseX, mouseY);
        } finally { MenuFont.opacity(1); }
        double iconOpacity = MenuOpening.icon(elapsed);
        if (iconOpacity > 0) {
            double travel = MenuOpening.travel(elapsed);
            int x = (int) Math.round(width / 2. + (left + 31 - width / 2.) * travel);
            int y = (int) Math.round(height / 2. + (top + 27 - height / 2.) * travel);
            int size = (int) Math.round(68 - 39 * travel);
            var intro = new MenuCanvas(graphics, iconOpacity);
            int halo = size + 24;
            intro.round(x - halo / 2, y - halo / 2, halo, halo, halo / 2, 0x14FFC655);
            intro.hivethief(config.style(), x, y, size);
        }
    }

    private void drawCustom(GuiGraphicsExtractor g, MenuCanvas c, int mouseX, int mouseY) {
        Button b = customButton;
        int x = b.getX(), y = b.getY(), w = b.getWidth(), h = b.getHeight();
        if (y >= bodyBottom || y + h <= bodyTop) return;
        boolean over = b.isFocused() || hovered(b, mouseX, mouseY);
        c.round(x, y, w, h, 10, over ? MenuTheme.ACCENT : MenuTheme.BORDER);
        c.round(x + 1, y + 1, w - 2, h - 2, 9, over ? MenuTheme.HOVER : MenuTheme.CARD);
        int mid = x + w / 2;
        c.round(mid - 10, y + 10, 20, 20, 6, MenuTheme.CONTROL);
        c.round(mid - 5, y + 19, 10, 2, 1, MenuTheme.ACCENT);
        c.round(mid - 1, y + 15, 2, 10, 1, MenuTheme.ACCENT);
        center(g, "Custom trackers", mid, y + h / 2 - 11, 1.02, MenuTheme.TEXT);
        int total = config.custom().all.size();
        center(g, total == 0 ? "Nametag or mob type" : total + (total == 1 ? " saved rule" : " saved rules"),
                mid, y + h / 2 + 5, .76, MenuTheme.MUTED);
        c.round(x + 10, y + h - 25, w - 20, 19, 5, MenuTheme.CONTROL);
        center(g, "Open library  →", mid, y + h - 20, .8, MenuTheme.TEXT);
    }

    private void drawTarget(GuiGraphicsExtractor graphics, MenuCanvas canvas, TracerTarget target, int mouseX, int mouseY) {
        int i = target.ordinal(); Button button = mobButtons[i];
        int x = button.getX() - 10, y = button.getY() - 8, w = button.getWidth() + 20, h = cardHeight;
        if (y >= bodyBottom || y + h <= bodyTop) return;
        boolean selected = config.isSelected(target), hover = button.visible && (hovered(button, mouseX, mouseY) || button.isFocused());
        int accent = config.lineColor(target);
        canvas.round(x, y + 2, w, h, 10, 0x40000000);
        canvas.round(x, y, w, h, 10, hover ? MenuTheme.ACCENT : MenuTheme.BORDER);
        canvas.round(x + 1, y + 1, w - 2, h - 2, 9, hover ? MenuTheme.HOVER : MenuTheme.CARD);
        if (cards) {
            int selector = cardHeight - 54;
            int middle = y + selector / 2;
            int tile = Math.min(32, selector - 8);
            canvas.round(x + 10, middle - tile / 2, tile, tile, 7, MenuTheme.CONTROL);
            MobSymbols.draw(graphics, canvas, target, x + 10 + tile / 2, middle, tile - 8);
            int labelX = x + tile + 17;
            double scale = Math.min(.94, (x + w - 38 - labelX) / MenuFont.width(target.label()));
            MenuFont.text(graphics, target.label(), labelX, middle - 9, scale, MenuTheme.TEXT);
            MenuFont.text(graphics, target.torrhusOnly() ? "Torrhus" : "All areas", labelX, middle + 5, .71, MenuTheme.MUTED);
            toggle(canvas, x + w - 37, middle - 7, selected, accent);
            canvas.round(x + 12, y + cardHeight - 54, w - 24, 1, 0, MenuTheme.BORDER);
        } else {
            MobSymbols.draw(graphics, canvas, target, x + 25, y + 27, 21);
            MenuFont.text(graphics, target.label(), x + 46, y + 23, 1, MenuTheme.TEXT);
            toggle(canvas, x + w - 41, y + 19, selected, accent);
        }
        colorButton(graphics, canvas, lineButtons[i], lineLabels[i], accent, mouseX, mouseY);
        colorButton(graphics, canvas, glowButtons[i], glowLabels[i], config.glowColor(target), mouseX, mouseY);
    }

    private void colorButton(GuiGraphicsExtractor graphics, MenuCanvas canvas, Button button, String text,
            int swatch, int mouseX, int mouseY) {
        boolean hover = button.visible && (hovered(button, mouseX, mouseY) || button.isFocused());
        canvas.round(button.getX(), button.getY(), button.getWidth(), button.getHeight(), 5, hover ? MenuTheme.CONTROL_HOVER : MenuTheme.CONTROL);
        canvas.round(button.getX() + 7, button.getY() + 6, 7, 7, 2, swatch == 0 ? MenuTheme.MUTED : swatch);
        MenuFont.text(graphics, text, button.getX() + 19, button.getY() + 6, .73, MenuTheme.TEXT);
        MenuFont.text(graphics, "v", button.getRight() - 13, button.getY() + 5, .75, MenuTheme.MUTED);
    }

    private OverlayColor[] popupColors() { return popupGlow ? GLOW_COLORS : LINE_COLORS; }
    private void openPopup(TracerTarget target, boolean glow, Button anchor) {
        if (popupTarget == target && popupGlow == glow) { closePopup(); return; }
        popupTarget = target; popupGlow = glow; popupAnchor = anchor;
        OverlayColor current = glow ? config.glowChoice(target) : config.lineChoice(target);
        popupIndex = Arrays.asList(popupColors()).indexOf(current);
        popupScroll = Math.clamp(popupIndex - 2, 0, popupColors().length - POPUP_ROWS);
        popupWidth = Math.min(190, Math.max(132, anchor.getWidth()));
        popupX = Math.clamp(anchor.getX(), left + 12, left + panelWidth - 12 - popupWidth);
        int popupHeight = POPUP_ROWS * POPUP_ROW_HEIGHT + 12;
        popupY = anchor.getBottom() + 4;
        if (popupY + popupHeight > top + panelHeight - 40) popupY = anchor.getY() - popupHeight - 4;
        popupY = Math.clamp(popupY, top + 54, top + panelHeight - 40 - popupHeight);
        positionPopup();
    }
    private void positionPopup() {
        OverlayColor[] choices = popupColors();
        for (int row = 0; row < POPUP_ROWS; row++) {
            Button b = popupButtons[row]; b.visible = true;
            b.setPosition(popupX + 4, popupY + 6 + row * POPUP_ROW_HEIGHT); b.setWidth(popupWidth - 13);
            b.setMessage(Component.literal(choices[popupScroll + row].label()));
        }
        if (popupIndex >= popupScroll && popupIndex < popupScroll + POPUP_ROWS) setFocused(popupButtons[popupIndex - popupScroll]);
        else setFocused(null);
    }
    private void closePopup() {
        popupTarget = null;
        for (Button b : popupButtons) if (b != null) b.visible = false;
        if (popupAnchor != null && popupAnchor.visible) setFocused(popupAnchor);
    }
    private void choose(int index) {
        if (popupTarget == null) return;
        colorChange.apply(popupTarget, popupGlow, popupColors()[index]);
        closePopup(); refreshLabels();
    }
    private void drawPopup(GuiGraphicsExtractor graphics, MenuCanvas canvas, int mouseX, int mouseY) {
        int h = POPUP_ROWS * POPUP_ROW_HEIGHT + 12;
        canvas.round(popupX - 2, popupY + 3, popupWidth + 4, h + 2, 8, 0xB0000000);
        canvas.round(popupX, popupY, popupWidth, h, 7, 0xFF737B87);
        canvas.round(popupX + 1, popupY + 1, popupWidth - 2, h - 2, 6, 0xFA1C1E23);
        OverlayColor current = popupGlow ? config.glowChoice(popupTarget) : config.lineChoice(popupTarget);
        for (int row = 0; row < POPUP_ROWS; row++) {
            Button b = popupButtons[row]; OverlayColor color = popupColors()[popupScroll + row];
            if (hovered(b, mouseX, mouseY) || b.isFocused() || color == current)
                canvas.round(b.getX(), b.getY(), b.getWidth(), b.getHeight(), 4, color == current ? 0xFF3A424D : MenuTheme.CONTROL_HOVER);
            int swatch = color == OverlayColor.MATCH ? config.lineColor(popupTarget) : color.argb();
            canvas.round(b.getX() + 6, b.getY() + 7, 8, 8, 2, swatch == 0 ? MenuTheme.MUTED : swatch);
            MenuFont.text(graphics, color.label(), b.getX() + 21, b.getY() + 7, .83, MenuTheme.TEXT);
        }
        int track = POPUP_ROWS * POPUP_ROW_HEIGHT, thumb = track * POPUP_ROWS / popupColors().length;
        canvas.round(popupX + popupWidth - 6, popupY + 6, 2, track, 1, MenuTheme.BORDER);
        canvas.round(popupX + popupWidth - 6, popupY + 6 + (track - thumb) * popupScroll / (popupColors().length - POPUP_ROWS), 2, thumb, 1, MenuTheme.ACCENT);
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (popupTarget != null) {
            popupScroll = Math.clamp(popupScroll + (vertical > 0 ? -1 : vertical < 0 ? 1 : 0), 0, popupColors().length - POPUP_ROWS);
            popupIndex = Math.clamp(popupIndex, popupScroll, popupScroll + POPUP_ROWS - 1);
            positionPopup(); return true;
        }
        if (x >= left && x < left + panelWidth && y >= bodyTop && y < bodyBottom && vertical != 0) {
            scroll -= (int) Math.round(vertical * 30); positionRows(); return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (popupTarget != null) {
            if (event.button() == 0) for (int row = 0; row < POPUP_ROWS; row++) {
                if (hovered(popupButtons[row], event.x(), event.y())) { choose(popupScroll + row); return true; }
            }
            closePopup(); return true;
        }
        if (event.button() == 0 && maximumScroll() > 0 && event.x() >= left + panelWidth - 15
                && event.x() < left + panelWidth - 5 && event.y() >= bodyTop && event.y() < bodyBottom) {
            draggingScrollbar = true; scrollToPointer(event.y()); return true;
        }
        return super.mouseClicked(event, twice);
    }
    private void scrollToPointer(double y) {
        int track = bodyBottom - bodyTop, thumb = Math.max(18, track * track / contentHeight);
        scroll = (int) Math.round((y - bodyTop - thumb / 2.) / (track - thumb) * maximumScroll()); positionRows();
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingScrollbar) { scrollToPointer(event.y()); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingScrollbar && event.button() == 0) { draggingScrollbar = false; return true; }
        return super.mouseReleased(event);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (popupTarget != null) {
            if (key == 256) closePopup();
            else if (key == 257 || key == 335 || key == 32) choose(popupIndex);
            else if (key == 264 || key == 265 || key == 258 || key == 266 || key == 267 || key == 268 || key == 269) {
                int step = key == 265 || key == 266 || (key == 258 && (event.modifiers() & 1) != 0) ? -1 : 1;
                popupIndex = key == 268 ? 0 : key == 269 ? popupColors().length - 1
                        : Math.clamp(popupIndex + step * (key == 266 || key == 267 ? POPUP_ROWS : 1), 0, popupColors().length - 1);
                if (popupIndex < popupScroll) popupScroll = popupIndex;
                if (popupIndex >= popupScroll + POPUP_ROWS) popupScroll = popupIndex - POPUP_ROWS + 1;
                positionPopup();
            }
            return true;
        }
        if (key == 258) {
            int at = navigation.indexOf(getFocused());
            int next = at < 0 ? ((event.modifiers() & 1) == 0 ? 0 : navigation.size() - 1)
                    : Math.floorMod(at + ((event.modifiers() & 1) == 0 ? 1 : -1), navigation.size());
            Button b = navigation.get(next); reveal(b); setFocused(b); return true;
        }
        if (key == 266 || key == 267 || key == 268 || key == 269) {
            scroll = key == 268 ? 0 : key == 269 ? maximumScroll() : scroll + (key == 266 ? -1 : 1) * (bodyBottom - bodyTop - 16);
            positionRows(); return true;
        }
        return super.keyPressed(event);
    }

    private static void smallButton(GuiGraphicsExtractor graphics, MenuCanvas canvas, Button button, String label, int mouseX, int mouseY) {
        boolean hover = hovered(button, mouseX, mouseY) || button.isFocused();
        boolean primary = label.equals("Done");
        canvas.round(button.getX(), button.getY(), button.getWidth(), button.getHeight(), 6,
                primary ? (hover ? MenuTheme.TEXT : MenuTheme.ACCENT) : hover ? MenuTheme.CONTROL_HOVER : MenuTheme.CONTROL);
        if (label.equals("Tracers")) MenuFont.text(graphics, label, button.getX() + 8, button.getY() + 9, .76, MenuTheme.TEXT);
        else center(graphics, label, button.getX() + button.getWidth() / 2, button.getY() + 8, .85, primary ? 0xFF171B20 : MenuTheme.TEXT);
    }
    private static void toggle(MenuCanvas canvas, int x, int y, boolean on, int color) {
        canvas.round(x, y, 29, 15, 7, on ? color : 0xFF4D5159);
        canvas.round(x + (on ? 16 : 2), y + 2, 11, 11, 5, on ? MenuTheme.TEXT : MenuTheme.MUTED);
    }
    private static void center(GuiGraphicsExtractor graphics, String text, int x, double y, double scale, int color) {
        MenuFont.text(graphics, text, x - MenuFont.width(text) * scale / 2, y, scale, color);
    }
    private static boolean hovered(Button button, double x, double y) {
        return x >= button.getX() && x < button.getRight() && y >= button.getY() && y < button.getBottom();
    }
    @Override public void onClose() { Minecraft.getInstance().gui.setScreen(home); }
    @Override public boolean isPauseScreen() { return false; }
}

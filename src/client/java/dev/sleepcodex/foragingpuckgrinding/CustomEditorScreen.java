package dev.sleepcodex.foragingpuckgrinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

final class CustomEditorScreen extends TrackerPage {
    private final TracerConfig config;
    private record Swatch(Button button, OverlayColor color, boolean glow) { }
    private final List<Swatch> swatches = new ArrayList<>();
    private final CustomTracker original;
    private final UUID id;
    private CustomTracker.Mode mode;
    private String named = "", typed = "";
    private OverlayColor line, glow;
    private EditBox field;
    private Button save;
    private boolean confirmRemove;
    private int fieldY, paletteY;

    CustomEditorScreen(CustomLibraryScreen parent, TracerConfig config, CustomTracker original) {
        super(original == null ? "Add tracker" : "Edit tracker", parent);
        this.config = config;
        this.original = original;
        id = original == null ? UUID.randomUUID() : original.id();
        mode = original == null ? CustomTracker.Mode.NAMETAG : original.mode();
        line = original == null ? OverlayColor.CYAN : original.line();
        glow = original == null ? OverlayColor.MATCH : original.glow();
        if (original != null) remember(original.query());
    }
    @Override int preferredHeight() { return 258; }
    @Override String subtitle() {
        return mode == CustomTracker.Mode.NAMETAG ? "Match a nametag (e.g. Revenant Sycophant)" : "Match by mob type (e.g. wither_skeleton)";
    }
    private boolean namedMode() { return mode == CustomTracker.Mode.NAMETAG; }
    private void remember(String text) { if (namedMode()) named = text; else typed = text; }

    @Override void layout() {
        int x = left + 16;
        int inner = w - 32;
        int half = (inner - 5) / 2;
        button("Nametag", x, top + 52, half, 20, namedMode() ? Tone.PRIMARY : Tone.NORMAL,
                () -> switchMode(CustomTracker.Mode.NAMETAG));
        button("Mob type", x + half + 5, top + 52, inner - half - 5, 20,
                namedMode() ? Tone.NORMAL : Tone.PRIMARY, () -> switchMode(CustomTracker.Mode.MOB_TYPE));
        fieldY = top + 85;
        field = new EditBox(font, x + 7, fieldY + 5, inner - 14, 13, Component.literal(namedMode() ? "Nametag contains" : "Mob type ID"));
        field.setBordered(false);
        field.setMaxLength(96);
        field.setValue(namedMode() ? named : typed);
        field.setTextColor(MenuTheme.TEXT);
        addRenderableWidget(field);
        paletteY = (top + 117 + footer - 48) / 2;
        swatches.clear();
        palette(false, paletteY);
        palette(true, paletteY + 24);
        button("Cancel", x, footer, 57, 23, Tone.NORMAL, this::onClose);
        if (original != null) button(confirmRemove ? "Confirm removal" : "Remove", x + 63, footer, 91, 23,
                Tone.DANGER, this::remove);
        save = button("Save tracker", left + w - 99, footer, 83, 23, Tone.PRIMARY, this::commit);
        field.setResponder(text -> { remember(text); validate(); });
        validate();
        setInitialFocus(field);
    }

    private void switchMode(CustomTracker.Mode next) {
        if (next == mode) { setFocused(field); return; }
        mode = next;
        confirmRemove = false;
        refresh();
    }

    private void palette(boolean forGlow, int y) {
        int x = left + 84;
        int step = Math.min(29, (w - 100) / OverlayColor.values().length);
        for (OverlayColor color : OverlayColor.values()) {
            if (!forGlow && !color.isSolid()) continue;
            String name = (forGlow ? "Glow " : "Line ") + color.label();
            Button b = addRenderableWidget(Button.builder(Component.literal(name), pressed -> {
                if (forGlow) glow = color; else line = color;
            }).bounds(x, y, step - 2, 18).build());
            swatches.add(new Swatch(b, color, forGlow));
            x += step;
        }
    }

    private void validate() {
        String value = CustomTracker.clean(field.getValue());
        status = "";
        if (value.isEmpty()) status = namedMode() ? "Enter part of the mob name." : "Enter a mob type.";
        else if (!namedMode()) {
            Identifier key = Identifier.tryParse(CustomTracker.normalizeType(value));
            var type = key == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(key).orElse(null);
            if (type == null || type == EntityTypes.PLAYER || type == EntityTypes.ARMOR_STAND || !DefaultAttributes.hasSupplier(type))
                status = "Unknown mob type. Try zombie or wither_skeleton.";
        }
        save.active = status.isEmpty();
    }

    private void commit() {
        validate();
        if (!save.active) return;
        var tracker = new CustomTracker(id, mode, field.getValue(), original == null || original.enabled(), line, glow);
        try {
            config.putCustom(tracker);
            ((CustomLibraryScreen) parent).reveal(tracker);
            onClose();
        } catch (IOException e) { saveFailed(e); }
    }

    private void remove() {
        if (!confirmRemove) { confirmRemove = true; refresh(); return; }
        try { config.removeCustom(id); onClose(); }
        catch (IOException e) { saveFailed(e); }
    }

    @Override void content(GuiGraphicsExtractor g, MenuCanvas c, int mx, int my, float delta) {
        int x = left + 16;
        c.round(x, fieldY - 1, w - 32, 21, 5, field.isFocused() ? MenuTheme.ACCENT : MenuTheme.BORDER);
        c.round(x + 1, fieldY, w - 34, 19, 4, 0xFF131519);
        field.extractRenderState(g, mx, my, delta);
        if (field.getValue().isEmpty()) {
            String example = namedMode() ? "e.g. Revenant Sycophant" : "e.g. wither_skeleton";
            MenuFont.text(g, example, x + 14, fieldY + 5, .87, MenuTheme.MUTED);
        }
        String hint = !status.isEmpty() ? status : namedMode() ? "Matches names containing \"" + named.strip() + "\""
                : CustomTracker.normalizeType(typed);
        MenuFont.text(g, MenuFont.ellipsis(hint, (w - 32) / .78), x, fieldY + 23, .78, MenuTheme.MUTED);
        String lineLabel = line.label(), glowLabel = glow.label();
        for (Swatch swatch : swatches) {
            Button b = swatch.button();
            boolean hovered = over(b, mx, my);
            if (hovered) {
                if (swatch.glow()) glowLabel = swatch.color().label(); else lineLabel = swatch.color().label();
            }
            boolean selected = swatch.color() == (swatch.glow() ? glow : line);
            int bx = b.getX(), by = b.getY(), bw = b.getWidth();
            c.round(bx, by, bw, 18, 4, selected ? MenuTheme.TEXT : hovered ? MenuTheme.ACCENT : MenuTheme.BORDER);
            c.round(bx + 1, by + 1, bw - 2, 16, 3, MenuTheme.CARD);
            OverlayColor color = swatch.color();
            int ink = color == OverlayColor.MATCH ? line.argb() : color.argb();
            if (ink != 0) c.round(bx + 4, by + 4, bw - 8, 10, 2, ink);
            if (color == OverlayColor.MATCH || color == OverlayColor.OFF)
                MenuFont.text(g, color == OverlayColor.MATCH ? "=" : "×", bx + bw / 2. - 2, by + 4, .85,
                        color == OverlayColor.MATCH ? 0xFF131519 : MenuTheme.TEXT);
        }
        MenuFont.text(g, "Line", x, paletteY, .85, MenuTheme.TEXT);
        MenuFont.text(g, lineLabel, x, paletteY + 10, .72, MenuTheme.MUTED);
        MenuFont.text(g, "Glow", x, paletteY + 24, .85, MenuTheme.TEXT);
        MenuFont.text(g, glowLabel, x, paletteY + 34, .72, MenuTheme.MUTED);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (field.isFocused() && (event.key() == 257 || event.key() == 335)) { commit(); return true; }
        return super.keyPressed(event);
    }
}

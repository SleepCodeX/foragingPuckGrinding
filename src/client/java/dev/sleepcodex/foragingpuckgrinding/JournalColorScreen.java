package dev.sleepcodex.foragingpuckgrinding;

import dev.sleepcodex.foragingpuckgrinding.journal.client.Colours;
import dev.sleepcodex.foragingpuckgrinding.journal.client.ConfigManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

final class JournalColorScreen extends TrackerPage {
    private final Consumer<String> set;
    private String value;
    private EditBox field;
    private record Swatch(OverlayColor color, Button button) {}
    private final List<Swatch> swatches = new ArrayList<>();
    JournalColorScreen(FieldJournalScreen parent, String label, String value, Consumer<String> set) {
        super(label, parent);
        this.value = value == null ? "#FFFFFFFF" : value;
        this.set = set;
    }
    @Override int preferredHeight() { return 218; }
    @Override String subtitle() { return status.isEmpty() ? "Choose a swatch or enter #AARRGGBB. Add @1 to @60 for chroma." : status; }
    @Override void layout() {
        field = new EditBox(font, left + 22, top + 63, w - 76, 18, Component.literal("Marker color"));
        field.setMaxLength(12);
        field.setValue(value);
        addRenderableWidget(field);
        swatches.clear();
        int step = Math.min(38, (w - 42) / 8);
        for (OverlayColor color : OverlayColor.values()) {
            if (!color.isSolid()) continue;
            Button b = addRenderableWidget(Button.builder(Component.literal(color.label()), ignored -> {
                String alpha = Colours.valid(value) ? value.substring(1, 3) : "FF";
                String cycle = Colours.valid(value) ? value.substring(9) : "";
                field.setValue("#" + alpha + String.format(Locale.ROOT, "%06X", color.argb() & 0xFFFFFF) + cycle);
            }).bounds(left + 20 + step * swatches.size(), top + 96, step - 4, 18).build());
            swatches.add(new Swatch(color, b));
        }
        button("Cycle hue", left + 20, top + 122, 73, 19, Tone.NORMAL, () -> {
            String base = Colours.valid(value) ? value : "#FFFFCC55";
            field.setValue(base.length() > 9 ? base.substring(0, 9) : base + "@4");
        });
        button("Opacity", left + 98, top + 122, 63, 19, Tone.NORMAL, () -> {
            String base = Colours.valid(value) ? value : "#FFFFCC55";
            int alpha = Integer.parseInt(base.substring(1, 3), 16);
            String next = alpha > 192 ? "C0" : alpha > 128 ? "80" : alpha > 64 ? "40" : "FF";
            field.setValue("#" + next + base.substring(3));
        });
        var save = button("Save color", left + w - 95, footer, 80, 23, Tone.PRIMARY, () -> {
            set.accept(value);
            if (ConfigManager.save()) onClose();
            else status = "Could not save. See the game log.";
        });
        save.active = Colours.valid(value);
        field.setResponder(v -> { value = v; save.active = Colours.valid(v); });
        button("Cancel", left + 16, footer, 65, 23, Tone.NORMAL, this::onClose);
        setInitialFocus(field);
    }
    @Override void content(GuiGraphicsExtractor g, MenuCanvas c, int mx, int my, float delta) {
        field.extractRenderState(g, mx, my, delta);
        c.round(left + w - 43, top + 61, 23, 23, 5, MenuTheme.BORDER);
        c.round(left + w - 41, top + 63, 19, 19, 4, Colours.argb(value, MenuTheme.CONTROL));
        for (var swatch : swatches) {
            Button b = swatch.button();
            c.round(b.getX(), b.getY(), b.getWidth(), b.getHeight(), 5, over(b,mx,my) ? MenuTheme.TEXT : MenuTheme.BORDER);
            c.round(b.getX()+3, b.getY()+3, b.getWidth()-6, b.getHeight()-6, 3, swatch.color().argb());
        }
    }
}

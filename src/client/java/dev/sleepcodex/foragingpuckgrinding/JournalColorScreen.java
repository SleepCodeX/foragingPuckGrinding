package dev.sleepcodex.foragingpuckgrinding;

import dev.sleepcodex.foragingpuckgrinding.journal.client.Colours;
import dev.sleepcodex.foragingpuckgrinding.journal.client.ConfigManager;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

final class JournalColorScreen extends TrackerPage {
    private final Consumer<String> set;
    private String value;
    private EditBox field;
    JournalColorScreen(FieldJournalScreen parent, String label, String value, Consumer<String> set) {
        super(label, parent);
        this.value = value == null ? "#FFFFFFFF" : value;
        this.set = set;
    }
    @Override int preferredHeight() { return 175; }
    @Override String subtitle() { return status.isEmpty() ? "Enter #AARRGGBB. Add @1 to @60 for a chroma period in seconds." : status; }
    @Override void layout() {
        field = new EditBox(font, left + 22, top + 63, w - 76, 18, Component.literal("Marker color"));
        field.setMaxLength(12);
        field.setValue(value);
        addRenderableWidget(field);
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
        MenuFont.text(g, "Example  #FFFFCC55@4", left + 22, top + 90, .83, MenuTheme.MUTED);
    }
}

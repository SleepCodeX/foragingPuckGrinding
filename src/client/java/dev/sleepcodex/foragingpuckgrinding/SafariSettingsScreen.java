package dev.sleepcodex.foragingpuckgrinding;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.johnceo.sparklingmutuals.config.ConfigEditorButton;
import net.johnceo.sparklingmutuals.config.ConfigEditorColour;
import net.johnceo.sparklingmutuals.config.ConfigEditorDropdown;
import net.johnceo.sparklingmutuals.config.ConfigEditorSlider;
import net.johnceo.sparklingmutuals.config.SafariSettings;
import net.johnceo.sparklingmutuals.config.SettingsCatalog;
import net.johnceo.sparklingmutuals.config.SettingsColor;
import net.johnceo.sparklingmutuals.config.SettingsTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Sparkling Mutuals' settings in Hunting Helper's matte charcoal style. Every row comes from SM's own settings
 * metadata (SafariSettings + SettingsCatalog), so settings added by future upstream releases appear here without
 * changes; pages this screen does not know yet are listed under "More".
 */
final class SafariSettingsScreen extends Screen {
    /** Areas shown in the side navigation, by SM page (category) name, with the tab label for each page. */
    private static final Map<String, Map<String, String>> AREAS = new LinkedHashMap<>();
    static {
        AREAS.put("HUDs", tabs("General", "Overview", "HUDs", "HUDs", "Customization", "Appearance"));
        AREAS.put("Safari", tabs("Modes", "Modes", "Safari helpers", "Helpers", "Banner", "Alerts", "Miria contest", "Contest"));
        AREAS.put("Hunting", tabs("Hunting", "Hunting"));
        AREAS.put("Party", tabs("Party Interactions", "Interactions", "Sparkle Warping", "Sparkle Warping"));
        AREAS.put("ESP", tabs("ESP General", "General", "ESP", "Colors", "Mode-wide Critter Toggles", "Mode-wide",
                "Profitable Critter Toggles", "Profitable", "Always On Toggle", "Always on", "Floor Drop Always On Toggle", "Floor drops"));
        AREAS.put("History", tabs("History", "History"));
        AREAS.put("Advanced", tabs("API key", "API", "SkyBlock", "SkyBlock"));
    }
    private static Map<String, String> tabs(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put(pairs[i], pairs[i + 1]);
        return map;
    }

    private record Rect(int x, int y, int w, int h) {
        boolean contains(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }
    private record Hit(Rect rect, Runnable action) { }
    private record Card(SettingsCatalog.Row row, int depth, int height, List<String> title, List<String> description, int controlWidth, boolean stacked) { }

    private final Screen parent;
    private final SettingsTarget initialTarget;
    private SafariSettings settings = new SafariSettings();
    private SettingsCatalog catalog = new SettingsCatalog(settings);
    private final List<String> areaNames = new ArrayList<>();
    private final Map<String, List<Integer>> areaPages = new LinkedHashMap<>();
    private final List<Button> areaButtons = new ArrayList<>(), tabButtons = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private final Set<String> expanded = new HashSet<>();
    private final long opened = System.nanoTime();
    private String area = "Safari";
    private int page, scroll, contentHeight;
    private int left, top, w, h, navRight, bodyTop, bodyBottom, bodyLeft, bodyRight, footer;
    private boolean targetApplied, contentDrawing;
    private String focusPath;
    private long highlightUntil;
    private EditBox search, editor;
    private Button back, done;
    // Modal state: a dropdown, a text field or a color picker for one row.
    private SettingsCatalog.Row modal;
    private List<String> choices;
    private int choiceScroll;
    private float hue, saturation, brightness = 1;
    private int alpha = 255, chroma;
    private Rect modalRect = new Rect(0, 0, 0, 0);
    private SettingsCatalog.Row slider;
    private Rect sliderTrack;
    private java.util.function.BiConsumer<Double, Double> colorDrag;
    private String validation = "";

    SafariSettingsScreen(Screen parent, SettingsTarget target) {
        super(Component.literal("Safari settings"));
        this.parent = parent;
        this.initialTarget = target;
        expanded.addAll(catalog.defaultExpanded());
        buildAreas();
        page = areaPages.get(area).getFirst();
    }

    String area() { return area; }
    String pageName() { return catalog.getPages().get(page).getName(); }

    private void buildAreas() {
        areaNames.clear(); areaPages.clear();
        var pages = catalog.getPages();
        Set<Integer> placed = new HashSet<>();
        for (var entry : AREAS.entrySet()) {
            List<Integer> indices = new ArrayList<>();
            for (String name : entry.getValue().keySet())
                for (int i = 0; i < pages.size(); i++) if (pages.get(i).getName().equals(name)) { indices.add(i); placed.add(i); }
            if (!indices.isEmpty()) { areaNames.add(entry.getKey()); areaPages.put(entry.getKey(), indices); }
        }
        List<Integer> more = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) if (!placed.contains(i)) more.add(i);
        if (!more.isEmpty()) { areaNames.add("More"); areaPages.put("More", more); }
    }

    private String tabLabel(int index) {
        String name = catalog.getPages().get(index).getName();
        Map<String, String> labels = AREAS.get(area);
        return labels != null && labels.containsKey(name) ? labels.get(name) : name;
    }

    @Override protected void init() {
        // Reopened after an editor (HUD layout, API key) changed the saved values: read them again.
        settings.apply(true);
        settings = new SafariSettings();
        catalog = new SettingsCatalog(settings);
        buildAreas();
        if (!targetApplied) {
            targetApplied = true;
            if (initialTarget != null) {
                var located = catalog.locate(initialTarget);
                if (located != null) {
                    page = located.getFirst();
                    for (String name : areaNames) if (areaPages.get(name).contains(page)) area = name;
                    expanded.addAll(catalog.expansionFor(initialTarget));
                    focusPath = located.getSecond().getLast().getPath();
                    highlightUntil = System.currentTimeMillis() + 6000;
                }
            }
        }
        w = Math.min(660, width - 16);
        h = Math.min(420, height - 12);
        left = (width - w) / 2;
        top = (height - h) / 2;
        int navWidth = w < 480 ? 78 : 108;
        navRight = left + 12 + navWidth;
        footer = top + h - 32;
        bodyLeft = navRight + 12;
        bodyRight = left + w - 14;
        areaButtons.clear(); tabButtons.clear();
        int y = top + 54;
        int rowHeight = Math.max(18, Math.min(24, (footer - 8 - y) / Math.max(1, areaNames.size()) - 3));
        for (String name : areaNames) {
            areaButtons.add(addRenderableWidget(Button.builder(Component.literal(name), b -> selectArea(name))
                    .bounds(left + 12, y, navWidth, rowHeight).build()));
            y += rowHeight + 3;
        }
        int tabX = bodyLeft, tabY = top + 54;
        List<Integer> pages = areaPages.get(area);
        if (pages.size() > 1) {
            for (int index : pages) {
                String label = tabLabel(index);
                int tabW = (int) Math.max(46, MenuFont.width(label) * .8 + 18);
                if (tabX > bodyLeft && tabX + tabW > bodyRight) { tabX = bodyLeft; tabY += 22; }
                final int target = index;
                tabButtons.add(addRenderableWidget(Button.builder(Component.literal(label), b -> { page = target; scroll = 0; rebuild(); })
                        .bounds(tabX, tabY, tabW, 18).build()));
                tabX += tabW + 4;
            }
            bodyTop = tabY + 26;
        } else bodyTop = top + 54;
        bodyBottom = footer - 8;
        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, left + w - 14 - 150 + 8, top + 18, 136, 12, Component.literal("Search settings"));
        search.setBordered(false); search.setMaxLength(80); search.setValue(query);
        search.setTextColor(MenuTheme.TEXT); search.setResponder(text -> scroll = 0);
        addRenderableWidget(search);
        if (editor != null) addRenderableWidget(editor);
        back = addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(left + w - 14 - 128, footer, 60, 22).build());
        done = addRenderableWidget(Button.builder(Component.literal("Done"), b -> { closeModal(); Minecraft.getInstance().gui.setScreen(null); })
                .bounds(left + w - 14 - 62, footer, 62, 22).build());
    }

    private void rebuild() { clearWidgets(); init(); }
    private void selectArea(String name) {
        if (name.equals(area) && search.getValue().isEmpty()) return;
        area = name; page = areaPages.get(name).getFirst(); scroll = 0; search.setValue(""); rebuild();
    }

    @Override public void tick() { settings.apply(true); }
    @Override public void removed() {
        settings.apply(true);
        settings.saveTextFields();
        super.removed();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() {
        if (modal != null) { closeModal(); return; }
        Minecraft.getInstance().gui.setScreen(parent);
    }

    // ------------------------------------------------------------------ layout

    private List<SettingsCatalog.Row> visibleRows(List<Integer> depths) {
        List<SettingsCatalog.Row> rows = new ArrayList<>();
        String query = search == null ? "" : search.getValue().trim();
        if (!query.isEmpty()) {
            for (var row : catalog.search(query)) { rows.add(row); depths.add(0); }
            return rows;
        }
        visit(catalog.getPages().get(page).getRows(), 0, rows, depths);
        return rows;
    }
    private void visit(List<SettingsCatalog.Row> source, int depth, List<SettingsCatalog.Row> rows, List<Integer> depths) {
        for (var row : source) {
            rows.add(row); depths.add(depth);
            if (row.getGroup() && expanded.contains(row.getPath())) visit(row.getChildren(), depth + 1, rows, depths);
        }
    }

    private static List<String> wrap(String text, double scale, int width) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String next = line.isEmpty() ? word : line + " " + word;
                if (MenuFont.width(next) * scale > width && !line.isEmpty()) { lines.add(line.toString()); line = new StringBuilder(word); }
                else line = new StringBuilder(next);
            }
            if (!line.isEmpty()) lines.add(line.toString());
        }
        return lines;
    }

    private String valueLabel(SettingsCatalog.Row row) {
        Field f = row.getField();
        Object value = row.read();
        var button = f.getAnnotation(ConfigEditorButton.class);
        var dropdown = f.getAnnotation(ConfigEditorDropdown.class);
        if (button != null) return button.buttonText();
        if (dropdown != null) {
            int index = value instanceof Number n ? n.intValue() : 0;
            return index >= 0 && index < dropdown.values().length ? dropdown.values()[index] : "Choose";
        }
        if (f.isAnnotationPresent(ConfigEditorColour.class)) return SettingsColor.INSTANCE.hex(String.valueOf(value));
        if (f.isAnnotationPresent(ConfigEditorSlider.class)) return number(value);
        String text = value == null ? "" : value.toString();
        return text.isBlank() ? "Edit…" : text;
    }
    private static String number(Object value) {
        float v = value instanceof Number n ? n.floatValue() : 0;
        return v % 1 == 0 ? Integer.toString((int) v) : String.format(Locale.ROOT, "%.2f", v);
    }

    private List<Card> measure() {
        List<Integer> depths = new ArrayList<>();
        List<SettingsCatalog.Row> rows = visibleRows(depths);
        List<Card> cards = new ArrayList<>();
        boolean searching = !search.getValue().isBlank();
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            int depth = Math.min(depths.get(i), 4);
            int width = bodyRight - 8 - bodyLeft - depth * 10;
            Object value = row.read();
            Field f = row.getField();
            int control;
            if (row.getGroup()) control = row.getPrimaryToggle() != null ? 40 : 0;
            else if (value instanceof Boolean && f.getAnnotation(ConfigEditorButton.class) == null) control = 40;
            else if (f.isAnnotationPresent(ConfigEditorSlider.class) || f.isAnnotationPresent(ConfigEditorColour.class)) control = 120;
            else control = (int) Math.min(220, Math.max(92, MenuFont.width(valueLabel(row)) * .78 + 30));
            boolean stacked = !row.getGroup() && width - control - 30 < 130;
            int textWidth = stacked || control == 0 ? width - 24 : width - control - 34;
            String heading = searching ? catalog.location(row) + " › " + row.getName() : row.getName();
            List<String> title = wrap(heading, .86, textWidth - (row.getGroup() ? 14 : 0));
            List<String> description = row.getDescription().isBlank() ? List.of() : wrap(row.getDescription(), .7, textWidth);
            int text = title.size() * 11 + (description.isEmpty() ? 0 : 3 + description.size() * 9);
            int height = row.getGroup() ? Math.max(28, 14 + text) : stacked ? 16 + text + 26 : Math.max(32, 16 + text);
            cards.add(new Card(row, depth, height, title, description, control, stacked));
        }
        return cards;
    }

    // ------------------------------------------------------------------ drawing

    private void hit(Rect r, Runnable action) {
        Rect clipped = r;
        if (contentDrawing) {
            int y0 = Math.max(bodyTop, r.y()), y1 = Math.min(bodyBottom, r.y() + r.h());
            clipped = new Rect(r.x(), y0, r.w(), Math.max(0, y1 - y0));
        }
        if (clipped.h() > 0) hits.add(new Hit(clipped, action));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        hits.clear();
        boolean blocked = modal != null;
        int px = blocked ? Integer.MIN_VALUE : mx, py = blocked ? Integer.MIN_VALUE : my;
        double progress = Math.clamp((System.nanoTime() - opened) / 140_000_000.0, 0, 1);
        g.fill(0, 0, width, height, 0x9706090C);
        var c = new MenuCanvas(g, .82 + .18 * progress);
        c.round(left, top + 4, w, h, 13, 0x60000000);
        c.round(left, top, w, h, 13, MenuTheme.BORDER);
        c.round(left + 1, top + 1, w - 2, h - 2, 12, MenuTheme.PANEL);
        c.round(left + 16, top + 1, w - 32, 1, 0, 0x527B8794);
        c.hivethief(left + 28, top + 24, 26);
        MenuFont.text(g, "Safari settings", left + 46, top + 11, 1.25, MenuTheme.TEXT);
        String subtitle = search.getValue().isBlank() ? area + (areaPages.get(area).size() > 1 ? "  ·  " + tabLabel(page) : "")
                : "Search results";
        MenuFont.text(g, MenuFont.ellipsis(subtitle, (w - 220) / .76), left + 47, top + 29, .76, MenuTheme.MUTED);
        // Search field.
        Rect searchBox = new Rect(left + w - 14 - 150, top + 13, 150, 22);
        c.round(searchBox.x(), searchBox.y(), searchBox.w(), searchBox.h(), 6, search.isFocused() ? MenuTheme.ACCENT : MenuTheme.BORDER);
        c.round(searchBox.x() + 1, searchBox.y() + 1, searchBox.w() - 2, searchBox.h() - 2, 5, MenuTheme.CONTROL);
        if (search.getValue().isEmpty() && !search.isFocused()) MenuFont.text(g, "Search settings…", searchBox.x() + 9, searchBox.y() + 6, .76, MenuTheme.MUTED);
        c.round(left + 15, top + 45, w - 30, 1, 0, MenuTheme.BORDER);
        c.round(navRight + 5, top + 52, 1, footer - top - 58, 0, MenuTheme.BORDER);
        for (Button b : areaButtons) {
            boolean on = b.getMessage().getString().equals(area) && search.getValue().isBlank();
            boolean over = TrackerPage.over(b, px, py);
            c.round(b.getX(), b.getY(), b.getWidth(), b.getHeight(), 6, on ? 0xFF3A424D : over ? MenuTheme.HOVER : 0x00000000);
            if (on) c.round(b.getX(), b.getY() + 4, 2, b.getHeight() - 8, 1, MenuTheme.ACCENT);
            MenuFont.text(g, MenuFont.ellipsis(b.getMessage().getString(), (b.getWidth() - 16) / .84), b.getX() + 9,
                    b.getY() + (b.getHeight() - 9) / 2., .84, on ? MenuTheme.TEXT : MenuTheme.MUTED);
        }
        for (Button b : tabButtons) {
            boolean on = b.getMessage().getString().equals(tabLabel(page)) && search.getValue().isBlank();
            boolean over = TrackerPage.over(b, px, py);
            c.round(b.getX(), b.getY(), b.getWidth(), b.getHeight(), 5, on ? MenuTheme.ACCENT : over ? MenuTheme.CONTROL_HOVER : MenuTheme.CONTROL);
            String label = b.getMessage().getString();
            MenuFont.text(g, label, b.getX() + (b.getWidth() - MenuFont.width(label) * .8) / 2, b.getY() + 5, .8,
                    on ? 0xFF15191D : MenuTheme.TEXT);
            b.visible = search.getValue().isBlank();
        }
        drawCards(g, c, px, py);
        MenuFont.text(g, "Changes save automatically", bodyLeft, footer + 7, .74, MenuTheme.MUTED);
        footerButton(g, c, back, false, px, py);
        footerButton(g, c, done, true, px, py);
        if (blocked) { hits.clear(); drawModal(g, c, mx, my); }
        search.visible = !blocked;
        super.extractRenderState(g, mx, my, delta);
    }

    private void footerButton(GuiGraphicsExtractor g, MenuCanvas c, Button b, boolean primary, int mx, int my) {
        boolean over = TrackerPage.over(b, mx, my);
        c.round(b.getX(), b.getY(), b.getWidth(), b.getHeight(), 5, primary ? (over ? 0xFFDBE7EF : 0xFFBFCEDB) : over ? MenuTheme.CONTROL_HOVER : MenuTheme.CONTROL);
        String label = b.getMessage().getString();
        MenuFont.text(g, label, b.getX() + (b.getWidth() - MenuFont.width(label) * .86) / 2, b.getY() + 7, .86, primary ? 0xFF15191D : MenuTheme.TEXT);
    }

    private void drawCards(GuiGraphicsExtractor g, MenuCanvas c, int mx, int my) {
        List<Card> cards = measure();
        contentHeight = 0;
        for (Card card : cards) contentHeight += card.height() + 6;
        if (focusPath != null) {
            int offset = 0;
            for (Card card : cards) { if (card.row().getPath().equals(focusPath)) { scroll = offset; break; } offset += card.height() + 6; }
            focusPath = focusPath + "\u0000";
        }
        scroll = Math.clamp(scroll, 0, maxScroll());
        g.enableScissor(bodyLeft, bodyTop, bodyRight, bodyBottom);
        contentDrawing = true;
        int y = bodyTop - scroll;
        String highlighted = highlightUntil > System.currentTimeMillis() && focusPath != null ? focusPath.replace("\u0000", "") : null;
        try {
            for (Card card : cards) {
                if (y + card.height() > bodyTop && y < bodyBottom) drawCard(g, c, card, y, mx, my, card.row().getPath().equals(highlighted));
                y += card.height() + 6;
            }
        } finally { contentDrawing = false; g.disableScissor(); }
        if (cards.isEmpty()) MenuFont.text(g, "Nothing matches.", bodyLeft + 4, bodyTop + 6, .86, MenuTheme.MUTED);
        if (maxScroll() > 0) {
            int track = bodyBottom - bodyTop, thumb = Math.max(18, track * track / Math.max(1, contentHeight));
            c.round(bodyRight - 3, bodyTop, 3, track, 1, MenuTheme.TRACK);
            c.round(bodyRight - 3, bodyTop + (track - thumb) * scroll / maxScroll(), 3, thumb, 1, MenuTheme.ACCENT);
        }
    }
    private int maxScroll() { return Math.max(0, contentHeight - (bodyBottom - bodyTop)); }

    private void drawCard(GuiGraphicsExtractor g, MenuCanvas c, Card card, int y, int mx, int my, boolean highlighted) {
        var row = card.row();
        int x = bodyLeft + card.depth() * 10, cw = bodyRight - 8 - x, ch = card.height();
        Rect r = new Rect(x, y, cw, ch);
        boolean over = r.contains(mx, my);
        if (!row.getStandalone()) {
            c.round(x, y, cw, ch, 7, highlighted ? MenuTheme.ACCENT : MenuTheme.BORDER);
            c.round(x + 1, y + 1, cw - 2, ch - 2, 6, over ? MenuTheme.HOVER : row.getGroup() ? 0xF0262930 : MenuTheme.CARD);
        }
        int textTop = row.getGroup() || card.stacked() ? y + 8 : y + (ch - (card.title().size() * 11
                + (card.description().isEmpty() ? 0 : 3 + card.description().size() * 9))) / 2;
        int textX = x + 11;
        if (row.getGroup()) {
            boolean open = expanded.contains(row.getPath());
            drawChevron(c, x + 10, y + ch / 2, open);
            textX = x + 24;
            hit(r, () -> { if (!expanded.add(row.getPath())) expanded.remove(row.getPath()); });
        }
        int ty = textTop;
        for (String line : card.title()) { MenuFont.text(g, line, textX, ty, .86, row.getStandalone() ? MenuTheme.ACCENT : MenuTheme.TEXT); ty += 11; }
        if (!card.description().isEmpty()) ty += 3;
        for (String line : card.description()) { MenuFont.text(g, line, textX, ty, .7, MenuTheme.MUTED); ty += 9; }
        if (row.getGroup()) {
            var toggle = row.getPrimaryToggle();
            if (toggle != null && !expanded.contains(row.getPath())) control(g, c, toggle, new Rect(x + cw - 12 - 40, y + (ch - 18) / 2, 40, 18), mx, my);
            return;
        }
        if (row.getStandalone()) return;
        Rect control = card.stacked() ? new Rect(x + 11, y + ch - 30, cw - 22, 20)
                : new Rect(x + cw - 12 - card.controlWidth(), y + (ch - 20) / 2, card.controlWidth(), 20);
        control(g, c, row, control, mx, my);
    }

    private static void drawChevron(MenuCanvas c, int x, int y, boolean open) {
        for (int step = 0; step < 4; step++) {
            if (open) { c.round(x - 3 + step, y - 2 + step, 1, 1, 0, MenuTheme.ACCENT); c.round(x + 3 - step, y - 2 + step, 1, 1, 0, MenuTheme.ACCENT); }
            else { c.round(x - 1 + step, y - 3 + step, 1, 1, 0, MenuTheme.ACCENT); c.round(x - 1 + step, y + 3 - step, 1, 1, 0, MenuTheme.ACCENT); }
        }
    }

    private void control(GuiGraphicsExtractor g, MenuCanvas c, SettingsCatalog.Row row, Rect r, int mx, int my) {
        Field f = row.getField();
        Object value = row.read();
        var button = f.getAnnotation(ConfigEditorButton.class);
        var range = f.getAnnotation(ConfigEditorSlider.class);
        var dropdown = f.getAnnotation(ConfigEditorDropdown.class);
        boolean over = r.contains(mx, my);
        if (button != null) {
            c.round(r.x(), r.y(), r.w(), r.h(), 5, over ? MenuTheme.CONTROL_HOVER : MenuTheme.CONTROL);
            centered(g, button.buttonText(), r, .8, MenuTheme.TEXT);
            hit(r, () -> { settings.apply(false); if (value instanceof Runnable run) run.run(); else settings.executeRunnable(button.runnableId()); });
        } else if (range != null) {
            float fraction = Math.clamp(((value instanceof Number n ? n.floatValue() : 0) - range.minValue()) / (range.maxValue() - range.minValue()), 0, 1);
            int trackX = r.x() + 30, trackW = r.w() - 34;
            MenuFont.text(g, number(value), r.x() + 1, r.y() + 6, .76, MenuTheme.ACCENT);
            c.round(trackX, r.y() + 9, trackW, 3, 1, MenuTheme.TRACK);
            c.round(trackX, r.y() + 9, Math.max(1, (int) (trackW * fraction)), 3, 1, MenuTheme.ACCENT);
            int knob = trackX + (int) (trackW * fraction);
            c.round(knob - 4, r.y() + 6, 9, 9, 4, over ? MenuTheme.TEXT : 0xFFD5DCE3);
            Rect track = new Rect(trackX - 4, r.y(), trackW + 8, r.h());
            hit(track, () -> { slider = row; sliderTrack = new Rect(trackX, r.y(), trackW, r.h()); setSlider(mx); });
        } else if (dropdown != null) {
            c.round(r.x(), r.y(), r.w(), r.h(), 5, over ? MenuTheme.CONTROL_HOVER : MenuTheme.CONTROL);
            MenuFont.text(g, MenuFont.ellipsis(valueLabel(row), (r.w() - 24) / .78), r.x() + 8, r.y() + 6, .78, MenuTheme.TEXT);
            MenuFont.text(g, "v", r.x() + r.w() - 12, r.y() + 5, .75, MenuTheme.MUTED);
            hit(r, () -> { modal = row; choices = List.of(dropdown.values()); choiceScroll = Math.max(0, (value instanceof Number n ? n.intValue() : 0) - 3); });
        } else if (f.isAnnotationPresent(ConfigEditorColour.class)) {
            String color = String.valueOf(value);
            c.round(r.x(), r.y(), r.w(), r.h(), 5, over ? MenuTheme.CONTROL_HOVER : MenuTheme.CONTROL);
            c.round(r.x() + 5, r.y() + 4, 22, r.h() - 8, 3, SettingsColor.INSTANCE.argb(color, System.currentTimeMillis()) | 0xFF000000);
            MenuFont.text(g, SettingsColor.INSTANCE.hex(color), r.x() + 33, r.y() + 6, .78, MenuTheme.TEXT);
            hit(r, () -> openColor(row));
        } else if (value instanceof Boolean on) {
            int tx = r.x() + r.w() - 31, ty = r.y() + (r.h() - 15) / 2;
            c.round(tx, ty, 29, 15, 7, on ? MenuTheme.ACCENT : 0xFF4D5159);
            c.round(tx + (on ? 16 : 2), ty + 2, 11, 11, 5, on ? MenuTheme.TEXT : MenuTheme.MUTED);
            hit(r, () -> { row.write(!on); settings.apply(true); });
        } else {
            c.round(r.x(), r.y(), r.w(), r.h(), 5, over ? MenuTheme.CONTROL_HOVER : MenuTheme.CONTROL);
            MenuFont.text(g, MenuFont.ellipsis(valueLabel(row), (r.w() - 14) / .78), r.x() + 7, r.y() + 6, .78, MenuTheme.TEXT);
            hit(r, () -> openText(row));
        }
    }

    private static void centered(GuiGraphicsExtractor g, String text, Rect r, double scale, int color) {
        String fitted = MenuFont.ellipsis(text, (r.w() - 8) / scale);
        MenuFont.text(g, fitted, r.x() + (r.w() - MenuFont.width(fitted) * scale) / 2, r.y() + (r.h() - 9) / 2., scale, color);
    }

    private void setSlider(double mx) {
        if (slider == null || sliderTrack == null) return;
        var range = slider.getField().getAnnotation(ConfigEditorSlider.class);
        double fraction = Math.clamp((mx - sliderTrack.x()) / Math.max(1, sliderTrack.w()), 0, 1);
        double raw = range.minValue() + fraction * (range.maxValue() - range.minValue());
        double snapped = Math.clamp(range.minValue() + Math.round((raw - range.minValue()) / range.minStep()) * range.minStep(),
                range.minValue(), range.maxValue());
        slider.write(slider.read() instanceof Integer ? (Object) (int) Math.round(snapped) : (Object) (float) snapped);
        settings.apply(true);
    }

    // ------------------------------------------------------------------ modals

    private void openText(SettingsCatalog.Row row) {
        modal = row; validation = "";
        editor = new EditBox(font, 0, 0, 200, 14, Component.literal(row.getName()));
        editor.setMaxLength(2048);
        editor.setValue(String.valueOf(row.read() == null ? "" : row.read()));
        editor.setTextColor(MenuTheme.TEXT);
        addRenderableWidget(editor); setFocused(editor);
    }
    private void openColor(SettingsCatalog.Row row) {
        String[] parts = String.valueOf(row.read()).split(":");
        int[] p = new int[5];
        for (int i = 0; i < 5 && i < parts.length; i++) try { p[i] = Integer.parseInt(parts[i]); } catch (NumberFormatException ignored) { }
        float[] hsb = java.awt.Color.RGBtoHSB(p[2], p[3], p[4], null);
        hue = hsb[0]; saturation = hsb[1]; brightness = hsb[2]; chroma = p[0]; alpha = parts.length > 1 ? p[1] : 255;
        openText(row);
        editor.setValue(SettingsColor.INSTANCE.hex(String.valueOf(row.read())));
    }
    private boolean colorEditing() { return modal != null && modal.getField().isAnnotationPresent(ConfigEditorColour.class); }
    private String colorValue() {
        int rgb = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
        return chroma + ":" + alpha + ":" + (rgb >> 16 & 255) + ":" + (rgb >> 8 & 255) + ":" + (rgb & 255);
    }
    private void closeModal() {
        if (editor != null) removeWidget(editor);
        editor = null; modal = null; choices = null; colorDrag = null; slider = null; validation = "";
    }
    private void saveEditor() {
        if (modal == null || editor == null) return;
        String input = editor.getValue();
        Object value = input;
        if (colorEditing()) {
            String parsed = SettingsColor.INSTANCE.fromHex(input, colorValue());
            if (parsed == null) { validation = "Use #RRGGBB, for example #55FF55"; return; }
            value = parsed;
        }
        modal.write(value);
        settings.apply(true);
        settings.saveTextFields();
        closeModal();
    }

    private void drawModal(GuiGraphicsExtractor g, MenuCanvas c, int mx, int my) {
        g.fill(0, 0, width, height, 0x99000000);
        boolean list = choices != null, color = colorEditing();
        int mw = Math.min(list || color ? 300 : 420, width - 24);
        int rows = list ? Math.min(choices.size(), Math.max(3, (height - 120) / 20)) : 0;
        int mh = list ? 46 + rows * 20 + 18 : color ? 210 : 112;
        int x = (width - mw) / 2, y = (height - mh) / 2;
        modalRect = new Rect(x, y, mw, mh);
        c.round(x - 2, y + 3, mw + 4, mh + 2, 10, 0xB0000000);
        c.round(x, y, mw, mh, 9, 0xFF737B87);
        c.round(x + 1, y + 1, mw - 2, mh - 2, 8, 0xFA1C1E23);
        MenuFont.text(g, MenuFont.ellipsis(modal.getName(), (mw - 28) / .95), x + 14, y + 12, .95, MenuTheme.TEXT);
        if (list) {
            choiceScroll = Math.clamp(choiceScroll, 0, Math.max(0, choices.size() - rows));
            Object current = modal.read();
            int selected = current instanceof Number n ? n.intValue() : -1;
            for (int i = 0; i < rows; i++) {
                int index = choiceScroll + i;
                Rect r = new Rect(x + 8, y + 32 + i * 20, mw - 16, 18);
                boolean over = r.contains(mx, my);
                if (over || index == selected) c.round(r.x(), r.y(), r.w(), r.h(), 4, index == selected ? 0xFF3A424D : MenuTheme.CONTROL_HOVER);
                MenuFont.text(g, MenuFont.ellipsis(choices.get(index), (r.w() - 16) / .82), r.x() + 8, r.y() + 5, .82, MenuTheme.TEXT);
                hit(r, () -> { modal.write(index); settings.apply(true); closeModal(); });
            }
            if (choices.size() > rows) MenuFont.text(g, "Scroll for more  ·  Esc to return", x + 14, y + mh - 15, .7, MenuTheme.MUTED);
            return;
        }
        int fieldY = y + mh - 62;
        if (color) drawColor(g, c, x, y, mw, mx, my);
        c.round(x + 12, fieldY - 4, mw - 24, 20, 5, MenuTheme.CONTROL);
        editor.setX(x + 18); editor.setY(fieldY + 2); editor.setWidth(mw - 36);
        MenuFont.text(g, validation.isBlank() ? (color ? "Hex color, or drag above" : "Enter to save") : validation,
                x + 14, y + mh - 30, .72, validation.isBlank() ? MenuTheme.MUTED : 0xFFFFA5A1);
        Rect cancel = new Rect(x + mw - 140, y + mh - 26, 60, 18), save = new Rect(x + mw - 74, y + mh - 26, 60, 18);
        c.round(cancel.x(), cancel.y(), cancel.w(), cancel.h(), 5, cancel.contains(mx, my) ? MenuTheme.CONTROL_HOVER : MenuTheme.CONTROL);
        centered(g, "Cancel", cancel, .82, MenuTheme.TEXT);
        c.round(save.x(), save.y(), save.w(), save.h(), 5, save.contains(mx, my) ? 0xFFDBE7EF : 0xFFBFCEDB);
        centered(g, "Save", save, .82, 0xFF15191D);
        hit(cancel, this::closeModal);
        hit(save, this::saveEditor);
    }

    private void drawColor(GuiGraphicsExtractor g, MenuCanvas c, int x, int y, int mw, int mx, int my) {
        Rect pad = new Rect(x + 14, y + 32, mw - 64, 64);
        for (int i = 0; i < 32; i++) for (int j = 0; j < 12; j++)
            g.fill(pad.x() + pad.w() * i / 32, pad.y() + pad.h() * j / 12, pad.x() + pad.w() * (i + 1) / 32, pad.y() + pad.h() * (j + 1) / 12,
                    java.awt.Color.HSBtoRGB(hue, i / 31f, 1 - j / 11f));
        int markX = pad.x() + (int) (saturation * pad.w()), markY = pad.y() + (int) ((1 - brightness) * pad.h());
        g.fill(markX - 2, markY - 1, markX + 3, markY + 2, 0xFFFFFFFF);
        Rect hueBar = new Rect(x + mw - 42, y + 32, 26, 64);
        for (int i = 0; i < 32; i++) g.fill(hueBar.x(), hueBar.y() + i * 2, hueBar.x() + hueBar.w(), hueBar.y() + i * 2 + 2, java.awt.Color.HSBtoRGB(i / 32f, 1, 1));
        bindColor(pad, (px, py) -> { saturation = (float) Math.clamp((px - pad.x()) / pad.w(), 0, 1); brightness = 1 - (float) Math.clamp((py - pad.y()) / pad.h(), 0, 1); }, mx, my);
        bindColor(hueBar, (px, py) -> hue = (float) Math.clamp((py - hueBar.y()) / hueBar.h(), 0, 1), mx, my);
        Rect opacity = new Rect(x + 120, y + 104, mw - 136, 10);
        MenuFont.text(g, "Opacity " + Math.round(alpha * 100 / 255f) + "%", x + 14, y + 104, .72, MenuTheme.MUTED);
        c.round(opacity.x(), opacity.y() + 3, opacity.w(), 4, 2, MenuTheme.TRACK);
        c.round(opacity.x(), opacity.y() + 3, Math.max(1, opacity.w() * alpha / 255), 4, 2, MenuTheme.ACCENT);
        bindColor(opacity, (px, py) -> alpha = (int) Math.round(Math.clamp((px - opacity.x()) / opacity.w(), 0, 1) * 255), mx, my);
        Rect speed = new Rect(x + 120, y + 120, mw - 136, 10);
        MenuFont.text(g, "Chroma " + chroma, x + 14, y + 120, .72, MenuTheme.MUTED);
        c.round(speed.x(), speed.y() + 3, speed.w(), 4, 2, MenuTheme.TRACK);
        c.round(speed.x(), speed.y() + 3, Math.max(1, speed.w() * chroma / 255), 4, 2, MenuTheme.ACCENT);
        bindColor(speed, (px, py) -> chroma = (int) Math.round(Math.clamp((px - speed.x()) / speed.w(), 0, 1) * 255), mx, my);
    }
    private void bindColor(Rect r, java.util.function.BiConsumer<Double, Double> action, int mx, int my) {
        hit(r, () -> { colorDrag = action; action.accept((double) mx, (double) my); editor.setValue(SettingsColor.INSTANCE.hex(colorValue())); });
    }

    // ------------------------------------------------------------------ input

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (event.button() == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                if (hits.get(i).rect().contains(event.x(), event.y())) { hits.get(i).action().run(); return true; }
            }
            if (modal != null && !modalRect.contains(event.x(), event.y())) { closeModal(); return true; }
        }
        if (modal != null && editor == null) return true;
        return super.mouseClicked(event, twice);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (colorDrag != null) { colorDrag.accept(event.x(), event.y()); if (editor != null) editor.setValue(SettingsColor.INSTANCE.hex(colorValue())); return true; }
        if (slider != null) { setSlider(event.x()); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        slider = null; colorDrag = null;
        return super.mouseReleased(event);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (choices != null) { choiceScroll -= (int) Math.signum(vertical) * 2; return true; }
        if (modal == null) scroll = Math.clamp(scroll - (int) Math.round(vertical * 30), 0, maxScroll());
        return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (modal != null) {
            if (key == 256) { closeModal(); return true; }
            if ((key == 257 || key == 335) && editor != null) { saveEditor(); return true; }
            return super.keyPressed(event);
        }
        if (key == 266 || key == 267) { scroll = Math.clamp(scroll + (key == 266 ? -1 : 1) * (bodyBottom - bodyTop - 20), 0, maxScroll()); return true; }
        return super.keyPressed(event);
    }
}

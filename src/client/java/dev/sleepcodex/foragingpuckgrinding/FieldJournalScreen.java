package dev.sleepcodex.foragingpuckgrinding;

import dev.sleepcodex.foragingpuckgrinding.journal.client.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/** Safari additions with their own state, plus the report and the panel layout editor. */
public final class FieldJournalScreen extends TrackerPage {
    private enum Group {
        HOME("Field journal", "Hunter offers, field observations and run reports"),
        DISPLAY("Journal panels", "Choose the panels shown during a run"),
        WORLD("World markers", "Mark trades, perches and capsule return positions"),
        ALERTS("Encounter alerts", "Choose which stages and milestones interrupt a run"),
        SHARING("Party sharing", "Choose an audience for each announcement"),
        VALUES("Shard values", "Compare order-book prices for your catches"),
        COLORS("Marker colors", "Edit a marker's color, opacity and chroma period"),
        TOOLS("Diagnostics", "Enable extra /critters commands on the next world join");
        final String title, detail;
        Group(String title, String detail) { this.title = title; this.detail = detail; }
    }
    private record Item(String label, Runnable action) {}
    private final List<Item> items = new ArrayList<>();
    private final Group group;
    private final CritterConfig cfg = ConfigManager.get();
    private int page;

    public FieldJournalScreen(Screen parent) { this(parent, Group.HOME); }
    private FieldJournalScreen(Screen parent, Group group) {
        super(group.title, parent);
        this.group = group;
    }
    @Override int preferredHeight() { return 290; }
    @Override String subtitle() { return status.isEmpty() ? group.detail : status; }

    private void show(Screen screen) { Minecraft.getInstance().gui.setScreen(screen); }
    private void persist() {
        status = ConfigManager.save() ? "" : "Could not save. See the game log.";
        refresh();
    }
    private void toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
        items.add(new Item(label + " · " + (get.getAsBoolean() ? "On" : "Off"), () -> {
            set.accept(!get.getAsBoolean());
            persist();
        }));
    }
    private void choices(String label, IntSupplier get, IntConsumer set, String... values) {
        int current = Math.floorMod(get.getAsInt(), values.length);
        items.add(new Item(label + " · " + values[current], () -> {
            set.accept((current + 1) % values.length);
            persist();
        }));
    }
    private void color(String name, Supplier<String> get, Consumer<String> set) {
        items.add(new Item(name, () -> show(new JournalColorScreen(this, name, get.get(), set))));
    }
    @Override void layout() {
        items.clear();
        switch (group) {
            case HOME -> {
                items.add(new Item("Open field report", () -> show(new CritterScreen(this))));
                items.add(new Item("Move journal panels", () -> show(new HudEditorScreen(this))));
                for (Group target : Group.values()) {
                    if (target != Group.HOME)
                        items.add(new Item(target.title, () -> show(new FieldJournalScreen(this, target))));
                }
            }
            case DISPLAY -> {
                toggle("Journal panels", () -> cfg.display.hudEnabled, v -> cfg.display.hudEnabled = v);
                toggle("Hunter offers", () -> cfg.display.showTrades, v -> cfg.display.showTrades = v);
                toggle("Party coverage", () -> cfg.display.showCoverage, v -> cfg.display.showCoverage = v);
                toggle("Player breakdown", () -> cfg.display.showPerPlayer, v -> cfg.display.showPerPlayer = v);
                toggle("Attempts and sightings", () -> cfg.display.showMissing, v -> cfg.display.showMissing = v);
                toggle("Count visible critters", () -> cfg.display.countSpawns, v -> cfg.display.countSpawns = v);
                toggle("Unique goals only", () -> cfg.display.uniqueOnly, v -> cfg.display.uniqueOnly = v);
                toggle("Troodon wall count", () -> cfg.display.showTroodonWalls, v -> cfg.display.showTroodonWalls = v);
                toggle("Mound outcomes", () -> cfg.advanced.showMounds, v -> cfg.advanced.showMounds = v);
                choices("Panel visibility", () -> cfg.display.showWhere, v -> cfg.display.showWhere = v, "Inside Safari", "Include entrance", "Everywhere");
            }
            case WORLD -> {
                toggle("Hunter trade pins", () -> cfg.display.highlightTrades, v -> cfg.display.highlightTrades = v);
                toggle("Hideonwall perches", () -> cfg.display.highlightHideonwalls, v -> cfg.display.highlightHideonwalls = v);
                toggle("Recatch pins", () -> cfg.display.recatchHelper, v -> cfg.display.recatchHelper = v);
                toggle("Troodon walls", () -> cfg.display.highlightTroodonWalls, v -> cfg.display.highlightTroodonWalls = v);
                toggle("Suppress Safari darkness", () -> cfg.display.removeDarkness, v -> cfg.display.removeDarkness = v);
            }
            case ALERTS -> {
                toggle("Hunter offers in chat", () -> cfg.alerts.traderAlerts, v -> cfg.alerts.traderAlerts = v);
                toggle("Gemzie stages", () -> cfg.alerts.gemzieAlert, v -> cfg.alerts.gemzieAlert = v);
                toggle("Wumpa stages", () -> cfg.alerts.wumpaAlert, v -> cfg.alerts.wumpaAlert = v);
                toggle("Doomspiral stages", () -> cfg.alerts.doomspiralAlert, v -> cfg.alerts.doomspiralAlert = v);
                toggle("Only in encounter biome", () -> cfg.alerts.encountersInBiomeOnly, v -> cfg.alerts.encountersInBiomeOnly = v);
                toggle("All except Macaw", () -> cfg.alerts.allButMacawNotify, v -> cfg.alerts.allButMacawNotify = v);
                toggle("Biome completed", () -> cfg.alerts.biomeDoneNotify, v -> cfg.alerts.biomeDoneNotify = v);
                toggle("Dex completed", () -> cfg.alerts.allDoneNotify, v -> cfg.alerts.allDoneNotify = v);
                toggle("Bluebird and Parakeet", () -> cfg.alerts.birdfeederAlert, v -> cfg.alerts.birdfeederAlert = v);
                toggle("Play alert sounds", () -> cfg.alerts.alertSound, v -> cfg.alerts.alertSound = v);
            }
            case SHARING -> {
                toggle("Read party offers", () -> cfg.party.acceptSharedTrades, v -> cfg.party.acceptSharedTrades = v);
                choices("Hunter trades", () -> cfg.party.traderBroadcast, v -> cfg.party.traderBroadcast = v, "Local only", "Party chat", "All chat");
                choices("Gemzie", () -> cfg.party.gemzieBroadcast, v -> cfg.party.gemzieBroadcast = v, "Local only", "Party chat", "All chat");
                choices("Wumpa", () -> cfg.party.wumpaBroadcast, v -> cfg.party.wumpaBroadcast = v, "Local only", "Party chat", "All chat");
                choices("Doomspiral", () -> cfg.party.doomspiralBroadcast, v -> cfg.party.doomspiralBroadcast = v, "Local only", "Party chat", "All chat");
                choices("Milestones", () -> cfg.party.milestoneBroadcast, v -> cfg.party.milestoneBroadcast = v, "Local only", "Party chat", "All chat");
                choices("Macaw", () -> cfg.party.macawBroadcast, v -> cfg.party.macawBroadcast = v, "Local only", "Party chat", "All chat");
                choices("Sparklings", () -> cfg.party.sparklingBroadcast, v -> cfg.party.sparklingBroadcast = v, "Local only", "Party chat", "All chat");
                choices("Your hotspot", () -> cfg.party.hotspotBroadcast, v -> cfg.party.hotspotBroadcast = v, "Local only", "Party chat", "All chat");
                choices("Manual missing report", () -> cfg.party.shareChannel, v -> cfg.party.shareChannel = v, "Party chat", "All chat", "Current chat");
            }
            case VALUES -> {
                toggle("Price run shards", () -> cfg.profit.enabled, v -> cfg.profit.enabled = v);
                choices("First price", () -> cfg.profit.priceSource, v -> cfg.profit.priceSource = v, "Instant sell", "Sell offer");
                toggle("Show both prices", () -> cfg.profit.showBothPrices, v -> cfg.profit.showBothPrices = v);
                toggle("Subtract 1.25% tax", () -> cfg.profit.subtractTax, v -> cfg.profit.subtractTax = v);
                toggle("Value in party notes", () -> cfg.profit.showOnHud, v -> cfg.profit.showOnHud = v);
            }
            case TOOLS -> {
                toggle("Diagnostic commands", () -> cfg.advanced.debugCommands, v -> cfg.advanced.debugCommands = v);
            }
            case COLORS -> {
                var d = cfg.display;
                color("Hunter trades", () -> d.tradeColour, v -> d.tradeColour = v);
                color("Troodon walls", () -> d.troodonWallColour, v -> d.troodonWallColour = v);
                color("Unchecked perch", () -> d.hideonwallColour, v -> d.hideonwallColour = v);
                color("Occupied perch", () -> d.hideonwallFoundColour, v -> d.hideonwallFoundColour = v);
                color("Recatch pins", () -> d.recatchColour, v -> d.recatchColour = v);
            }
        }
        int rows = Math.max(1, (footer - top - 57) / 27);
        int capacity = rows * 2, pages = Math.ceilDiv(items.size(), capacity);
        page = Math.clamp(page, 0, pages - 1);
        int col = (w - 36) / 2;
        for (int slot = 0; slot < capacity && page * capacity + slot < items.size(); slot++) {
            Item item = items.get(page * capacity + slot);
            button(item.label(), left + 15 + slot % 2 * (col + 6), top + 53 + slot / 2 * 27,
                    col, 23, group == Group.HOME && slot == 0 ? Tone.PRIMARY : Tone.NORMAL, item.action());
        }
        button("Back", left + 15, footer, 61, 23, Tone.NORMAL, this::onClose);
        if (pages > 1) {
            button("Prev", left + w - 111, footer, 45, 23, Tone.NORMAL, () -> { page--; refresh(); }).active = page > 0;
            button("Next", left + w - 61, footer, 46, 23, Tone.NORMAL, () -> { page++; refresh(); }).active = page + 1 < pages;
        }
    }
    @Override void content(GuiGraphicsExtractor g, MenuCanvas c, int mx, int my, float delta) {}
}

package dev.sleepcodex.foragingpuckgrinding;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import kotlin.Pair;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.johnceo.sparklingmutuals.config.ConfigManager;
import net.johnceo.sparklingmutuals.config.SafariChatConfig;
import net.johnceo.sparklingmutuals.hud.HudPanel;
import net.johnceo.sparklingmutuals.hud.HudRow;
import net.johnceo.sparklingmutuals.safari.*;
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear;
import net.johnceo.sparklingmutuals.safari.structures.WallSummary;
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels;
import net.minecraft.network.chat.Component;

final class FullClearGameChecks {
    private static final String FOX = "LOOT SHARE! You received 5x Foxtrot Shard from [VIP] Solsnatcher catching a Foxtrot!";
    private static final String GAZER = "CAPTURE! You caught a Gazer and gained a Gazer Shard!";

    private static void hidden(String text) {
        ClientReceiveMessageEvents.GAME_CANCELED.invoker().onReceiveGameMessageCanceled(Component.literal(text), false);
    }
    private static void visible(String text) {
        ClientReceiveMessageEvents.GAME.invoker().onReceiveGameMessage(Component.literal(text), false);
    }
    private static HudPanel panel(SafariRun run, SafariBiome biome) {
        return SafariPanels.INSTANCE.missing(run, biome, false, 0, 0,
                new WallSummary(List.of()), SafariSparklingMode.INSTANCE.getState());
    }
    private static void expect(HudPanel panel, String label, String value) {
        String actual = panel.getRows().stream().filter(row -> row.getLabel().equals(label))
                .map(HudRow::getValue).findFirst().orElse(null);
        if (!java.util.Objects.equals(actual, value))
            throw new AssertionError(label + " expected " + value + " but displayed " + actual);
    }

    static void run(ClientGameTestContext c, TestSingleplayerContext world) {
        var previousMode = c.computeOnClient(client -> SafariFullClear.INSTANCE.getMode());
        boolean previousFilter = c.computeOnClient(client -> ConfigManager.INSTANCE.getHideCaptureChat());
        c.runOnClient(client -> {
            SafariFullClear.INSTANCE.select(SafariMode.FULL_CLEAR);
            SafariAssist.INSTANCE.markEntered();
            SafariTracking.INSTANCE.getLedger().enter(System.currentTimeMillis());
            ConfigManager.INSTANCE.setHideCaptureChat(true);
            if (!SafariChatFilter.INSTANCE.hidden(FOX, true, SafariChatConfig.INSTANCE::hidden))
                throw new AssertionError("Expected the real chat helper to suppress this loot share");
        });
        // A real server packet goes through Fabric's ordinary GAME path and SM's display filter.
        world.getServer().runCommand("tellraw @p " + new com.google.gson.Gson().toJson(FOX));
        c.waitFor(client -> SafariTracking.INSTANCE.getRun() != null && SafariTracking.INSTANCE.getRun().count("Foxtrot") == 1);
        c.runOnClient(client -> {
            var run = SafariTracking.INSTANCE.getRun();
            if (run.count("Solsnatcher") != 0) throw new AssertionError("Catcher name became the species");
            hidden(FOX + "\n" + FOX);
            hidden(FOX);
            visible(FOX);
            hidden("Party > Moss: " + FOX + "\n" + FOX);
            ClientReceiveMessageEvents.GAME_CANCELED.invoker().onReceiveGameMessageCanceled(Component.literal(FOX), true);
            visible("FLOOR DROP! You found a Foxtrot Shard!");
            if (run.count("Foxtrot") != 5) throw new AssertionError("Expected five catches independent of shard stacks, chat visibility or quotes");
            expect(panel(run, SafariBiome.FOREST), "Foxtrot", "1–3 remaining");
            expect(panel(run, SafariBiome.FOREST), "Macaw", "Check feeder");
            visible(GAZER);
            hidden(GAZER);
            visible(GAZER);
            expect(panel(run, SafariBiome.HAUNTED), "Gazer", "1 remaining");
            var progress = SafariPanels.INSTANCE.progress(run, false, System.currentTimeMillis(), SafariSparklingMode.INSTANCE.getState());
            expect(progress, "Species caught", "2/37");
            expect(progress, "Forest", "1/9");
            expect(progress, "Haunted", "1/10");
            if (progress.getRows().stream().noneMatch(row -> row.getLabel().equals("Fully Collected")))
                throw new AssertionError("Missing strict full-clear total");
            for (var area : SafariBiome.values()) {
                int roster = area == SafariBiome.HAUNTED ? 10 : 9;
                if (FullClearProgress.remaining(null, area).size() != roster) throw new AssertionError("Incomplete " + area);
            }
            var rendered = new SafariRun(0, () -> 50_000L);
            var bodies = IntStream.range(0, 8).mapToObj(i -> new Pair<>("Foxtrot", new UUID(0, i))).toList();
            rendered.observePopulation(bodies);
            rendered.observePopulation(bodies);
            expect(panel(rendered, SafariBiome.FOREST), "Foxtrot", "8 remaining");
            for (int i = 0; i < 7; i++) rendered.record(SafariCatch.Companion.parse(FOX), 51_000L, "Player");
            expect(panel(rendered, SafariBiome.FOREST), "Foxtrot", "1 remaining");
            rendered.record(SafariCatch.Companion.parse(FOX), 51_100L, "Player");
            expect(panel(rendered, SafariBiome.FOREST), "Foxtrot", null);
        });
        c.clickScreenButton("Remaining critters");
        c.waitForScreen(FullClearScreen.class);
        c.clickScreenButton("Haunted");
        c.waitTicks(3);
        c.takeScreenshot("full-clear-remaining");
        c.clickScreenButton("Forest");
        c.clickScreenButton("Icy");
        c.clickScreenButton("Cavern");
        c.clickScreenButton("Back");
        c.waitForScreen(FieldJournalScreen.class);
        c.runOnClient(client -> {
            visible(GAZER);
            expect(panel(SafariTracking.INSTANCE.getRun(), SafariBiome.HAUNTED), "Gazer", null);
            SafariTracking.INSTANCE.getLedger().enter(System.currentTimeMillis());
            expect(panel(SafariTracking.INSTANCE.getRun(), SafariBiome.HAUNTED), "Gazer", "4 remaining");
            SafariFullClear.INSTANCE.select(SafariMode.UNIQUE);
            if (panel(SafariTracking.INSTANCE.getRun(), SafariBiome.HAUNTED).getTitle().equals("Haunted remaining"))
                throw new AssertionError("Unique mode picked up full-clear rows");
            SafariFullClear.INSTANCE.select(previousMode);
            ConfigManager.INSTANCE.setHideCaptureChat(previousFilter);
        });
        System.out.println("PASS: standalone full-clear HUD, filtered server loot shares, rendered populations, four biomes and run reset");
    }
}

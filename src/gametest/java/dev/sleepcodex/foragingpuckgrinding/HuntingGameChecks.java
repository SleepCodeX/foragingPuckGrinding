package dev.sleepcodex.foragingpuckgrinding;

import java.util.List;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.johnceo.sparklingmutuals.config.ConfigManager;
import net.johnceo.sparklingmutuals.config.SafariChatConfig;
import net.johnceo.sparklingmutuals.hud.HudPanel;
import net.johnceo.sparklingmutuals.hud.HudRow;
import net.johnceo.sparklingmutuals.hunting.Audience;
import net.johnceo.sparklingmutuals.hunting.ChatQueue;
import net.johnceo.sparklingmutuals.hunting.CritterEntities;
import net.johnceo.sparklingmutuals.hunting.HideonwallSpots;
import net.johnceo.sparklingmutuals.hunting.HuntingAnnouncer;
import net.johnceo.sparklingmutuals.hunting.HuntingConfig;
import net.johnceo.sparklingmutuals.hunting.HuntingOutput;
import net.johnceo.sparklingmutuals.hunting.HuntingRuns;
import net.johnceo.sparklingmutuals.hunting.RecatchSpots;
import net.johnceo.sparklingmutuals.hunting.TraderWatch;
import net.johnceo.sparklingmutuals.safari.SafariAssist;
import net.johnceo.sparklingmutuals.safari.SafariBiome;
import net.johnceo.sparklingmutuals.safari.SafariChatFilter;
import net.johnceo.sparklingmutuals.safari.SafariRoster;
import net.johnceo.sparklingmutuals.safari.SafariTracking;
import net.johnceo.sparklingmutuals.safari.SafariSparklingMode;
import net.johnceo.sparklingmutuals.safari.structures.WallSummary;
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** The CritterMod features living in Sparkling Mutuals, the second HUD, and the Hunting Helper styled settings. */
final class HuntingGameChecks {
    private static String share(String species, String catcher) {
        return "LOOT SHARE! You received a " + species + " Shard from [VIP] " + catcher + " catching a " + species + "!";
    }
    private static final String GAZER = "CAPTURE! You caught a Gazer and gained a Gazer Shard!";

    private static void hidden(String text) {
        ClientReceiveMessageEvents.GAME_CANCELED.invoker().onReceiveGameMessageCanceled(Component.literal(text), false);
    }
    private static void visible(String text) {
        ClientReceiveMessageEvents.GAME.invoker().onReceiveGameMessage(Component.literal(text), false);
    }
    private static HudPanel missing() {
        // SM's own missing panel; Hunting Helper's extras decorate it through SafariPanels.missingExtras.
        return SafariPanels.INSTANCE.missing(SafariTracking.INSTANCE.getRun(), SafariBiome.HAUNTED, true, 0, 0,
                new WallSummary(List.of()), SafariSparklingMode.INSTANCE.getState());
    }
    private static String value(HudPanel panel, String label) {
        return panel.getRows().stream().filter(row -> row.getLabel().equals(label)).map(HudRow::getValue).findFirst().orElse("<absent>");
    }
    private static void expect(HudPanel panel, String label, String value) {
        String actual = value(panel, label);
        if (!java.util.Objects.equals(actual, value)) throw new AssertionError(label + " expected " + value + " but displayed " + actual);
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    static void run(ClientGameTestContext c, TestSingleplayerContext world) {
        var server = world.getServer();
        var home = c.computeOnClient(client -> client.gui.screen());
        String restore = c.computeOnClient(client -> String.format(java.util.Locale.ROOT, "tp @p %.4f %.4f %.4f %.2f %.2f",
                client.player.getX(), client.player.getY(), client.player.getZ(), client.player.getYRot(), client.player.getXRot()));
        boolean filter = c.computeOnClient(client -> ConfigManager.INSTANCE.getHideCaptureChat());
        c.runOnClient(client -> {
            var root = ClientCommands.getActiveDispatcher().getRoot();
            for (String gone : List.of("cm", "ct", "critters"))
                require(root.getChild(gone) == null, "The field journal command /" + gone + " must be gone");
            require(root.getChild("hh").getChild("journal") != null, "Legacy journal shortcut missing");
            require(root.getChild("hh").getChild("run") != null && root.getChild("hh").getChild("safari") != null, "Missing /hh run or /hh safari");
            require(!FabricLoader.getInstance().isModLoaded("crittermod") && !HuntingOutput.getCritterModLoaded(), "Unexpected standalone CritterMod");
            var config = HuntingConfig.INSTANCE;
            require(config.getMissingExtras() && !config.getUniquesOnly() && config.getMissingLocated() && config.getMissingAlsoHere() && config.getTradeHud(), "Missing panel extras defaults");
            require(java.util.Arrays.stream(net.johnceo.sparklingmutuals.hud.SafariHud.values()).noneMatch(h -> h.name().equals("HUNTING_MISSING")), "The separate missing HUD must be gone");
            require(SafariPanels.missingExtras != null, "Missing panel extras not installed");
            require(config.getTradeAudience() == Audience.NOBODY && config.getMilestoneAudience() == Audience.NOBODY
                    && config.getGemzieAudience() == Audience.NOBODY, "Broadcasts must be opt-in");
        });

        // Stand on a Haunted perch so the missing panel has an area to report.
        server.runCommand("fill 12 70 -63 24 70 -51 minecraft:smooth_stone");
        server.runCommand("tp @p 15 71 -57 -90 10");
        c.runOnClient(client -> {
            client.gui.setScreen(null);
            HuntingConfig.INSTANCE.setUniquesOnly(false);
            HuntingConfig.INSTANCE.setHideonwallMarks(true);
            HuntingConfig.INSTANCE.setRecatchMarks(true);
            ConfigManager.INSTANCE.setHideCaptureChat(true);
            SafariAssist.INSTANCE.markEntered();
            SafariTracking.INSTANCE.getLedger().enter(System.currentTimeMillis());
            require(SafariChatFilter.INSTANCE.hidden(GAZER, true, SafariChatConfig.INSTANCE::hidden), "Expected the chat helper to hide captures");
        });
        c.waitFor(client -> SafariAssist.INSTANCE.getBiome() == SafariBiome.HAUNTED);

        // One Gazer through the chat helper's visible path, one canceled by "another mod".
        c.runOnClient(client -> { visible(GAZER); hidden(GAZER); visible(share("Areita", "Moss")); });
        c.waitFor(client -> SafariTracking.INSTANCE.getRun() != null && SafariTracking.INSTANCE.getRun().count("Gazer") == 2);
        c.runOnClient(client -> {
            HudPanel panel = missing();
            expect(panel, "Gazer", "2/4");
            expect(panel, "Areita", "<absent>");
            require(panel.getTitle().equals("Haunted Biome — 9 left"), "Unexpected title " + panel.getTitle());
        });
        c.waitTicks(6);
        c.takeScreenshot("missing-panel-extras-gazer-2-of-4");
        c.runOnClient(client -> {
            HuntingConfig.INSTANCE.setUniquesOnly(true);
            expect(missing(), "Gazer", "<absent>");
            require(missing().getTitle().equals("Haunted Biome — 8 left"), "Uniques only should tick Gazer after one catch");
        });
        c.waitTicks(3);
        c.takeScreenshot("missing-panel-extras-uniques-only");
        c.runOnClient(client -> {
            HuntingConfig.INSTANCE.setUniquesOnly(false);
            visible(GAZER);
            expect(missing(), "Gazer", "3/4");
            hidden(share("Gazer", "Moss"));
            expect(missing(), "Gazer", "<absent>");
            require(SafariTracking.INSTANCE.getRun().count("Gazer") == 4, "Gazer count");
        });

        // Capsule attempts, escapes and the recatch pin, against a real labelled mob.
        server.runCommand("summon silverfish 17 71 -57 {NoAI:1b,NoGravity:1b,Silent:1b,Tags:['hunting_fixture'],CustomName:'Rockmite'}");
        c.waitFor(client -> CritterEntities.all().stream().anyMatch(s -> s.critter().getName().equals("Rockmite") && s.mob() != null));
        c.runOnClient(client -> {
            visible("You threw a Critter Capsule at the Rockmite!");
            require(RecatchSpots.pinned() != null && RecatchSpots.pinnedCritter().getName().equals("Rockmite"), "Throw did not pin the mob");
            visible("The Rockmite escaped your Critter Capsule!");
            require(RecatchSpots.pinned() != null, "Escape lost the recatch pin");
            var run = SafariTracking.INSTANCE.getRun();
            require(HuntingRuns.INSTANCE.attempts(run, "Rockmite") == 1 && HuntingRuns.INSTANCE.escapes(run, "Rockmite") == 1, "Attempts per species");
            require(HideonwallSpots.all().size() == 8, "Hideonwall perches");
        });
        c.waitTicks(4);
        c.takeScreenshot("hunting-recatch-and-perch-marks");
        c.runOnClient(client -> {
            visible("CAPTURE! You caught a Rockmite and gained a Rockmite Shard!");
            require(RecatchSpots.pinned() == null, "Capture kept its pin");
            var by = HuntingRuns.INSTANCE.catchers(SafariTracking.INSTANCE.getRun(), "Areita", client.getUser().getName());
            require(by.size() == 1 && by.getFirst().getFirst().equals("Moss"), "Who caught what: " + by);
        });
        server.runCommand("kill @e[tag=hunting_fixture]");

        // Hunter trades, including a partymate's CritterMod-format share with a lower-case channel prefix.
        c.runOnClient(client -> {
            visible("[NPC] Hunter Harry: Say, do you have a use for a Nozzlenose Shard? I found it lying around...");
            visible("[NPC] Hunter Harry: I'll give you it in exchange for a Yogi Berry!");
            require(TraderWatch.found().size() == 1, "Hunter dialogue lost");
            // Floor-drop filter: the trade's Yogi Berry stays highlighted, rewards are filtered.
            HuntingConfig.INSTANCE.setQuestItemsOnly(true);
            HuntingConfig.INSTANCE.setHideCollectedQuestItems(true);
            var drops = net.johnceo.sparklingmutuals.hunting.QuestDropFilter.INSTANCE;
            require(Boolean.TRUE.equals(drops.decide(net.johnceo.sparklingmutuals.safari.resources.FloorDropType.BERRY)), "Trade item must stay highlighted");
            require(Boolean.FALSE.equals(drops.decide(net.johnceo.sparklingmutuals.safari.resources.FloorDropType.GENERIC)), "Rewards must be filtered");
            require(drops.decide(net.johnceo.sparklingmutuals.safari.resources.FloorDropType.LIME) == null, "Uncollected gem left to SM");
            visible("party > [VIP] Moss: Hunter Dennis (Icy -106 87 -7): Gimmiegold Shard for Purple Gem");
            visible("Party > [VIP] Moss: Hunter Dennis (Icy 999999999999 87 -7): Gimmiegold Shard for Purple Gem");
            require(TraderWatch.found().size() == 2 && TraderWatch.found().getLast().spot().x() == -106, "Shared trade lost or overwritten");
            require(ChatQueue.pendingCount() == 0, "Trades must stay local by default");
        });
        c.waitTicks(3);
        c.takeScreenshot("hunting-trades-hud");
        c.runOnClient(client -> {
            visible("You received a Nozzlenose Shard!");
            visible("PARTY > Moss: Trade taken: Gimmiegold Shard");
            require(TraderWatch.found().isEmpty(), "Taken trades were not retired");
            require(net.johnceo.sparklingmutuals.hunting.QuestDropFilter.INSTANCE.decide(net.johnceo.sparklingmutuals.safari.resources.FloorDropType.BERRY) == null,
                    "A taken trade no longer forces its item");
            HuntingConfig.INSTANCE.setQuestItemsOnly(false);
            HuntingConfig.INSTANCE.setHideCollectedQuestItems(false);
        });

        // Whole-run announcements, counted like the second HUD.
        c.runOnClient(client -> {
            for (var critter : SafariRoster.INSTANCE.getAll()) {
                if (critter.getName().equals("Macaw")) continue;
                int have = SafariTracking.INSTANCE.getRun().count(critter.getName());
                for (int i = have; i < critter.getQuota(); i++) hidden(share(critter.getName(), "Moss"));
            }
        });
        c.waitFor(client -> "Everything except Macaw done!".equals(HuntingAnnouncer.getLast()));
        c.waitTicks(4);
        c.takeScreenshot("hunting-everything-except-macaw");
        c.runOnClient(client -> hidden(share("Macaw", "Moss")));
        c.waitFor(client -> "Everything Done!".equals(HuntingAnnouncer.getLast()));
        c.waitTicks(4);
        c.takeScreenshot("hunting-everything-done");

        // Screens: the run report and the HUD editor with the new panels.
        c.runOnClient(client -> client.gui.setScreen(new RunReportScreen(null)));
        c.waitForScreen(RunReportScreen.class);
        c.waitTicks(3);
        c.takeScreenshot("hunting-run-report");
        c.runOnClient(client -> client.gui.setScreen(new net.johnceo.sparklingmutuals.contest.ContestGui(null)));
        c.waitTicks(3);
        c.takeScreenshot("hud-editor-with-hunting-panels");

        // Located and remaining creatures for all 37 species, then a real Cavern scene (upstream #10).
        c.runOnClient(client -> client.gui.setScreen(null));
        SafariRosterGameChecks.run(c);
        CavernPopulationGameChecks.run(c, world);

        server.runCommand(restore);
        c.runOnClient(client -> {
            SafariTracking.INSTANCE.finish();
            SafariAssist.INSTANCE.reset();
            HuntingConfig.INSTANCE.setHideonwallMarks(false);
            HuntingConfig.INSTANCE.setRecatchMarks(false);
            ConfigManager.INSTANCE.setHideCaptureChat(filter);
            client.gui.setScreen(home);
        });
        c.waitForScreen(HuntingHomeScreen.class);
        settingsScreens(c);
        System.out.println("PASS: missing panel extras (quota, uniques only, located), filtered catches, trades, recatch, announcements, report and HH settings");
    }

    /** /hh sections open Sparkling Mutuals' settings in Hunting Helper's style, and so does /sm. */
    private static void settingsScreens(ClientGameTestContext c) {
        c.clickScreenButton("Hunting & journal");
        c.waitForScreen(SafariSettingsScreen.class);
        c.runOnClient(client -> require(((SafariSettingsScreen) client.gui.screen()).area().equals("Hunting"), "Hunting section opened elsewhere"));
        c.waitTicks(4);
        c.takeScreenshot("hh-safari-settings-hunting");
        c.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        c.waitForScreen(HuntingHomeScreen.class);
        c.clickScreenButton("This run");
        c.waitForScreen(RunReportScreen.class);
        c.clickScreenButton("Back");
        c.waitForScreen(HuntingHomeScreen.class);
        c.runOnClient(client -> client.gui.setScreen(null));
        c.waitTicks(2);
        // Fabric routes the command to the client dispatcher, as typing /sm does.
        c.runOnClient(client -> client.player.connection.sendCommand("sm"));
        c.waitForScreen(SafariSettingsScreen.class);
        c.waitTicks(4);
        c.takeScreenshot("sm-command-hh-settings");
        c.clickScreenButton("ESP");
        c.waitTicks(2);
        c.runOnClient(client -> require(((SafariSettingsScreen) client.gui.screen()).area().equals("ESP"), "ESP area"));
        c.takeScreenshot("sm-settings-esp");
        c.clickScreenButton("Done");
        c.waitFor(client -> client.gui.screen() == null);
        c.waitTicks(2);
        // Fabric routes the command to the client dispatcher, as typing /hh does.
        c.runOnClient(client -> client.player.connection.sendCommand("hh"));
        c.waitForScreen(HuntingHomeScreen.class);
    }
}

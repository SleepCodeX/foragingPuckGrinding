package dev.sleepcodex.foragingpuckgrinding;

import dev.sleepcodex.foragingpuckgrinding.journal.client.*;
import dev.sleepcodex.foragingpuckgrinding.journal.data.Critters;
import dev.sleepcodex.foragingpuckgrinding.journal.session.SessionManager;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class FieldJournalGameChecks {
    static void run(ClientGameTestContext c, TestSingleplayerContext world) {
        c.runOnClient(client -> {
            if (FabricLoader.getInstance().isModLoaded("crittermod")) throw new AssertionError("Unexpected external critterMod");
            if (ConfigManager.get().display.showCoverage || ConfigManager.get().display.showMissing
                    || ConfigManager.get().party.trades() != CritterConfig.Broadcast.NONE)
                throw new AssertionError("Fresh setup must keep existing panels and announcements single");
            ConfigManager.get().profit.enabled = false;
            if (dev.sleepcodex.foragingpuckgrinding.mixin.HypixelSessionAccess.huntingHelper$isOnHypixel())
                throw new AssertionError("Unexpected online session in the packaged client");
            net.johnceo.sparklingmutuals.party.PartyManager.INSTANCE.requestPartyInfo();
            BazaarPrices.ageMillis(); // Resolve the bundled market adapter without contacting any service.
        });
        c.clickScreenButton("Field journal");
        c.waitForScreen(FieldJournalScreen.class);
        c.waitTicks(4);
        c.takeScreenshot("field-journal");
        c.clickScreenButton("World markers");
        c.clickScreenButton("Recatch pins · Off");
        c.runOnClient(client -> {
            if (!ConfigManager.get().display.recatchHelper || !java.nio.file.Files.isRegularFile(
                    FabricLoader.getInstance().getConfigDir().resolve("hunting-helper-field-journal.json")))
                throw new AssertionError("Recatch preference was not saved");
        });
        c.takeScreenshot("field-markers");
        c.clickScreenButton("Back");
        c.clickScreenButton("Marker colors");
        c.clickScreenButton("Unchecked perch");
        c.waitForScreen(JournalColorScreen.class);
        c.clickScreenButton("Save color");
        c.clickScreenButton("Back");
        c.clickScreenButton("Move journal panels");
        c.waitForScreen(HudEditorScreen.class);
        c.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        c.waitForScreen(FieldJournalScreen.class);
        c.runOnClient(client -> ClientReceiveMessageEvents.GAME.invoker().onReceiveGameMessage(
                Component.literal(client.getUser().getName() + " entered Critter Safari!"), false));
        world.getServer().runCommand("summon silverfish 0 -59 5 {NoAI:1b,NoGravity:1b,Silent:1b,Tags:['journal_fixture'],CustomName:'Rockmite'}");
        c.waitFor(client -> CritterEntities.all().stream().anyMatch(s -> s.critter().name().equals("Rockmite") && s.mob() != null));
        c.runOnClient(client -> {
            var visible = ClientReceiveMessageEvents.GAME.invoker();
            var hidden = ClientReceiveMessageEvents.GAME_CANCELED.invoker();
            visible.onReceiveGameMessage(Component.literal("You threw a Critter Capsule at the Rockmite!"), false);
            if (RecatchSpots.pinned() == null || !RecatchSpots.pinnedCritter().name().equals("Rockmite"))
                throw new AssertionError("Throw did not pin the observed mob");
            visible.onReceiveGameMessage(Component.literal("The Rockmite escaped your Critter Capsule!"), false);
            if (RecatchSpots.pinned() == null) throw new AssertionError("Escaped throw lost its recatch pin");
            if (HideonwallSpots.all().size() != 8 || !HideonwallSpots.all().contains(new net.minecraft.core.BlockPos(-24,71,-76)))
                throw new AssertionError("Missing corrected Haunted perch");
            hidden.onReceiveGameMessageCanceled(Component.literal("LOOT SHARE! You received 2x Rockmite Shard from Moss catching a Rockmite!"), false);
            visible.onReceiveGameMessage(Component.literal("[NPC] Hunter Harry: Say, do you have a use for a Nozzlenose Shard?"), false);
            visible.onReceiveGameMessage(Component.literal("[NPC] Hunter Harry: I'll give you it in exchange for a Yogi Berry!"), false);
            var run = SessionManager.current();
            var rockmite = Critters.byName("Rockmite");
            if (run == null || run.attempts(rockmite) != 1 || run.totalShards() != 2
                    || !run.catchersOf(rockmite).contains("Moss") || TraderWatch.found().size() != 1)
                throw new AssertionError("Field report or Hunter dialogue lost an event");
            visible.onReceiveGameMessage(Component.literal("You received a Nozzlenose Shard!"), false);
            visible.onReceiveGameMessage(Component.literal("Party > [VIP] Moss: Hunter Dennis (Icy -106 87 -7): Gimmiegold Shard for Purple Gem"), false);
            visible.onReceiveGameMessage(Component.literal("Party > [VIP] Moss: Hunter Dennis (Icy 999999999999 87 -7): Gimmiegold Shard for Purple Gem"), false);
            if (TraderWatch.found().size() != 1 || TraderWatch.found().getFirst().spot().x() != -106)
                throw new AssertionError("Party offer was lost or overwritten by malformed coordinates");
            visible.onReceiveGameMessage(Component.literal("Party > [VIP] Moss: Trade taken: Gimmiegold Shard"), false);
            if (!TraderWatch.found().isEmpty() || ChatQueue.pendingCount() != 0)
                throw new AssertionError("Trade retirement or local-only delivery failed");
        });
        c.runOnClient(client -> {
            ClientReceiveMessageEvents.GAME.invoker().onReceiveGameMessage(
                    Component.literal("CAPTURE! You caught a Rockmite and gained a Rockmite Shard!"), false);
            if (RecatchSpots.pinned() != null) throw new AssertionError("Successful capture kept its pin");
        });
        world.getServer().runCommand("kill @e[tag=journal_fixture]");
        c.clickScreenButton("Open field report");
        c.waitForScreen(CritterScreen.class);
        c.waitTicks(3);
        c.takeScreenshot("field-report");
        c.clickScreenButton("Stats");
        c.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        c.waitForScreen(FieldJournalScreen.class);
        c.clickScreenButton("Back");
        c.waitForScreen(HuntingHomeScreen.class);
        System.out.println("PASS: standalone field journal, filtered catches, Hunter offers, local-only delivery and report navigation");
    }
}

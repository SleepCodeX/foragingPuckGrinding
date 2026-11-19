package dev.sleepcodex.foragingpuckgrinding;

import java.io.IOException;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.entity.EntityTypes;
import org.lwjgl.glfw.GLFW;

/** Exercise the shipped screens, saved rules and real client entity associations together. */
final class CustomTrackerGameChecks {
    static void run(ClientGameTestContext c, TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("scoreboard objectives setdisplay sidebar");
        server.runCommand("time set night");
        open(c, "hh", HuntingHomeScreen.class);
        captureHome(c);
        c.runOnClient(client -> {
            var loader = FabricLoader.getInstance();
            for (String nested : java.util.List.of("sparkling-mutuals", "fabric-language-kotlin", "fabric-api")) {
                var container = loader.getModContainer(nested).orElseThrow();
                var host = container.getContainingMod().orElseThrow(() -> new AssertionError(nested + " must load from the Hunting Helper JAR"));
                if (!host.getMetadata().getId().equals(VexTracerClient.MOD_ID)) throw new AssertionError("Unexpected parent " + host);
            }
        });
        FieldJournalGameChecks.run(c, world);
        c.clickScreenButton("All Safari settings");
        c.waitForScreen(net.johnceo.sparklingmutuals.config.SafariConfigScreen.class);
        c.waitTicks(4);
        c.takeScreenshot("bundled-safari-settings");
        c.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        c.waitForScreen(HuntingHomeScreen.class);
        var catalog = c.computeOnClient(client -> new net.johnceo.sparklingmutuals.config.SettingsCatalog(
                new net.johnceo.sparklingmutuals.config.SafariSettings()));
        for (var section : HuntingHomeScreen.Section.values()) {
            if (catalog.locate(section.target()) == null) throw new AssertionError("Unknown Safari settings route " + section);
            c.clickScreenButton(section.label);
            c.waitForScreen(net.johnceo.sparklingmutuals.config.SafariConfigScreen.class);
            c.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
            c.waitForScreen(HuntingHomeScreen.class);
        }
        c.clickScreenButton("Hunting presets");
        c.waitForScreen(TracerSettingsScreen.class);
        c.clickScreenButton("Clear presets");
        c.clickScreenButton("Custom trackers");
        c.waitForScreen(CustomLibraryScreen.class);
        c.clickScreenButton("Add tracker");
        c.waitForScreen(CustomEditorScreen.class);
        c.takeScreenshot("custom-nametag-editor");
        enter(c, "rEv");
        c.clickScreenButton("Line Pink");
        c.clickScreenButton("Save tracker");
        c.waitForScreen(CustomLibraryScreen.class);
        c.clickScreenButton("On");
        c.clickScreenButton("Off");
        c.clickScreenButton("Add tracker");
        c.clickScreenButton("Mob type");
        c.takeScreenshot("custom-mob-type-editor");
        enter(c, "wither_skeleto");
        c.runOnClient(client -> {
            var save = client.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .filter(b -> b.getMessage().getString().equals("Save tracker")).findFirst().orElseThrow();
            if (save.active) throw new AssertionError("Unknown mob ID was accepted");
        });
        c.getInput().typeChars("n");
        c.clickScreenButton("Line Green");
        c.clickScreenButton("Save tracker");
        c.waitForScreen(CustomLibraryScreen.class);
        c.clickScreenButton("Add tracker");
        enter(c, "Honeybuzz");
        c.clickScreenButton("Line Gold");
        c.clickScreenButton("Save tracker");
        c.waitForScreen(CustomLibraryScreen.class);
        c.takeScreenshot("custom-tracker-library");
        c.clickScreenButton("Back");
        c.clickScreenButton("Done");
        String body = " {NoAI:1b,NoGravity:1b,PersistenceRequired:1b,Silent:1b}";
        server.runCommand("summon husk -8 -57 16" + body);
        server.runCommand("summon armor_stand -8 -55 16 {Invisible:1b,Marker:1b,NoGravity:1b,CustomName:'[Lv377] Atoned Horror 10M❤'}");
        server.runCommand("summon husk -5 -57 18 {NoAI:1b,NoGravity:1b,CustomName:'Revenant Sycophant'}");
        server.runCommand("summon husk -2 -57 20 {NoAI:1b,NoGravity:1b,CustomName:'Atoned Champion'}");
        server.runCommand("summon wither_skeleton 1 -57 18 {NoAI:1b,NoGravity:1b,CustomName:'Wither Guard'}");
        server.runCommand("summon wither_skeleton 4 -57 16 {NoAI:1b,NoGravity:1b,CustomName:'Wither Archer'}");
        server.runCommand("summon bee 7.0 -55 18.0" + body);
        server.runCommand("summon bee 7.95 -55 18.0" + body);
        server.runCommand("summon text_display 7.0 -53 18.0 {text:'[Lv24] Honeybuzz'}");
        server.runCommand("summon armor_stand 7.95 -53 18.0 {Invisible:1b,Marker:1b,NoGravity:1b,CustomName:'[Lv24] Pollendart'}");

        c.waitFor(client -> {
            int husks = 0, skeletons = 0, bees = 0;
            for (var mob : client.level.entitiesForRendering()) {
                int color = TrackedOutlines.colorFor(mob);
                if (mob.getType() == EntityTypes.HUSK) {
                    if (color != (mob.getX() < -3 ? OverlayColor.PINK.argb() : 0)) return false;
                    husks++;
                } else if (mob.getType() == EntityTypes.WITHER_SKELETON) {
                    if (color != OverlayColor.GREEN.argb()) return false;
                    skeletons++;
                } else if (mob.getType() == EntityTypes.BEE) {
                    if (color != (mob.getX() < 7.5 ? OverlayColor.GOLD.argb() : 0)) return false;
                    bees++;
                }
            }
            return husks == 3 && skeletons == 2 && bees == 2;
        });
        c.waitTicks(210);
        c.takeScreenshot("custom-tracked-mobs");
        open(c, "hh custom", CustomLibraryScreen.class);
        // Page through a compact library, then remove its final rule through the same widgets players use.
        c.getInput().resizeWindow(960,720);
        c.runOnClient(client -> { client.options.guiScale().set(3); client.resizeGui(); });
        c.waitTicks(3);
        c.getInput().pressKey(GLFW.GLFW_KEY_PAGE_DOWN);
        c.clickScreenButton("Edit");
        c.waitForScreen(CustomEditorScreen.class);
        c.runOnClient(client -> {
            if (!field(client.gui.screen()).getValue().equals("Honeybuzz")) throw new AssertionError("Wrong compact page");
        });
        c.clickScreenButton("Glow Off");
        c.clickScreenButton("Save tracker");
        c.waitForScreen(CustomLibraryScreen.class);
        c.clickScreenButton("Remove");
        c.clickScreenButton("Confirm");
        c.waitForScreen(CustomLibraryScreen.class);
        c.clickScreenButton("Back");
        c.clickScreenButton("Done");
        c.waitTicks(3);
        c.runOnClient(client -> {
            var config = new TracerConfig(FabricLoader.getInstance().getConfigDir().resolve("foragingpuckgrinding.properties"));
            try { config.load(); } catch (IOException e) { throw new AssertionError(e); }
            if (config.custom().all.size() != 2 || config.custom().name("Honeybuzz") != null
                    || config.custom().name("Atoned Horror") == null || !config.selectedTargets().isEmpty())
                throw new AssertionError("The custom editor did not preserve the saved state");
            for (var entity : client.level.entitiesForRendering())
                if (entity.getType() == EntityTypes.BEE && TrackedOutlines.colorFor(entity) != 0)
                    throw new AssertionError("Removed bee tracker still glows");
        });
        System.out.println("PASS: partial name, Tier V alias, text and stand labels, typed bodies, custom editor and library");
    }

    private static void captureHome(ClientGameTestContext c) {
        var home = c.computeOnClient(client -> (HuntingHomeScreen)client.gui.screen());
        var time = new java.util.concurrent.atomic.AtomicLong();
        c.runOnClient(client -> client.gui.setScreen(home.withClock(time::get)));
        for (int i = 0; i <= 10; i++) {
            time.set(i * 40_000_000L);
            c.waitTicks(1);
            c.takeScreenshot(String.format("home-opening-%02d", i));
        }
        c.takeScreenshot("home-dashboard");
        c.getInput().resizeWindow(960, 720);
        c.runOnClient(client -> { client.options.guiScale().set(3); client.resizeGui(); });
        c.waitTicks(2);
        c.takeScreenshot("compact-dashboard");
        c.getInput().resizeWindow(1280, 720);
        c.runOnClient(client -> { client.options.guiScale().set(2); client.resizeGui(); client.gui.setScreen(home); });
    }

    private static EditBox field(net.minecraft.client.gui.screens.Screen screen) {
        return screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
    }
    private static void enter(ClientGameTestContext c, String value) {
        c.runOnClient(client -> client.gui.screen().setFocused(field(client.gui.screen())));
        c.getInput().typeChars(value);
    }
    private static <T extends net.minecraft.client.gui.screens.Screen> void open(ClientGameTestContext c, String command, Class<T> screen) {
        c.getInput().pressKey(o -> o.keyChat);
        c.getInput().typeChars("/" + command);
        c.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
        c.waitForScreen(screen);
    }
}

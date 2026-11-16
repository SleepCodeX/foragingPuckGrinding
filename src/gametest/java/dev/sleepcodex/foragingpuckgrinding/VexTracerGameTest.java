package dev.sleepcodex.foragingpuckgrinding;

import com.mojang.blaze3d.platform.InputConstants;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

/** Runs only in the isolated test mod; never included in the distributed jar. */
public final class VexTracerGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder()
                .adjustSettings(settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            world.getServer().runCommand("tp @p 0 -59 0 0 0");
            world.getServer().runCommand("fill -12 -60 8 12 -48 9 minecraft:stone");
            world.getServer().runCommand("summon minecraft:vex -6 -54 18 {NoAI:1b}");
            world.getServer().runCommand("summon minecraft:vex 6 -54 18 {NoAI:1b}");
            world.getServer().runCommand("summon minecraft:vex 0 -51 22 {NoAI:1b}");
            world.getServer().runCommand("summon minecraft:pig 3 -59 5 {NoAI:1b}");
            world.getServer().runCommand("summon minecraft:parrot -10 -52 20 {NoAI:1b,NoGravity:1b,Variant:1}");
            world.getServer().runCommand("summon minecraft:parrot 10 -52 20 {NoAI:1b,NoGravity:1b,Variant:0}");
            world.getServer().runCommand("summon minecraft:allay 8 -51 22 {NoAI:1b,NoGravity:1b}");
            world.getServer().runOnServer(server -> {
                var board = server.getScoreboard();
                var objective = board.addObjective("hh_test", ObjectiveCriteria.DUMMY, Component.literal("SKYBLOCK"),
                        ObjectiveCriteria.RenderType.INTEGER, false, null);
                board.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
                board.getOrCreatePlayerScore(ScoreHolder.forNameOnly("location"), objective).set(1);
                var team = board.addPlayerTeam("hh_location");
                board.addPlayerToTeam("location", team);
                team.setPlayerPrefix(Component.literal("⏣ Torrhus Heights"));
            });
            world.getConnection().waitForChunksRender();
            context.getInput().lookAt(0, 0);
            command(context, "vextracers off");
            requirePurple(context.takeScreenshot("vex_tracers_off"), false);

            command(context, "vextracers on");
            Path vexScreenshot = context.takeScreenshot("vex_tracers_through_wall");
            requirePurple(vexScreenshot, true);

            context.getInput().pressKey(GLFW.GLFW_KEY_V);
            context.waitTicks(2);
            requirePurple(context.takeScreenshot("vex_tracers_keybind_off"), false);

            command(context, "huntinghelper");
            context.waitForScreen(TracerSettingsScreen.class);
            context.takeScreenshot("hunting_helper_intro");
            waitForIntro(context);
            requireMenuRows(context.takeScreenshot("hunting_helper_compact"));
            context.clickScreenButton("Tracers: OFF");
            context.clickScreenButton("Targets: Vex");
            context.clickScreenButton("Done");
            context.waitTicks(14);
            Path blueJayScreenshot = context.takeScreenshot("blue_jay_only");
            requirePurple(blueJayScreenshot, true);

            openMenu(context);
            context.clickScreenButton("Targets: Blue Jay");
            context.getInput().resizeWindow(1280, 720);
            context.waitTicks(3);
            waitForIntro(context);
            requireMenuRows(context.takeScreenshot("hunting_helper_menu"));
            context.clickScreenButton("Done");
            context.waitTicks(3);
            // Return to the same scene resolution before comparing the two target sets.
            context.getInput().resizeWindow(854, 480);
            context.waitTicks(3);
            Path bothScreenshot = context.takeScreenshot("both_targets");
            requirePurple(bothScreenshot, true);
            int bothPixels = purplePixels(bothScreenshot);
            if (bothPixels < Math.max(purplePixels(vexScreenshot), purplePixels(blueJayScreenshot)) + 60) {
                throw new AssertionError("Both mode did not render both independent target sets");
            }
            context.runOnClient(client -> {
                var saved = new TracerConfig(FabricLoader.getInstance().getConfigDir()
                        .resolve("foragingpuckgrinding.properties"));
                try { saved.load(); }
                catch (IOException exception) { throw new AssertionError("Could not reload menu choices", exception); }
                if (!saved.enabled() || saved.target() != TracerTarget.BOTH) {
                    throw new AssertionError("Menu choices did not persist together");
                }
            });
            world.getServer().runCommand("kill @e[type=minecraft:vex]");
            context.waitTicks(40);
            requirePurple(context.takeScreenshot("blue_jay_remains"), true);
            openMenu(context);
            context.clickScreenButton("Targets: Both");
            context.clickScreenButton("Done");
            context.waitTicks(3);
            requirePurple(context.takeScreenshot("vex_mode_excludes_blue_jay"), false);

            openMenu(context);
            context.clickScreenButton("Targets: Vex");
            context.clickScreenButton("Done");
            context.waitTicks(3);
            requirePurple(context.takeScreenshot("blue_jay_mode_restored"), true);
            world.getServer().runCommand("scoreboard objectives setdisplay sidebar");
            context.waitTicks(14);
            requirePurple(context.takeScreenshot("blue_parrot_outside_torrhus"), false);
            world.getServer().runCommand("scoreboard objectives setdisplay sidebar hh_test");
            context.waitTicks(14);
            requirePurple(context.takeScreenshot("blue_jay_in_torrhus"), true);
            world.getServer().runCommand("kill @e[type=minecraft:parrot]");
            context.waitTicks(40);
            requirePurple(context.takeScreenshot("targets_removed_allay_excluded"), false);
            command(context, "vextracers off");
        }
    }

    private static void openMenu(ClientGameTestContext context) {
        command(context, "huntinghelper");
        context.waitForScreen(TracerSettingsScreen.class);
        waitForIntro(context);
    }

    private static void waitForIntro(ClientGameTestContext context) {
        long until = System.nanoTime() + 700_000_000L;
        context.waitFor(client -> System.nanoTime() >= until);
    }

    private static void command(ClientGameTestContext context, String command) {
        context.getInput().pressKey(options -> options.keyChat);
        context.getInput().typeChars("/" + command);
        context.getInput().holdKeyFor(InputConstants.KEY_RETURN, 0);
        context.waitTicks(3);
    }

    private static void requirePurple(Path screenshot, boolean expected) {
        int purple = purplePixels(screenshot);
        if (expected ? purple < 100 : purple > 5) {
            throw new AssertionError("Expected tracers " + (expected ? "visible" : "absent")
                    + "; found " + purple + " purple scene pixels in " + screenshot);
        }
    }

    private static int purplePixels(Path screenshot) {
        try {
            BufferedImage image = ImageIO.read(screenshot.toFile());
            int purple = 0;
            // Exclude chat/action bar/HUD: count only the scene's upper and middle region.
            for (int y = 0; y < image.getHeight() * 2 / 3; y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getRGB(x, y);
                    int r = (pixel >> 16) & 255;
                    int g = (pixel >> 8) & 255;
                    int b = pixel & 255;
                    if (r > 140 && b > 170 && g < 150 && r > g * 1.3 && b > g * 1.5) purple++;
                }
            }
            return purple;
        } catch (IOException exception) {
            throw new AssertionError("Could not inspect screenshot " + screenshot, exception);
        }
    }

    private static void requireMenuRows(Path screenshot) {
        try {
            BufferedImage image = ImageIO.read(screenshot.toFile());
            int rowPixels = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int color = image.getRGB(x, y);
                    int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
                    if (r >= 25 && r <= 45 && g >= 33 && g <= 55 && b >= 47 && b <= 78) rowPixels++;
                }
            }
            if (rowPixels < image.getWidth() * image.getHeight() / 7) {
                throw new AssertionError("Menu rows were not fully visible in " + screenshot);
            }
        } catch (IOException exception) {
            throw new AssertionError("Could not inspect menu screenshot", exception);
        }
    }
}

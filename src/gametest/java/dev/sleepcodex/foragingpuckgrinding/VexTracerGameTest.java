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
            world.getConnection().waitForChunksRender();
            context.getInput().lookAt(0, 0);
            command(context, "vextracers off");
            requirePurple(context.takeScreenshot("vex_tracers_off"), false);

            command(context, "vextracers on");
            requirePurple(context.takeScreenshot("vex_tracers_through_wall"), true);

            context.getInput().pressKey(GLFW.GLFW_KEY_V);
            context.waitTicks(2);
            requirePurple(context.takeScreenshot("vex_tracers_keybind_off"), false);

            command(context, "vextracers on");
            world.getServer().runCommand("kill @e[type=minecraft:vex]");
            context.waitTicks(40);
            requirePurple(context.takeScreenshot("vex_tracers_removed"), false);
            command(context, "vextracers off");
        }
    }

    private static void command(ClientGameTestContext context, String command) {
        context.getInput().pressKey(options -> options.keyChat);
        context.getInput().typeChars("/" + command);
        context.getInput().holdKeyFor(InputConstants.KEY_RETURN, 0);
        context.waitTicks(3);
    }

    private static void requirePurple(Path screenshot, boolean expected) {
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
            if (expected ? purple < 100 : purple > 5) {
                throw new AssertionError("Expected tracers " + (expected ? "visible" : "absent")
                        + "; found " + purple + " purple scene pixels in " + screenshot);
            }
        } catch (IOException exception) {
            throw new AssertionError("Could not inspect screenshot " + screenshot, exception);
        }
    }
}

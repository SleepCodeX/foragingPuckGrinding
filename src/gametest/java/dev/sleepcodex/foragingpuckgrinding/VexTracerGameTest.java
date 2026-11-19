package dev.sleepcodex.foragingpuckgrinding;

import com.mojang.blaze3d.platform.InputConstants;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

/** Real packaged-client rendering and input checks; this mod is never distributed. */
public final class VexTracerGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().adjustSettings(s -> s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            try {
                world.getServer().runCommand("tp @p 0 -59 0 0 0");
                world.getServer().runCommand("fill -12 -60 8 12 -48 9 minecraft:stone");
                world.getServer().runCommand("summon minecraft:vex -6 -54 18 {NoAI:1b}");
                world.getServer().runCommand("summon minecraft:vex 6 -54 18 {NoAI:1b}");
                world.getServer().runCommand("summon minecraft:parrot -10 -52 20 {NoAI:1b,NoGravity:1b,Variant:1}");
                world.getServer().runCommand("summon minecraft:parrot -8 -52 22 {NoAI:1b,NoGravity:1b,Variant:0}");
                world.getServer().runCommand("summon minecraft:ghast 10 -53 24 {NoAI:1b,NoGravity:1b}");
                world.getServer().runCommand("summon minecraft:allay 8 -51 22 {NoAI:1b,NoGravity:1b}");
                world.getServer().runCommand("summon minecraft:pig 3 -59 5 {NoAI:1b}");
                world.getServer().runOnServer(server -> {
                    var board=server.getScoreboard();
                    var objective=board.addObjective("hh_test",ObjectiveCriteria.DUMMY,Component.literal("SKYBLOCK"),ObjectiveCriteria.RenderType.INTEGER,false,null);
                    board.setDisplayObjective(DisplaySlot.SIDEBAR,objective);
                    board.getOrCreatePlayerScore(ScoreHolder.forNameOnly("location"),objective).set(1);
                    var team=board.addPlayerTeam("hh_location");board.addPlayerToTeam("location",team);
                    team.setPlayerPrefix(Component.literal("⏣ Torrhus Heights"));
                });
                world.getConnection().waitForChunksRender();context.getInput().lookAt(0,0);
                command(context,"vextracers off");requireColors(context.takeScreenshot("trackers-off"),false,false,false);
                command(context,"vextracers on");requireColors(context.takeScreenshot("vex-purple-boxes"),true,false,false);
                context.getInput().pressKey(GLFW.GLFW_KEY_V);context.waitTicks(3);
                requireColors(context.takeScreenshot("keybind-off"),false,false,false);
                openMenu(context);
                context.runOnClient(client -> client.options.guiScale().set(3));
                context.getInput().resizeWindow(960,720);context.waitTicks(3);
                requireButtonsFit(context);context.takeScreenshot("tracker-menu-compact");
                context.clickScreenButton("Tracers: OFF");context.clickScreenButton("Select all");
                context.runOnClient(client -> client.options.guiScale().set(2));
                context.getInput().resizeWindow(1280,720);context.waitTicks(3);requireButtonsFit(context);
                context.takeScreenshot("tracker-menu-all");context.clickScreenButton("Done");context.waitTicks(14);
                requireColors(context.takeScreenshot("all-colored-lines-and-boxes"),true,true,true);

                openMenu(context);context.clickScreenButton("Vex: ON");context.clickScreenButton("Done");context.waitTicks(3);
                requireColors(context.takeScreenshot("blue-jay-and-hivethief"),false,true,true);
                openMenu(context);context.clickScreenButton("Blue Jay: ON");context.clickScreenButton("Done");context.waitTicks(3);
                requireColors(context.takeScreenshot("hivethief-gold-box"),false,false,true);
                context.runOnClient(client -> {
                    var saved=new TracerConfig(FabricLoader.getInstance().getConfigDir().resolve("foragingpuckgrinding.properties"));
                    try {saved.load();} catch(IOException e){throw new AssertionError(e);}
                    if(!saved.enabled()||!saved.selectedTargets().equals(EnumSet.of(TracerTarget.HIVE_THIEF)))throw new AssertionError("Independent choices did not persist");
                });
                context.getInput().pressKey(GLFW.GLFW_KEY_F1);context.waitTicks(3);
                requireColors(context.takeScreenshot("hidden-hud-hides-lines-and-boxes"),false,false,false);
                context.getInput().pressKey(GLFW.GLFW_KEY_F1);context.waitTicks(3);
                world.getServer().runCommand("scoreboard objectives setdisplay sidebar");context.waitTicks(14);
                requireColors(context.takeScreenshot("ghast-outside-torrhus"),false,false,false);
                world.getServer().runCommand("scoreboard objectives setdisplay sidebar hh_test");context.waitTicks(14);
                requireColors(context.takeScreenshot("hivethief-in-torrhus"),false,false,true);
                openMenu(context);context.clickScreenButton("Clear all");context.clickScreenButton("Done");context.waitTicks(3);
                requireColors(context.takeScreenshot("no-selected-mobs"),false,false,false);
                openMenu(context);context.clickScreenButton("Select all");context.clickScreenButton("Tracers: ON");context.clickScreenButton("Done");context.waitTicks(3);
                requireColors(context.takeScreenshot("master-hides-all-three"),false,false,false);
                command(context,"vextracers on");
                world.getServer().runCommand("kill @e[type=minecraft:ghast]");context.waitTicks(40);
                requireColors(context.takeScreenshot("ghast-removal-clears-gold"),true,true,false);
                world.getServer().runCommand("kill @e[type=minecraft:vex]");world.getServer().runCommand("kill @e[type=minecraft:parrot]");context.waitTicks(40);
                requireColors(context.takeScreenshot("unrelated-entities-excluded"),false,false,false);
                command(context,"vextracers off");
                System.out.println("PASS: three independent colored tracers/boxes, All/None, native input, persistence, F1, location gating and removal");
            } finally {
                // Fabric API #5623: keep processing the halt task before its tick phaser parks.
                var server=context.computeOnClient(Minecraft::getSingleplayerServer);
                if(server!=null){long deadline=System.nanoTime()+30_000_000_000L;server.execute(()->server.managedBlock(()->!server.isRunning()||System.nanoTime()>=deadline));}
            }
        }
    }
    private static void openMenu(ClientGameTestContext c) {
        command(c,"huntinghelper");c.waitForScreen(TracerSettingsScreen.class);
        long until=System.nanoTime()+220_000_000L;c.waitFor(client->System.nanoTime()>=until);
    }
    private static void requireButtonsFit(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var buttons = client.gui.screen().children().stream()
                    .filter(net.minecraft.client.gui.components.Button.class::isInstance)
                    .map(net.minecraft.client.gui.components.Button.class::cast).toList();
            if (buttons.size() != 7) throw new AssertionError("Master, three trackers and three footer controls must be reachable");
            int width = client.getWindow().getGuiScaledWidth(), height = client.getWindow().getGuiScaledHeight();
            for (int i = 0; i < buttons.size(); i++) {
                var a = buttons.get(i);
                if (a.getX() < 0 || a.getY() < 0 || a.getRight() > width || a.getBottom() > height)
                    throw new AssertionError("A tracker control is clipped");
                for (int j = i + 1; j < buttons.size(); j++) {
                    var b = buttons.get(j);
                    if (a.getX() < b.getRight() && a.getRight() > b.getX() && a.getY() < b.getBottom() && a.getBottom() > b.getY())
                        throw new AssertionError("Tracker controls overlap");
                }
            }
        });
    }

    private static void command(ClientGameTestContext c,String command) {
        c.getInput().pressKey(o->o.keyChat);c.getInput().typeChars("/"+command);
        c.getInput().holdKeyFor(InputConstants.KEY_RETURN,0);c.waitTicks(3);
    }
    private static void requireColors(Path screenshot,boolean vex,boolean blueJay,boolean hiveThief) {
        try {
            BufferedImage image=ImageIO.read(screenshot.toFile());int[] count=new int[3];
            for(int y=0;y<image.getHeight()*2/3;y++)for(int x=0;x<image.getWidth();x++){
                int p=image.getRGB(x,y),r=p>>16&255,g=p>>8&255,b=p&255;
                if(r>140&&b>170&&g<150&&r>g*1.3&&b>g*1.5)count[0]++;
                if(r<100&&g>190&&b>220)count[1]++;
                if(r>220&&g>160&&g<225&&b<125)count[2]++;
            }
            boolean[] expected={vex,blueJay,hiveThief};
            for(int i=0;i<3;i++)if(expected[i]?count[i]<80:count[i]>5)throw new AssertionError("Mob color "+i+" expected="+expected[i]+" pixels="+count[i]+" in "+screenshot);
        } catch(IOException e){throw new AssertionError(e);}
    }
}

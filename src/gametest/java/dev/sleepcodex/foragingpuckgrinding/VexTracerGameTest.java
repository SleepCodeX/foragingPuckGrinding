package dev.sleepcodex.foragingpuckgrinding;

import com.mojang.blaze3d.platform.InputConstants;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;
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
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.minecraft.world.entity.EntityTypes;
import org.lwjgl.glfw.GLFW;

/** Real packaged-client rendering and input checks; this mod is never distributed. */
public final class VexTracerGameTest implements FabricClientGameTest {
    private volatile int renderedVexGlow, renderedGhastGlow;
    @Override public void runTest(ClientGameTestContext context) {
        LevelExtractionEvents.END_EXTRACTION.register(extraction -> {
            int vex = 0, ghast = 0;
            for (var state : extraction.levelState().entityRenderStates) {
                if (state.entityType == EntityTypes.VEX) vex = state.outlineColor;
                if (state.entityType == EntityTypes.GHAST) ghast = state.outlineColor;
            }
            renderedVexGlow = vex; renderedGhastGlow = ghast;
        });
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
                context.getInput().resizeWindow(960,720);
                context.runOnClient(client -> {client.options.guiScale().set(3);client.resizeGui();});context.waitTicks(3);
                context.runOnClient(client -> {
                    if(client.getWindow().getGuiScaledWidth()!=320)throw new AssertionError("Compact GUI width="+client.getWindow().getGuiScaledWidth()+", option="+client.options.guiScale().get());
                });
                requireButtonsFit(context);requireKeyboardReachability(context);
                context.getInput().pressKey(GLFW.GLFW_KEY_HOME);context.takeScreenshot("tracker-menu-compact");
                context.clickScreenButton("Tracers: OFF");context.clickScreenButton("Select all");
                context.getInput().resizeWindow(1280,720);
                context.runOnClient(client -> {client.options.guiScale().set(2);client.resizeGui();});context.waitTicks(3);requireButtonsFit(context);
                captureOpening(context);
                context.takeScreenshot("tracker-menu-all");
                requireKeyboardReachability(context);
                context.clickScreenButton("Vex line: Purple");
                for(int i=0;i<3;i++)context.getInput().scroll(-1);
                context.getInput().scroll(1);
                context.takeScreenshot("line-color-dropdown");
                context.clickScreenButton("Green");
                context.clickScreenButton("Vex glow: Match line");
                context.getInput().pressKey(GLFW.GLFW_KEY_END);
                for(int i=0;i<3;i++)context.getInput().pressKey(GLFW.GLFW_KEY_UP);
                context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
                context.clickScreenButton("Done");context.waitTicks(3);
                requireOutlines(context,OverlayColor.PINK.argb(),OverlayColor.GOLD.argb());
                context.takeScreenshot("independent-green-line-pink-vex-glow");
                context.runOnClient(client -> {
                    var saved=new TracerConfig(FabricLoader.getInstance().getConfigDir().resolve("foragingpuckgrinding.properties"));
                    try {saved.load();}catch(IOException e){throw new AssertionError(e);}
                    if(saved.lineChoice(TracerTarget.VEX)!=OverlayColor.GREEN||saved.glowChoice(TracerTarget.VEX)!=OverlayColor.PINK)
                        throw new AssertionError("Independent line and glow colors did not persist");
                });
                openMenu(context);context.clickScreenButton("Vex glow: Pink");
                context.getInput().pressKey(GLFW.GLFW_KEY_HOME);context.getInput().pressKey(GLFW.GLFW_KEY_DOWN);
                context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
                context.clickScreenButton("Done");context.waitTicks(3);
                requireOutlines(context,0,OverlayColor.GOLD.argb());
                context.takeScreenshot("vex-glow-off-keeps-line-and-box");
                openMenu(context);context.clickScreenButton("Vex line: Green");
                context.getInput().pressKey(GLFW.GLFW_KEY_HOME);context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
                context.clickScreenButton("Vex glow: Off");
                context.getInput().pressKey(GLFW.GLFW_KEY_HOME);context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
                context.clickScreenButton("Done");context.waitTicks(14);
                requireOutlines(context,OverlayColor.PURPLE.argb(),OverlayColor.GOLD.argb());
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
                requireOutlines(context,0,0);
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
                requireOutlines(context,0,0);
                command(context,"vextracers on");
                world.getServer().runCommand("kill @e[type=minecraft:ghast]");context.waitTicks(40);
                requireColors(context.takeScreenshot("ghast-removal-clears-gold"),true,true,false);
                world.getServer().runCommand("kill @e[type=minecraft:vex]");world.getServer().runCommand("kill @e[type=minecraft:parrot]");context.waitTicks(40);
                requireColors(context.takeScreenshot("unrelated-entities-excluded"),false,false,false);
                command(context,"vextracers off");
                System.out.println("PASS: native colored model outlines through walls, independent saved line/glow colors, scrollable dropdown keyboard controls, Hivethief crossfade, tracker selection, F1, location gating and removal");
            } finally {
                // Fabric API #5623: keep processing the halt task before its tick phaser parks.
                var server=context.computeOnClient(Minecraft::getSingleplayerServer);
                if(server!=null){long deadline=System.nanoTime()+30_000_000_000L;server.execute(()->server.managedBlock(()->!server.isRunning()||System.nanoTime()>=deadline));}
            }
        }
    }
    private static void openMenu(ClientGameTestContext c) {
        command(c,"huntinghelper");c.waitForScreen(TracerSettingsScreen.class);
        long until=System.nanoTime()+MenuOpening.DURATION_NANOS;c.waitFor(client->System.nanoTime()>=until);
    }
    private void requireOutlines(ClientGameTestContext context,int vex,int ghast) {
        context.waitFor(client -> renderedVexGlow==vex && renderedGhastGlow==ghast);
    }
    private static void captureOpening(ClientGameTestContext context) {
        var original=context.computeOnClient(client -> (TracerSettingsScreen)client.gui.screen());
        AtomicLong time=new AtomicLong();
        context.runOnClient(client -> client.gui.setScreen(original.withClock(time::get)));
        for(int frame=0;frame<=14;frame++) {
            time.set(frame*40_000_000L);context.waitTicks(1);
            context.takeScreenshot(String.format("menu-opening-%02d",frame));
        }
        context.runOnClient(client -> client.gui.setScreen(original));
    }
    private static void requireKeyboardReachability(ClientGameTestContext context) {
        HashSet<String> reached=new HashSet<>();
        context.runOnClient(client -> client.gui.screen().setFocused(null));
        for(int i=0;i<13;i++) {
            context.getInput().pressKey(GLFW.GLFW_KEY_TAB);
            context.runOnClient(client -> {
                var screen=client.gui.screen();
                if(!(screen.getFocused() instanceof net.minecraft.client.gui.components.Button button)||!button.visible)
                    throw new AssertionError("Keyboard focus was not revealed");
                reached.add(button.getMessage().getString());
            });
        }
        if(reached.size()!=13)throw new AssertionError("Not every tracker/color/footer control is keyboard reachable: "+reached);
    }
    private static void requireButtonsFit(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var buttons = client.gui.screen().children().stream()
                    .filter(net.minecraft.client.gui.components.Button.class::isInstance)
                    .map(net.minecraft.client.gui.components.Button.class::cast).filter(b -> b.visible).toList();
            if (buttons.size() < 5) throw new AssertionError("Visible menu controls must remain reachable");
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

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
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
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
    private volatile int renderedParchedGlow, renderedPlainParchedGlow, renderedPollendartTint, renderedPlainBeeTint;
    private volatile boolean renderedFarPollendart, renderedFarPlainBee;
    @Override public void runTest(ClientGameTestContext context) {
        LevelExtractionEvents.END_EXTRACTION.register(extraction -> {
            int vex = 0, ghast = 0, parched = 0, plainParched = 0, pollendart = 0, plainBee = 0;
            boolean farPollendart = false, farPlainBee = false;
            for (var state : extraction.levelState().entityRenderStates) {
                if (state.entityType == EntityTypes.VEX) vex = state.outlineColor;
                if (state.entityType == EntityTypes.GHAST) ghast = state.outlineColor;
                if (state.entityType == EntityTypes.PARCHED) {
                    if (near(state.x, -4)) parched = state.outlineColor; else plainParched = state.outlineColor;
                }
                if (state.entityType == EntityTypes.BEE) {
                    // The tint color travels in the render state; it is what the fill pass submits.
                    int tint = ((HighlightState) state).huntingHelper$glow();
                    if (state.z > 50) { if (near(state.x, 2)) farPollendart = true; else farPlainBee = true; }
                    else if (near(state.x, 4)) pollendart = tint; else plainBee = tint;
                }
            }
            renderedVexGlow = vex; renderedGhastGlow = ghast;
            renderedParchedGlow = parched; renderedPlainParchedGlow = plainParched;
            renderedPollendartTint = pollendart; renderedPlainBeeTint = plainBee;
            renderedFarPollendart = farPollendart; renderedFarPlainBee = farPlainBee;
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
                    board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(" "),objective).set(1);
                    var team=board.addPlayerTeam("hh_location");board.addPlayerToTeam(" ",team);
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
                requireAllTrackersVisible(context);
                context.takeScreenshot("tracker-menu-all");
                context.getInput().resizeWindow(1920,1080);
                context.runOnClient(client -> {client.options.guiScale().set(2);client.resizeGui();});context.waitTicks(3);
                requireAllTrackersVisible(context);requireButtonsFit(context);context.takeScreenshot("tracker-menu-large");
                context.getInput().resizeWindow(1280,720);
                context.runOnClient(client -> {client.options.guiScale().set(2);client.resizeGui();});context.waitTicks(3);
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
                    if(!saved.enabled()||!saved.selectedTargets().equals(EnumSet.of(TracerTarget.HIVE_THIEF,TracerTarget.PARCHED,TracerTarget.POLLENDART)))throw new AssertionError("Independent choices did not persist");
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
                requireNametagDetection(context,world);
                command(context,"vextracers off");
                System.out.println("PASS: native colored model outlines through walls, independent saved line/glow colors, scrollable dropdown keyboard controls, Hivethief crossfade, tracker selection, F1, location gating and removal, nametag detection for Parched and Pollendart, tint fill, far glow, /hh menu and debug");
            } finally {
                // Fabric API #5623: keep processing the halt task before its tick phaser parks.
                var server=context.computeOnClient(Minecraft::getSingleplayerServer);
                if(server!=null){long deadline=System.nanoTime()+30_000_000_000L;server.execute(()->server.managedBlock(()->!server.isRunning()||System.nanoTime()>=deadline));}
            }
        }
    }
    private void requireNametagDetection(ClientGameTestContext context, TestSingleplayerContext world) {
        var server=world.getServer();
        context.runOnClient(client -> { if (HuntingDebug.enabled()) throw new AssertionError("Debug starts disabled"); });
        command(context,"hh debug");
        // Hypixel shows a SkyBlock mob's name on an invisible armor stand about two blocks above it.
        server.runCommand("summon minecraft:parched -4 -57 14 {NoAI:1b,NoGravity:1b,Silent:1b,PersistenceRequired:1b}");
        server.runCommand(stand(-4,-55,14,"[Lv58] Parched 25,000/25,000\u2764"));
        server.runCommand("summon minecraft:parched -9 -57 20 {NoAI:1b,NoGravity:1b,Silent:1b,PersistenceRequired:1b}");
        server.runCommand("summon minecraft:bogged 0 -57 20 {NoAI:1b,NoGravity:1b,Silent:1b,PersistenceRequired:1b}");
        server.runCommand("effect give @e[type=minecraft:parched] minecraft:fire_resistance infinite 0 true");
        server.runCommand("effect give @e[type=minecraft:bogged] minecraft:fire_resistance infinite 0 true");
        server.runCommand("summon minecraft:bee 4 -56 14 {NoAI:1b,NoGravity:1b,Silent:1b}");
        server.runCommand(stand(4,-54,14,"[Lv24] Pollendart"));
        server.runCommand("summon minecraft:bee 9 -56 14 {NoAI:1b,NoGravity:1b,Silent:1b}");
        server.runCommand(stand(12,-52,30,"[Lv5] Parched"));
        // Far beyond vanilla's size-based bee cull (about 40 blocks): only the tracked bee may still render.
        server.runCommand("summon minecraft:bee 2 -55 64 {NoAI:1b,NoGravity:1b,Silent:1b}");
        server.runCommand(stand(2,-53,64,"[Lv24] Pollendart"));
        server.runCommand("summon minecraft:bee -2 -55 64 {NoAI:1b,NoGravity:1b,Silent:1b}");
        int red=OverlayColor.RED.argb(), green=OverlayColor.GREEN.argb();
        int fills=HuntingDebug.FILLS.get();
        context.waitFor(client -> {
            int tagged=0, plain=0, bogged=0, bee=0, plainBee=0, fallback=0;
            for (var entity : client.level.entitiesForRendering()) {
                if (entity.getZ() > 50) continue;
                int color=TrackedOutlines.colorFor(entity);
                if (entity.getType()==EntityTypes.PARCHED) { if (near(entity.getX(),-4)) tagged=color; else plain=color; }
                if (entity.getType()==EntityTypes.BOGGED) bogged=color;
                if (entity.getType()==EntityTypes.BEE) { if (near(entity.getX(),4)) bee=color; else plainBee=color; }
                if (entity.getType()==EntityTypes.ARMOR_STAND && near(entity.getX(),12)) fallback=TrackedOutlines.targetFor(entity)==TracerTarget.PARCHED && color==0 ? red : -1;
            }
            return tagged==red && plain==0 && bogged==0 && bee==green && plainBee==0 && fallback==red;
        });
        context.waitFor(client -> renderedParchedGlow==red && renderedPlainParchedGlow==0
                && renderedPollendartTint==green && renderedPlainBeeTint==0
                && renderedFarPollendart && !renderedFarPlainBee && HuntingDebug.FILLS.get()!=fills);
        context.waitTicks(210); // Let command feedback fade out of the public visual guide.
        context.takeScreenshot("nametag-parched-and-pollendart");
        // Duplicate nametags must not produce duplicate candidates. Keep the original Hypixel name rules.
        server.runCommand(stand(4,-54,14,"[Lv24] Pollendart"));
        context.waitTicks(3);
        context.runOnClient(client -> {
            int count=0;
            for (var entity:client.level.entitiesForRendering())
                if (entity.getType()==EntityTypes.BEE && near(entity.getX(),4)
                        && TrackedOutlines.targetFor(entity)==TracerTarget.POLLENDART) count++;
            if(count!=1)throw new AssertionError("Duplicate tags duplicated a tracked body");
        });
        // Both new targets follow the same location and master/F1 gates as the existing Torrhus trackers.
        context.getInput().pressKey(GLFW.GLFW_KEY_F1);context.waitTicks(3);
        context.waitFor(client -> renderedParchedGlow==0 && renderedPollendartTint==0);
        context.getInput().pressKey(GLFW.GLFW_KEY_F1);context.waitTicks(3);
        server.runCommand("scoreboard objectives setdisplay sidebar");context.waitTicks(14);
        requireNoCustomGlow(context);
        server.runCommand("scoreboard objectives setdisplay sidebar hh_test");context.waitTicks(14);
        context.waitFor(client -> renderedParchedGlow==red && renderedPollendartTint==green);
        // Cross a spatial-column boundary; moving mobs must be found on the next discovery tick.
        server.runCommand("tp @e[type=minecraft:bee,x=4,y=-56,z=14,distance=..1] 5.0 -56 14.0");
        server.runCommand("tp @e[type=minecraft:armor_stand,x=4,y=-54,z=14,distance=..1] 5.0 -54 14.0");
        context.waitTicks(5);
        context.waitFor(client -> {
            for (var entity:client.level.entitiesForRendering())
                if(entity.getType()==EntityTypes.BEE && Math.abs(entity.getX()-5)<.2
                        && TrackedOutlines.colorFor(entity)==green) return true;
            return false;
        });
        server.runCommand("tp @e[type=minecraft:bee,x=5,y=-56,z=14,distance=..1] 4.0 -56 14.0");
        server.runCommand("tp @e[type=minecraft:armor_stand,x=5,y=-54,z=14,distance=..1] 4.0 -54 14.0");
        context.waitTicks(5);
        server.runCommand("summon minecraft:vex -8 -54 18 {NoAI:1b}");
        server.runCommand("summon minecraft:parrot -10 -52 20 {NoAI:1b,NoGravity:1b,Variant:1}");
        server.runCommand("summon minecraft:ghast 10 -53 24 {NoAI:1b,NoGravity:1b}");
        context.waitTicks(10);
        requireOutlines(context,OverlayColor.PURPLE.argb(),OverlayColor.GOLD.argb());
        context.waitTicks(210);
        requireColors(context.takeScreenshot("all-five-trackers"),true,true,true);
        command(context,"huntinghelper debug");
        context.waitTicks(5);
        context.runOnClient(client -> {
            if(HuntingDebug.enabled()||HuntingDebug.STATES.get()!=0||HuntingDebug.FILLS.get()!=0)
                throw new AssertionError("Disabled diagnostics still record render counters");
        });


        // /hh opens the same menu; the second card row (Parched, Pollendart) is reached by scrolling.
        command(context,"hh");context.waitForScreen(TracerSettingsScreen.class);
        long until=System.nanoTime()+MenuOpening.DURATION_NANOS;context.waitFor(client->System.nanoTime()>=until);
        context.getInput().pressKey(GLFW.GLFW_KEY_END);requireButtonsFit(context);
        context.runOnClient(client -> {
            var labels=client.gui.screen().children().stream()
                    .filter(net.minecraft.client.gui.components.Button.class::isInstance)
                    .map(net.minecraft.client.gui.components.Button.class::cast).filter(b -> b.visible)
                    .map(b -> b.getMessage().getString()).toList();
            if(!labels.contains("Parched: ON")||!labels.contains("Pollendart: ON"))throw new AssertionError("Second card row not reachable: "+labels);
        });
        context.takeScreenshot("tracker-menu-second-row");
        // Pollendart glow OFF removes its outline and tint.
        context.clickScreenButton("Pollendart glow: Match line");
        context.getInput().pressKey(GLFW.GLFW_KEY_HOME);context.getInput().pressKey(GLFW.GLFW_KEY_DOWN);
        context.getInput().pressKey(GLFW.GLFW_KEY_ENTER);
        context.clickScreenButton("Done");context.waitTicks(3);
        context.waitFor(client -> renderedPollendartTint==0 && renderedParchedGlow==red);

        context.runOnClient(client -> { if (HuntingDebug.enabled()) throw new AssertionError("Debug starts disabled"); });
        command(context,"hh debug");context.waitTicks(105);
        context.runOnClient(client -> { if (!HuntingDebug.enabled()) throw new AssertionError("/hh debug did not enable debug logging"); });
        command(context,"huntinghelper debug");
        context.runOnClient(client -> { if (HuntingDebug.enabled()) throw new AssertionError("/huntinghelper debug did not toggle debug logging"); });
        server.runCommand("kill @e[type=minecraft:parched]");server.runCommand("kill @e[type=minecraft:bogged]");
        server.runCommand("kill @e[type=minecraft:bee]");server.runCommand("kill @e[type=minecraft:armor_stand]");
        server.runCommand("kill @e[type=minecraft:vex]");server.runCommand("kill @e[type=minecraft:parrot]");
        server.runCommand("kill @e[type=minecraft:ghast]");
        context.waitTicks(40);
        requireNoCustomGlow(context);
    }
    private static void requireNoCustomGlow(ClientGameTestContext context) {
        context.runOnClient(client -> {
            for(var entity:client.level.entitiesForRendering())
                if(TrackedOutlines.colorFor(entity)!=0)throw new AssertionError("A hidden/removed target still glows");
        });
    }
    private static void requireAllTrackersVisible(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var buttons=client.gui.screen().children().stream()
                    .filter(net.minecraft.client.gui.components.Button.class::isInstance)
                    .map(net.minecraft.client.gui.components.Button.class::cast).filter(b->b.visible).toList();
            if(buttons.size()!=TracerTarget.values().length*3+5)
                throw new AssertionError("The normal menu must show every tracker and color control without scrolling");
        });
    }
    private static String stand(int x,int y,int z,String name) {
        return "summon minecraft:armor_stand "+x+" "+y+" "+z+" {NoGravity:1b,Invisible:1b,Marker:1b,CustomNameVisible:1b,CustomName:\""+name+"\"}";
    }
    private static boolean near(double value,double expected) { return Math.abs(value-expected)<1; }

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
        int controls=TracerTarget.values().length*3+5;
        for(int i=0;i<controls;i++) {
            context.getInput().pressKey(GLFW.GLFW_KEY_TAB);
            context.runOnClient(client -> {
                var screen=client.gui.screen();
                if(!(screen.getFocused() instanceof net.minecraft.client.gui.components.Button button)||!button.visible)
                    throw new AssertionError("Keyboard focus was not revealed");
                reached.add(button.getMessage().getString());
            });
        }
        if(reached.size()!=controls)throw new AssertionError("Not every tracker/color/footer control is keyboard reachable: "+reached);
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

package dev.sleepcodex.foragingpuckgrinding;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.azureaaron.hmapi.events.HypixelPacketEvents;
import net.azureaaron.hmapi.network.packet.v1.s2c.LocationUpdateS2CPacket;
import net.johnceo.sparklingmutuals.config.SafariEspConfig;
import net.johnceo.sparklingmutuals.safari.SafariIsland;
import net.johnceo.sparklingmutuals.safari.esp.SafariEsp;
import net.johnceo.sparklingmutuals.safari.SafariAssist;
import net.johnceo.sparklingmutuals.safari.SafariBiome;
import net.johnceo.sparklingmutuals.safari.SafariRoster;
import net.johnceo.sparklingmutuals.safari.SafariTracking;

/** Real Cavern models under the production recognizer. The run owns the counts across despawns. */
final class CavernPopulationGameChecks {
    private record Model(String species, String entity, String texture, int count) {
        String summon(int index, int z) {
            String tag = species.toLowerCase(Locale.ROOT);
            String common = "NoGravity:1b,NoAI:1b,Silent:1b,CustomName:'" + species
                    + "',CustomNameVisible:1b,Tags:['roster_fixture','roster_" + tag + "','roster_" + tag + index + "']";
            String item = "";
            if (texture != null) {
                String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + texture + "\"}}}";
                String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
                String head = "{id:'minecraft:player_head',count:1,components:{'minecraft:profile':"
                        + "{properties:[{name:'textures',value:'" + encoded + "'}]}}}";
                item = entity.equals("armor_stand") ? ",equipment:{head:" + head + "}" : ",item:" + head;
            }
            return "summon " + entity + " " + (-86 + index * 5) + " 65 " + z + " {" + common + item + "}";
        }
    }

    private static final List<Model> MODELS = List.of(
            new Model("Shyworm", "armor_stand", "b4287b8a0a642dac535a6ee29459efd17d5cee1eb359e436bc8e7abba3da14b7", 2),
            new Model("Chuckwalla", "item_display", "fc63cd0d480971a7beae5fd503e5d51658cd906330843cbad92018f5b98b4fe5", 2),
            new Model("Driftling", "armor_stand", "f4c4f8e5fce1ec2d299cb8a395792ecddc497a1d8af86faaa5e20373016c7225", 2),
            new Model("Scrappy", "armadillo", null, 3));

    private static void check(String species, int caught, int located) {
        var run = SafariTracking.INSTANCE.getRun();
        var critter = SafariRoster.INSTANCE.named(species);
        int actualCaught = SafariRosterGameChecks.caught(run, species);
        Integer actualLocated = SafariRosterGameChecks.located(run, critter);
        if (actualCaught != caught || !Integer.valueOf(located).equals(actualLocated))
            throw new AssertionError(species + " expected " + caught + " caught, " + located + " located, got " + actualCaught + "/" + actualLocated);
    }

    static void run(ClientGameTestContext c, TestSingleplayerContext world) {
        var parent = c.computeOnClient(client -> client.gui.screen());
        String restore = c.computeOnClient(client -> String.format(Locale.ROOT, "tp @p %.4f %.4f %.4f %.2f %.2f",
                client.player.getX(), client.player.getY(), client.player.getZ(), client.player.getYRot(), client.player.getXRot()));
        var server = world.getServer();
        server.runCommand("fill -92 64 26 -65 64 53 stone");
        server.runCommand("tp @p -80 65 31 0 6");
        c.runOnClient(client -> {
            client.gui.setScreen(null);
            SafariAssist.INSTANCE.markEntered();
            SafariTracking.INSTANCE.getLedger().enter(System.currentTimeMillis());
        });
        c.waitFor(client -> SafariAssist.INSTANCE.getBiome() == SafariBiome.CAVERN);
        for (int n = 0; n < MODELS.size(); n++) {
            var model = MODELS.get(n);
            for (int i = 0; i < model.count(); i++) server.runCommand(model.summon(i, 36 + 5 * n));
        }
        c.waitTicks(20);
        // Static Driftling decorations have the same head. Only a model that actually moves qualifies.
        for (int i = 0; i < 3; i++) {
            server.runCommand("execute as @e[tag=roster_driftling] at @s run tp @s ~0.5 ~ ~");
            c.waitTicks(6);
        }
        c.runOnClient(client -> {
            var empty = java.util.Optional.<String>empty();
            HypixelPacketEvents.LOCATION_UPDATE.invoker().onPacket(new LocationUpdateS2CPacket(
                    "fixture", java.util.Optional.of("SKYBLOCK"), empty, java.util.Optional.of("safari"), empty));
            var armadillo = java.util.stream.StreamSupport.stream(client.level.entitiesForRendering().spliterator(), false)
                    .filter(e -> e.getType() == net.minecraft.world.entity.EntityTypes.ARMADILLO).findFirst().orElseThrow();
            boolean glow = SafariEspConfig.INSTANCE.getCritterGlow();
            try {
                SafariEspConfig.INSTANCE.setCritterGlow(true);
                if (SafariEsp.INSTANCE.outlineColor(armadillo) == null || !client.shouldEntityAppearGlowing(armadillo))
                    throw new AssertionError("A highlighted Scrappy must be eligible for native glow even in another room");
                SafariEspConfig.INSTANCE.setCritterGlow(false);
                if (client.shouldEntityAppearGlowing(armadillo))
                    throw new AssertionError("Disabling critter outlines must restore ordinary culling");
            } finally {
                SafariEspConfig.INSTANCE.setCritterGlow(glow);
                SafariIsland.INSTANCE.reset();
            }
            for (var model : MODELS) check(model.species(), 0, model.count());
            SafariRosterGameChecks.message("The Shyworm burrowed into the ground!", false);
            SafariRosterGameChecks.message("The Chuckwalla dodged your critter capsule and started running away!", false);
            SafariRosterGameChecks.message("The Scrappy happily eats the Wholesale Wheat.", false);
            for (var model : MODELS) check(model.species(), 0, model.count());
        });
        // A burrowed head, hidden tag, stationary model or unloaded mob does not award a catch.
        server.runCommand("tp @e[tag=roster_shyworm0] -86 1 36");
        server.runCommand("data merge entity @e[tag=roster_chuckwalla0,limit=1] {CustomNameVisible:0b}");
        server.runCommand("kill @e[tag=roster_fixture]");
        c.waitTicks(12);
        c.runOnClient(client -> {
            for (var model : MODELS) {
                check(model.species(), 0, model.count());
                for (int i = 1; i < model.count(); i++) SafariRosterGameChecks.catchOne(model.species(), true);
                check(model.species(), model.count() - 1, 1);
            }
            client.gui.setScreen(new RunReportScreen(parent));
        });
        c.clickScreenButton("Cavern");
        c.waitTicks(3);
        c.takeScreenshot("cavern-roster-checklist");
        c.clickScreenButton("Back");
        server.runCommand(restore);
        System.out.println("PASS: rendered Cavern Shyworm, Chuckwalla, moving Driftling and Scrappy tracked through model loss and filtered catches");
    }
}

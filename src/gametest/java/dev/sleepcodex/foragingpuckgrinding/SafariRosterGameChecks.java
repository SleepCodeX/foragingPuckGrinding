package dev.sleepcodex.foragingpuckgrinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import kotlin.Pair;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.johnceo.sparklingmutuals.hunting.HuntingCompletion;
import net.johnceo.sparklingmutuals.hunting.HuntingHuds;
import net.johnceo.sparklingmutuals.safari.*;
import net.minecraft.network.chat.Component;

/** Exercise the installed chat handlers and the same run snapshot used by the world scanner. */
final class SafariRosterGameChecks {
    // The six source-dependent species get a concrete population for this simulated run.
    private record Population(int expected, boolean sourceDependent, String names) {}
    private static final List<Population> POPULATIONS = List.of(
            new Population(8, false, "Foxtrot,Cavernfish,Flitter,Shyworm,Strongarm,Tepid,Litterbug,Solsnatcher"),
            new Population(6, false, "Honeybug,Treefrog,Woodchucker,Driftling,Shuddersquid,Mantis Shrimp,Areita,Bloodbat"),
            new Population(4, false, "Chuckwalla,Polaris,Billygoat,Nozzlenose,Duplico,Hideonwall,Gazer"),
            new Population(3, false, "Fluffling,Hideonfloor,Scrappy,Gemzie,Troodon"),
            new Population(1, false, "Wumpa,Hideyho,Doomspiral"),
            new Population(2, true, "Bluebird,Parakeet,Macaw,Rockmite,Snoozle"),
            new Population(5, true, "Gimmiegold"));

    static void message(String text, boolean filtered) {
        var component = Component.literal(text);
        if (filtered) ClientReceiveMessageEvents.GAME_CANCELED.invoker().onReceiveGameMessageCanceled(component, false);
        else ClientReceiveMessageEvents.GAME.invoker().onReceiveGameMessage(component, false);
    }

    static void catchOne(String species, boolean shared) {
        String text;
        if (shared) {
            String verb = species.equals("Hideyho") ? "finding the " : "catching a ";
            text = "§bLOOT SHARE! §7You received 4x " + species + " Shard from [VIP] Moss " + verb + species + "!";
        } else if (species.equals("Hideyho")) {
            text = "CAPTURE! You found the Hideyho, and as a reward it gave you a Hideyho Shard!";
        } else {
            text = "CAPTURE! You caught a " + species + " and gained 3x " + species + " Shard!";
        }
        message(text, shared);
    }

    static Integer located(SafariRun run, SafariCritter critter) {
        return HuntingCompletion.INSTANCE.located(critter, HuntingCompletion.INSTANCE.of(run));
    }
    static int caught(SafariRun run, String name) { return HuntingCompletion.INSTANCE.of(run).caught(name); }
    private static int coverage(SafariRun run) {
        int total = 0;
        for (var species : SafariRoster.INSTANCE.getAll()) if (caught(run, species.getName()) > 0) total++;
        return total;
    }
    /** The second HUD row (missing list or "also here") for a species, from the run alone. */
    private static String hud(SafariRun run, SafariCritter critter) {
        var panel = HuntingHuds.INSTANCE.missingPanel(critter.getBiome(), HuntingCompletion.INSTANCE.of(run), false, java.util.Map.of(), n -> 0);
        return panel.getRows().stream().filter(row -> row.getLabel().equals(critter.getName()))
                .map(row -> String.valueOf(row.getValue())).findFirst().orElse("<absent>");
    }

    static void run(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var ledger = SafariTracking.INSTANCE.getLedger();
            ledger.enter(System.currentTimeMillis());
            var run = ledger.getCurrent();
            var roster = SafariRoster.INSTANCE.getAll();
            var names = new HashSet<String>();
            var bodies = new ArrayList<Pair<String, UUID>>();
            for (var group : POPULATIONS) for (String name : group.names().split(",")) {
                if (!names.add(name) || SafariRoster.INSTANCE.named(name) == null) throw new AssertionError(name);
                for (int i = 0; i < group.expected(); i++) bodies.add(new Pair<>(name, new UUID(31, bodies.size())));
            }
            if (names.size() != roster.size()) throw new AssertionError("Missing a species from the roster test");
            // Qualify bird populations through the feeder survey. Two Macaws from one feed are two bodies.
            run.visitBiome(SafariBiome.FOREST, System.currentTimeMillis());
            run.scanBirdSources(3, List.of("Bluebird", "Bluebird", "Parakeet", "Parakeet", "Macaw", "Macaw"), true, null);
            run.observePopulation(bodies);
            run.observePopulation(bodies);
            run.observePopulation(List.of());
            for (var group : POPULATIONS) for (String name : group.names().split(",")) {
                var species = SafariRoster.INSTANCE.named(name);
                if (!Integer.valueOf(group.expected()).equals(located(run, species)) || caught(run, name) != 0)
                    throw new AssertionError(name + " lost its located population after unloading: " + located(run, species));
                for (int i = 1; i < group.expected(); i++) catchOne(name, i % 2 == 1);
                if (!Integer.valueOf(1).equals(located(run, species)) || caught(run, name) != group.expected() - 1)
                    throw new AssertionError(name + " should have one located creature left: " + located(run, species));
                // Second HUD: a quota still owed reads "3/4 · 1 located"; a done species stays under "also here".
                String row = hud(run, species);
                String expectedRow = species.getQuota() > 1 && species.getQuota() > group.expected() - 1
                        ? (group.expected() - 1) + "/" + species.getQuota() + " · 1 located" : "1 located";
                if (!row.equals(expectedRow)) throw new AssertionError(name + " HUD expected " + expectedRow + " but displayed " + row);
                catchOne(name, true);
                if (caught(run, name) != group.expected())
                    throw new AssertionError(name + " counted the reward stack more than once");
                if (!Integer.valueOf(0).equals(located(run, species)) || !hud(run, species).equals("<absent>"))
                    throw new AssertionError(name + " should have cleared: " + located(run, species) + " / " + hud(run, species));
            }
            if (coverage(run) != 37) throw new AssertionError("Incomplete shared coverage");
            ledger.enter(System.currentTimeMillis());
            var fresh = ledger.getCurrent();
            for (var species : roster) {
                if (located(fresh, species) != null || caught(fresh, species.getName()) != 0)
                    throw new AssertionError("Stale population for " + species.getName());
                catchOne(species.getName(), false);
            }
            if (coverage(fresh) != 37) throw new AssertionError("Incomplete personal coverage");
        });
        System.out.println("PASS: all 37 Safari species in the run ledger and second HUD, visible and hidden captures, 31 bounded populations, six conditional sources and fresh-run isolation");
    }
}

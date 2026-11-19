package dev.sleepcodex.foragingpuckgrinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import kotlin.Pair;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
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

    private static FullClearProgress.Remaining row(SafariRun run, SafariCritter critter) {
        return FullClearProgress.remaining(run, critter.getBiome()).stream()
                .filter(item -> item.species().equals(critter)).findFirst().orElse(null);
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
                var item = row(run, species);
                if (item == null || !Integer.valueOf(group.expected()).equals(item.located()) || item.caught() != 0)
                    throw new AssertionError(name + " lost its located population after unloading: " + item);
                for (int i = 1; i < group.expected(); i++) catchOne(name, i % 2 == 1);
                item = row(run, species);
                if (item == null || !Integer.valueOf(1).equals(item.located()) || item.caught() != group.expected() - 1
                        || !item.amount().equals(group.sourceDependent() ? "1+ remaining" : "1 remaining"))
                    throw new AssertionError(name + " should have one located creature left: " + item);
                catchOne(name, true);
                item = row(run, species);
                if (FullClearProgress.caught(run, name) != group.expected())
                    throw new AssertionError(name + " counted the reward stack more than once");
                if (!group.sourceDependent() && item != null)
                    throw new AssertionError(name + " should have cleared: " + item);
                if (group.sourceDependent() && (item == null || !Integer.valueOf(0).equals(item.located())
                        || !item.amount().startsWith("Check ")))
                    throw new AssertionError(name + " lost its unresolved spawn source: " + item);
            }
            if (FullClearProgress.coverage(run, roster) != 37) throw new AssertionError("Incomplete shared coverage");
            ledger.enter(System.currentTimeMillis());
            var fresh = ledger.getCurrent();
            for (var species : roster) {
                var item = row(fresh, species);
                if (item == null || item.located() != null || item.caught() != 0)
                    throw new AssertionError("Stale population for " + species.getName());
                catchOne(species.getName(), false);
            }
            if (FullClearProgress.coverage(fresh, roster) != 37) throw new AssertionError("Incomplete personal coverage");
        });
        System.out.println("PASS: all 37 Safari species, visible and hidden captures, 31 bounded populations, six conditional sources and fresh-run isolation");
    }
}

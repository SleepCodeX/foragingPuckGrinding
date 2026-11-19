package dev.sleepcodex.foragingpuckgrinding;

import java.util.ArrayList;
import java.util.List;
import net.johnceo.sparklingmutuals.hud.HudPanel;
import net.johnceo.sparklingmutuals.hud.HudRow;
import net.johnceo.sparklingmutuals.hud.UiIcons;
import net.johnceo.sparklingmutuals.safari.SafariBiome;
import net.johnceo.sparklingmutuals.safari.SafariCritter;
import net.johnceo.sparklingmutuals.safari.SafariMode;
import net.johnceo.sparklingmutuals.safari.SafariRoster;
import net.johnceo.sparklingmutuals.safari.SafariRun;
import net.johnceo.sparklingmutuals.safari.completion.SafariFullClear;

/** A capture is run-wide. A rendered population is a lower bound until the area is checked. */
public final class FullClearProgress {
    private static final int MUTED = 0xFFB6C0CB;
    private FullClearProgress() {}

    // Published spawn ranges and conditional sources are documented in docs/FIELD-JOURNAL.md.
    private static int maximum(String name) {
        return switch (name) {
            case "Foxtrot", "Cavernfish", "Flitter", "Shyworm", "Strongarm", "Tepid",
                    "Litterbug", "Solsnatcher" -> 8;
            case "Honeybug", "Treefrog", "Woodchucker", "Driftling", "Shuddersquid",
                    "Mantis Shrimp", "Areita", "Bloodbat" -> 6;
            case "Chuckwalla", "Polaris", "Billygoat", "Nozzlenose", "Duplico", "Hideonwall" -> 4;
            case "Fluffling", "Hideonfloor" -> 3;
            default -> SafariFullClear.INSTANCE.getMaximumCaptures().getOrDefault(name, 0);
        };
    }

    static int caught(SafariRun run, String name) {
        if (run == null) return 0;
        int local = run.inferredCapture(name) ? 0 : run.count(name);
        // Both players may have observed the same loot share.
        return Math.max(local, run.getSharedCritters().captures(name));
    }

    static int coverage(SafariRun run, List<SafariCritter> roster) {
        int total = 0;
        for (var species : roster) if (caught(run, species.getName()) > 0) total++;
        return total;
    }

    private static HudRow coverageBar(String label, SafariRun run, List<SafariCritter> roster, int color) {
        int count = coverage(run, roster);
        return new HudRow(label, count + "/" + roster.size(), color, color, (float) count / roster.size(),
                UiIcons.INSTANCE.forLabel(label), null, List.of(), UiIcons.Kind.CHECK, List.of());
    }

    public static HudPanel progress(SafariRun run, HudPanel original) {
        var rows = new ArrayList<HudRow>();
        rows.add(coverageBar("Species caught", run, SafariRoster.INSTANCE.getAll(), HudRow.WHITE));
        for (HudRow row : original.getRows()) {
            SafariBiome biome = null;
            for (var area : SafariBiome.values()) if (area.getLabel().equals(row.getLabel())) biome = area;
            rows.add(biome == null ? row : coverageBar(biome.getLabel(), run, biome.getCritters(), biome.getColor()));
        }
        return new HudPanel(original.getTitle(), original.getTitleColor(), rows, original.getTitleIcon(), null, null);
    }

    record Remaining(SafariCritter species, int caught, String amount) {}

    static List<Remaining> remaining(SafariRun run, SafariBiome biome) {
        var partial = new ArrayList<Remaining>();
        var unstarted = new ArrayList<Remaining>();
        for (var species : biome.getCritters()) {
            String name = species.getName();
            int captured = caught(run, name);
            String amount = amount(run, name, captured);
            if (amount != null) (captured > 0 ? partial : unstarted).add(new Remaining(species, captured, amount));
        }
        partial.addAll(unstarted);
        return partial;
    }

    private static String amount(SafariRun run, String name, int caught) {
        int upper = maximum(name);
        int target = SafariFullClear.INSTANCE.minimum(name);
        boolean bird = name.equals("Bluebird") || name.equals("Parakeet") || name.equals("Macaw");
        if (run != null) {
            if (bird) target = run.getBirds().spawnedCount(name);
            else {
                target = Math.max(target, run.capturesLeft(name) + run.count(name));
                if (name.equals("Rockmite")) target = Math.max(target, run.getRockmiteMounds());
                if (name.equals("Honeybug") && run.getBeeNestCountKnown()) upper = run.getHoneybugCaptureTarget();
            }
        }
        int left = Math.max(0, target - caught);
        if (left == 0) {
            if (upper > 0 && caught >= upper) return null;
            if (run != null && (run.fullClearMissingComplete(name) || run.sharedCritterComplete(name,
                    SafariMode.FULL_CLEAR))) return null;
        }
        if (upper > 0) {
            int high = Math.max(left, upper - caught);
            if (left == high) return left + " remaining";
            return left == 0 ? "Up to " + high + " left" : left + "–" + high + " remaining";
        }
        if (left > 0) {
            boolean sourcesResolved = run != null && (bird && run.birdSpawnsComplete(false)
                    || name.equals("Rockmite") && run.getAllMoundsBroken());
            return left + (sourcesResolved ? " remaining" : "+ remaining");
        }
        if (bird) return "Check feeder";
        return switch (name) {
            case "Snoozle" -> "Check walls";
            case "Rockmite" -> "Check mounds";
            case "Gimmiegold" -> "Check coins";
            default -> "Check area";
        };
    }

    public static HudPanel missing(SafariRun run, SafariBiome biome) {
        var rows = new ArrayList<HudRow>();
        for (var item : remaining(run, biome)) {
            rows.add(new HudRow(item.species().getName(), item.amount(), item.species().getColor(),
                    item.caught() > 0 ? HudRow.WHITE : MUTED, null, null, null, List.of(), UiIcons.Kind.CHECK, List.of()));
        }
        if (rows.isEmpty()) rows.add(new HudRow("Area cleared", null, biome.getColor(), MUTED,
                null, null, true, List.of(), UiIcons.Kind.CHECK, List.of()));
        return new HudPanel(biome.getLabel() + " remaining", biome.getColor(), rows,
                UiIcons.INSTANCE.forLabel(biome.getLabel()), null, null);
    }
}

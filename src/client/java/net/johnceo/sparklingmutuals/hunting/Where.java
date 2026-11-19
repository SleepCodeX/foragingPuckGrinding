// Hunting Helper: CritterMod features ported into Sparkling Mutuals (see docs/SM-UPSTREAM.md).
package net.johnceo.sparklingmutuals.hunting;

import net.johnceo.sparklingmutuals.safari.SafariAreaMap;
import net.johnceo.sparklingmutuals.safari.SafariAssist;
import net.johnceo.sparklingmutuals.safari.SafariBiome;
import net.johnceo.sparklingmutuals.safari.SafariCritter;
import net.johnceo.sparklingmutuals.safari.SafariRoster;
import net.johnceo.sparklingmutuals.safari.SafariRules;

/** Sparkling Mutuals' own location, roster and text rules, for the ported CritterMod helpers. */
final class Where {
    private Where() { }

    static boolean inSafari() { return SafariAssist.INSTANCE.getInSafari(); }
    static SafariBiome biome() { return SafariAssist.INSTANCE.getBiome(); }
    static String strip(String text) { return SafariRules.INSTANCE.strip(text); }
    static SafariCritter critter(String name) { return name == null ? null : SafariRoster.INSTANCE.named(name.trim()); }
    static SafariCritter findIn(String text) { return SafariRoster.INSTANCE.findIn(text); }

    /** The biome a position belongs to on the area map, or null on the boat and outside the map. */
    static SafariBiome biomeAt(double x, double y, double z) {
        Integer area = SafariAreaMap.INSTANCE.biomeAt(x, y, z);
        return area == null ? null : SafariBiome.Companion.mapped(area);
    }

    static SafariBiome biomeNamed(String label) {
        if (label == null) return null;
        for (SafariBiome biome : SafariBiome.values()) if (biome.getLabel().equalsIgnoreCase(label)) return biome;
        return null;
    }

    /** Commons (white in the roster) are caught on the throw. */
    static boolean common(SafariCritter critter) { return critter.getColor() == 0xFFFFFFFF; }
}

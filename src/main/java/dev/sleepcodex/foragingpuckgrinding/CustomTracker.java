package dev.sleepcodex.foragingpuckgrinding;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** A saved user rule. Its ID survives renames, color changes and toggles. */
record CustomTracker(UUID id, Mode mode, String query, boolean enabled, OverlayColor line,
        OverlayColor glow) implements TrackingRule {
    enum Mode { NAMETAG, MOB_TYPE }
    private static final Pattern FORMAT = Pattern.compile("§[0-9a-fk-orx]", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPACE = Pattern.compile("(?U)\\s+");
    private static final Pattern TYPE = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    CustomTracker {
        Objects.requireNonNull(id);
        Objects.requireNonNull(mode);
        Objects.requireNonNull(glow);
        if (!line.isSolid()) throw new IllegalArgumentException("Choose a visible line color");
        query = clean(query);
        if (query.isEmpty() || query.length() > 96) throw new IllegalArgumentException("Enter 1 to 96 characters");
        if (mode == Mode.MOB_TYPE) {
            query = normalizeType(query);
            if (!TYPE.matcher(query).matches()) throw new IllegalArgumentException("Enter a valid mob type");
        }
    }

    static String clean(String text) {
        String plain = text.indexOf('§') < 0 ? text : FORMAT.matcher(text).replaceAll("");
        return SPACE.matcher(plain).replaceAll(" ").strip();
    }

    static String normalizedName(String text) { return clean(text).toLowerCase(Locale.ROOT); }

    static String normalizeType(String text) {
        String key = normalizedName(text).replace(' ', '_');
        return key.indexOf(':') < 0 ? "minecraft:" + key : key;
    }

    public String label() {
        return mode == Mode.MOB_TYPE && query.startsWith("minecraft:") ? query.substring(10) : query;
    }

    CustomTracker toggled() { return new CustomTracker(id, mode, query, !enabled, line, glow); }
}

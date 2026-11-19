package dev.sleepcodex.foragingpuckgrinding;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Each mob has one stable selection key and the same color for its line and box. */
public enum TracerTarget implements TrackingRule {
    VEX("Vex", 0xFFCC55FF),
    BLUE_JAY("Blue Jay", 0xFF45D9FF),
    HIVE_THIEF("Hivethief", 0xFFFFC655),
    PARCHED("Parched", 0xFFFF6868),
    POLLENDART("Pollendart", 0xFF76E5A0);

    private static final Pattern FORMATTING = Pattern.compile("§[0-9a-fk-or]", Pattern.CASE_INSENSITIVE);
    // Whole words only, so "Vex" never matches inside longer names.
    private static final Pattern NAMETAG = Pattern.compile(
            "\\b(vex|blue ?jay|hive ?thief|parched|pollen ?dart)\\b", Pattern.CASE_INSENSITIVE);

    private final String label;
    private final int color;
    TracerTarget(String label, int color) { this.label = label; this.color = color; }
    public String label() { return label; }
    public int color() { return color; }
    /** Everything except Vex is only tracked while the scoreboard or tab list says Torrhus. */
    public boolean torrhusOnly() { return this != VEX; }
    public static TracerTarget parse(String name) {
        if (name == null) return null;
        return switch (name.strip().toUpperCase(Locale.ROOT).replace(' ', '_')) {
            case "VEX" -> VEX;
            case "BLUE_JAY", "BLUEJAY" -> BLUE_JAY;
            case "HIVE_THIEF", "HIVETHIEF", "GHAST" -> HIVE_THIEF;
            case "PARCHED" -> PARCHED;
            case "POLLENDART", "POLLEN_DART" -> POLLENDART;
            default -> null;
        };
    }

    /** Vex, Blue Jay and Hivethief are also found by entity type; Parched and Pollendart only by nametag. */
    public boolean typeDetected() { return this == VEX || this == BLUE_JAY || this == HIVE_THIEF; }

    /** The target named by a Hypixel nametag such as "[Lv58] Parched 25,000/25,000❤", or null. */
    public static TracerTarget fromNametag(String text) {
        if (text == null) return null;
        Matcher matcher = NAMETAG.matcher(FORMATTING.matcher(text).replaceAll(""));
        return matcher.find() ? parse(matcher.group(1).replace(" ", "")) : null;
    }
}

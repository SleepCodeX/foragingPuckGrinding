package dev.sleepcodex.foragingpuckgrinding;

import java.util.Locale;

/** Each mob has one stable selection key and the same color for its line and box. */
public enum TracerTarget {
    VEX("Vex", 0xFFCC55FF),
    BLUE_JAY("Blue Jay", 0xFF45D9FF),
    HIVE_THIEF("Hivethief", 0xFFFFC655);

    private final String label;
    private final int color;
    TracerTarget(String label, int color) { this.label = label; this.color = color; }
    public String label() { return label; }
    public int color() { return color; }
    public static TracerTarget parse(String name) {
        if (name == null) return null;
        return switch (name.strip().toUpperCase(Locale.ROOT).replace(' ', '_')) {
            case "VEX" -> VEX;
            case "BLUE_JAY", "BLUEJAY" -> BLUE_JAY;
            case "HIVE_THIEF", "HIVETHIEF", "GHAST" -> HIVE_THIEF;
            default -> null;
        };
    }
}

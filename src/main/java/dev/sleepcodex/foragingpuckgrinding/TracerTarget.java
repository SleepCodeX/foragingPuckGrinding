package dev.sleepcodex.foragingpuckgrinding;

import java.util.Locale;

public enum TracerTarget {
    VEX("Vex"), BLUE_JAY("Blue Jay"), BOTH("Both");

    private final String label;

    TracerTarget(String label) { this.label = label; }
    public String label() { return label; }
    public boolean includesVex() { return this != BLUE_JAY; }
    public boolean includesBlueJay() { return this != VEX; }
    public TracerTarget next() { return values()[(ordinal() + 1) % values().length]; }

    public static TracerTarget parse(String value) {
        try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException | NullPointerException ignored) { return VEX; }
    }
}

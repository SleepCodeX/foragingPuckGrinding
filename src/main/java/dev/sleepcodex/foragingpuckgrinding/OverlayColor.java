package dev.sleepcodex.foragingpuckgrinding;

import java.util.Locale;

/** Small shared palette; line and model-outline choices are stored independently. */
public enum OverlayColor {
    MATCH("Match line", 0), OFF("Off", 0),
    PURPLE("Purple", 0xFFCC55FF), CYAN("Cyan", 0xFF45D9FF), GOLD("Gold", 0xFFFFC655),
    GREEN("Green", 0xFF76E5A0), PINK("Pink", 0xFFFF80B8), BLUE("Blue", 0xFF729CFF),
    RED("Red", 0xFFFF6868), WHITE("White", 0xFFF4F3FF);

    private final String label;
    private final int argb;
    OverlayColor(String label, int argb) { this.label = label; this.argb = argb; }
    public String label() { return label; }
    public int argb() { return argb; }
    public boolean isSolid() { return this != MATCH && this != OFF; }
    public static OverlayColor defaultFor(TracerTarget target) {
        return switch (target) { case VEX -> PURPLE; case BLUE_JAY -> CYAN; case HIVE_THIEF -> GOLD;
            case PARCHED -> RED; case POLLENDART -> GREEN; };
    }
    static OverlayColor parse(String value, OverlayColor fallback) {
        try { return valueOf(value.strip().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException | NullPointerException ignored) { return fallback; }
    }
}

package dev.sleepcodex.foragingpuckgrinding;

/** Clock-based crossfade with no tick timers, input lock, or persistent animation work. */
public final class MenuOpening {
    public static final long DURATION_NANOS = 400_000_000L;
    private MenuOpening() { }
    public static double backdrop(long elapsed) { return smooth(elapsed / 200_000_000.0); }
    public static double panel(long elapsed) { return smooth((elapsed - 55_000_000L) / 345_000_000.0); }
    public static double content(long elapsed) { return smooth((elapsed - 135_000_000L) / 245_000_000.0); }
    public static double icon(long elapsed) {
        return smooth(elapsed / 140_000_000.0) * (1 - smooth((elapsed - 145_000_000L) / 255_000_000.0));
    }
    public static double travel(long elapsed) { return smooth((elapsed - 60_000_000L) / 340_000_000.0); }
    private static double smooth(double value) {
        double t = Math.max(0, Math.min(1, value));
        return t * t * (3 - 2 * t);
    }
}

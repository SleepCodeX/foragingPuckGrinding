package dev.sleepcodex.foragingpuckgrinding.journal.client;

import java.awt.Color;
import java.util.regex.Pattern;

/** An optional @period suffix animates hue while preserving alpha, saturation and brightness. */
public final class Colours {
    private static final Pattern FORMAT = Pattern.compile("#[0-9a-fA-F]{8}(?:@(?:[1-9]|[1-5][0-9]|60))?");
    private Colours() {}
    public static boolean valid(String value) { return value != null && FORMAT.matcher(value).matches(); }
    public static int argb(String value, int fallback) {
        if (!valid(value)) return fallback;
        int color = (int) Long.parseLong(value.substring(1, 9), 16);
        if (value.length() == 9) return color;
        long period = Integer.parseInt(value.substring(10)) * 1000L;
        float[] hsb = Color.RGBtoHSB(color >> 16 & 255, color >> 8 & 255, color & 255, null);
        float hue = hsb[0] + (System.currentTimeMillis() % period) / (float) period;
        return color & 0xFF000000 | Color.HSBtoRGB(hue, hsb[1], hsb[2]) & 0xFFFFFF;
    }
}

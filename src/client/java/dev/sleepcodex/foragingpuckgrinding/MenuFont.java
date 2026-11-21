// Adapted from SleepCodeX/trevorRngMod. Copyright (c) 2026 Anish. MIT; see licenses/Trevor-MIT.txt.
package dev.sleepcodex.foragingpuckgrinding;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** A single Nunito atlas with coverage baked at multiple physical pixel sizes. */
final class MenuFont {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("foragingpuckgrinding", "textures/gui/menu_font.png");
    private static final double EM = 13;
    private static final int[] LATIN = new int[256];
    private static final Map<Integer, Integer> SYMBOLS = new HashMap<>();
    private static final double[] ADVANCES;
    private static final Strike[] STRIKES;
    private static final int ATLAS_WIDTH, ATLAS_HEIGHT;
    private static double opacity = 1;

    private record Glyph(int u, int v, int width, int height, int left, int top) {}
    private record Strike(int size, Glyph[] glyphs) {}

    static {
        try (var reader = new InputStreamReader(Objects.requireNonNull(MenuFont.class.getResourceAsStream(
                "/assets/foragingpuckgrinding/menu_font.json")), StandardCharsets.UTF_8)) {
            JsonObject atlas = JsonParser.parseReader(reader).getAsJsonObject();
            ATLAS_WIDTH = atlas.get("width").getAsInt();
            ATLAS_HEIGHT = atlas.get("height").getAsInt();
            String repertoire = atlas.get("characters").getAsString();
            int[] characters = repertoire.codePoints().toArray();
            Arrays.fill(LATIN, repertoire.indexOf('?'));
            ADVANCES = new double[characters.length];
            JsonArray advances = atlas.getAsJsonArray("advances");
            for (int i = 0; i < characters.length; i++) {
                int ch = characters[i];
                if (ch < LATIN.length) LATIN[ch] = i;
                else SYMBOLS.put(ch, i);
                ADVANCES[i] = advances.get(i).getAsDouble() * EM;
            }
            JsonArray strikes = atlas.getAsJsonArray("strikes");
            STRIKES = new Strike[strikes.size()];
            for (int i = 0; i < STRIKES.length; i++) {
                JsonObject strike = strikes.get(i).getAsJsonObject();
                JsonArray metrics = strike.getAsJsonArray("glyphs");
                Glyph[] glyphs = new Glyph[characters.length];
                for (int j = 0; j < glyphs.length; j++) {
                    JsonArray g = metrics.get(j).getAsJsonArray();
                    glyphs[j] = new Glyph(g.get(0).getAsInt(), g.get(1).getAsInt(), g.get(2).getAsInt(),
                            g.get(3).getAsInt(), g.get(4).getAsInt(), g.get(5).getAsInt());
                }
                STRIKES[i] = new Strike(strike.get("size").getAsInt(), glyphs);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static void opacity(double value) { opacity = value; }

    private static int index(int ch) {
        return ch < LATIN.length ? LATIN[ch] : SYMBOLS.getOrDefault(ch, LATIN['?']);
    }

    static double width(String text) {
        double width = 0;
        for (int i = 0; i < text.length();) {
            int ch = text.codePointAt(i);
            width += ADVANCES[index(ch)];
            i += Character.charCount(ch);
        }
        return width;
    }

    private static Strike nearest(double pixels) {
        for (int i = 1; i < STRIKES.length; i++) {
            if (2 * pixels <= STRIKES[i - 1].size + STRIKES[i].size) return STRIKES[i - 1];
        }
        return STRIKES[STRIKES.length - 1];
    }

    static void text(GuiGraphicsExtractor g, String text, double x, double y, double scale, int color) {
        int alpha = (int) Math.round((color >>> 24) * opacity);
        if (alpha == 0 || text.isEmpty() || scale <= 0) return;
        color = (color & 0x00FFFFFF) | (alpha << 24);
        int density = Minecraft.getInstance().getWindow().getGuiScale();
        double physicalScale = scale * density;
        Strike strike = nearest(EM * physicalScale);
        double ratio = EM * physicalScale / strike.size;
        double pen = x * density;
        int baseline = (int) Math.round(y * density + EM * .75 * physicalScale);
        g.pose().pushMatrix();
        try {
            g.pose().scale(1f / density, 1f / density);
            for (int i = 0; i < text.length();) {
                int ch = text.codePointAt(i);
                i += Character.charCount(ch);
                int index = index(ch);
                Glyph glyph = strike.glyphs[index];
                if (glyph.width > 0) {
                    int left = (int) Math.round(pen + glyph.left * ratio);
                    int top = baseline + (int) Math.round(glyph.top * ratio);
                    g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, glyph.u, glyph.v,
                            Math.max(1, (int) Math.round(glyph.width * ratio)), Math.max(1, (int) Math.round(glyph.height * ratio)),
                            glyph.width, glyph.height, ATLAS_WIDTH, ATLAS_HEIGHT, color);
                }
                pen += ADVANCES[index] * physicalScale;
            }
        } finally {
            g.pose().popMatrix();
        }
    }

    static String ellipsis(String text, double max) {
        if (width(text) <= max) return text;
        double used = ADVANCES[index('…')];
        if (used > max) return "";
        int end = 0;
        while (end < text.length()) {
            int ch = text.codePointAt(end);
            used += ADVANCES[index(ch)];
            if (used > max) break;
            end += Character.charCount(ch);
        }
        return text.substring(0, end) + "…";
    }
}

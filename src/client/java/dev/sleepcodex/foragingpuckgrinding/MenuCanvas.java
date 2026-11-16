// Rounded texture geometry adapted from SleepCodeX/trevorRngMod.
// Copyright (c) 2026 Anish. MIT; see licenses/Trevor-MIT.txt.
package dev.sleepcodex.foragingpuckgrinding;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

final class MenuCanvas {
    private static final Identifier CIRCLE = Identifier.fromNamespaceAndPath(VexTracerClient.MOD_ID, "textures/gui/circle.png");
    private static final Identifier AXE = Identifier.fromNamespaceAndPath(VexTracerClient.MOD_ID, "icon.png");
    private final GuiGraphicsExtractor graphics;
    private final double opacity;

    MenuCanvas(GuiGraphicsExtractor graphics, double opacity) {
        this.graphics = graphics;
        this.opacity = opacity;
    }

    private int tint(int color) {
        return (color & 0xFFFFFF) | ((int) Math.round((color >>> 24) * opacity) << 24);
    }

    void round(int x, int y, int width, int height, int radius, int color) {
        color = tint(color);
        if (width <= 0 || height <= 0 || (color >>> 24) == 0) return;
        int r = Math.min(radius, Math.min(width, height) / 2);
        graphics.fill(x + r, y, x + width - r, y + height, color);
        graphics.fill(x, y + r, x + r, y + height - r, color);
        graphics.fill(x + width - r, y + r, x + width, y + height - r, color);
        corner(x, y, 0, 0, r, color);
        corner(x + width - r, y, 64, 0, r, color);
        corner(x, y + height - r, 0, 64, r, color);
        corner(x + width - r, y + height - r, 64, 64, r, color);
    }

    private void corner(int x, int y, int u, int v, int size, int color) {
        if (size > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, CIRCLE, x, y, u, v,
                size, size, 64, 64, 128, 128, color);
    }

    void axe(int x, int y, int size) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, AXE, x, y, 0, 0,
                size, size, 128, 128, 128, 128, tint(0xFFFFFFFF));
    }
}

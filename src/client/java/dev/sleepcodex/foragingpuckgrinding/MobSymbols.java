package dev.sleepcodex.foragingpuckgrinding;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Original rounded menu symbols, drawn with the existing shared corner atlas. */
final class MobSymbols {
    private MobSymbols() { }

    static void draw(GuiGraphicsExtractor graphics, MenuCanvas canvas, TracerTarget target, int x, int y, int size) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(size / 24f, size / 24f);
        try {
            switch (target) {
                case VEX -> {
                    canvas.round(-11, -3, 5, 10, 3, 0xFFA9DDEB);
                    canvas.round(6, -3, 5, 10, 3, 0xFFA9DDEB);
                    canvas.round(-6, -10, 12, 14, 5, 0xFFE6E3FB);
                    canvas.round(-4, 1, 8, 8, 3, 0xFFBAB1E9);
                    canvas.round(-2, 6, 4, 5, 2, 0xFFCC55FF);
                    canvas.round(-4, -5, 2, 3, 1, 0xFF645085);
                    canvas.round(2, -5, 2, 3, 1, 0xFF645085);
                }
                case BLUE_JAY -> {
                    canvas.round(-10, 3, 8, 3, 1, 0xFF4484B6);
                    canvas.round(-5, -6, 12, 14, 5, 0xFF45D9FF);
                    canvas.round(-5, -3, 7, 9, 4, 0xFF4484B6);
                    canvas.round(0, -11, 10, 9, 4, 0xFF80E8FF);
                    canvas.round(8, -7, 5, 3, 1, 0xFFF5C36E);
                    canvas.round(6, -8, 2, 2, 1, 0xFF243C55);
                    canvas.round(-1, 7, 2, 4, 1, 0xFFD1B28B);
                    canvas.round(4, 7, 2, 4, 1, 0xFFD1B28B);
                }
                case HIVE_THIEF -> {
                    canvas.round(-9, -10, 18, 15, 3, 0xFFFFC655);
                    canvas.round(-8, -9, 16, 12, 2, 0xFFFFF0CC);
                    canvas.round(-5, -5, 3, 3, 1, 0xFF755D4E);
                    canvas.round(2, -5, 3, 3, 1, 0xFF755D4E);
                    canvas.round(-2, 0, 4, 2, 1, 0xFFB68A69);
                    canvas.round(-6, 4, 3, 7, 1, 0xFFF1C87A);
                    canvas.round(-1, 4, 3, 9, 1, 0xFFFFE2A1);
                    canvas.round(4, 4, 3, 6, 1, 0xFFF1C87A);
                }
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }
}

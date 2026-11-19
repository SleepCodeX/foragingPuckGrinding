package dev.sleepcodex.foragingpuckgrinding;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MenuOpeningTest {
    @Test void startsWithIconThenCompletesWithoutAnOverlay() {
        assertEquals(0, MenuOpening.backdrop(-1));
        assertEquals(0, MenuOpening.panel(0));
        assertEquals(0, MenuOpening.icon(0));
        assertEquals(1, MenuOpening.icon(140_000_000));
        assertEquals(0, MenuOpening.panel(55_000_000));
        assertTrue(MenuOpening.panel(350_000_000) > 0);
        assertTrue(MenuOpening.icon(350_000_000) > 0);
        for (long time : new long[]{MenuOpening.DURATION_NANOS, 1_000_000_000, Long.MAX_VALUE}) {
            assertEquals(1, MenuOpening.panel(time));
            assertEquals(1, MenuOpening.travel(time));
            assertEquals(1, MenuOpening.backdrop(time));
            assertEquals(0, MenuOpening.icon(time));
        }
    }
    @Test void boundedFrameIndependentCurvesNeverReverseOrJump() {
        double panel = 0, backdrop = 0, travel = 0;
        for (long time = 0; time <= 700_000_000; time += 1_000_000) {
            double p = MenuOpening.panel(time), b = MenuOpening.backdrop(time), t = MenuOpening.travel(time), i = MenuOpening.icon(time);
            assertTrue(p >= panel && p <= 1 && b >= backdrop && b <= 1 && t >= travel && t <= 1);
            assertTrue(i >= 0 && i <= 1);
            assertTrue(p - panel < .01 && b - backdrop < .01 && t - travel < .01);
            panel = p; backdrop = b; travel = t;
        }
    }
}

package dev.sleepcodex.foragingpuckgrinding;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TracerRangeTest {
    @Test void includesBoundaryAndRejectsBeyondIt() {
        assertTrue(TracerRange.contains(100, -100, 228, -100, 8));
        assertFalse(TracerRange.contains(100, -100, 228.01, -100, 8));
    }

    @Test void measuresHorizontalRadiusRatherThanSquareOrDistanceFromWorldOrigin() {
        assertTrue(TracerRange.contains(-300, 700, -348, 764, 5));
        assertFalse(TracerRange.contains(-300, 700, -220, 780, 5));
    }

    @Test void followsRenderDistanceChanges() {
        assertFalse(TracerRange.contains(0, 0, 100, 0, 4));
        assertTrue(TracerRange.contains(0, 0, 100, 0, 8));
    }
}

package dev.sleepcodex.foragingpuckgrinding;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TracerRangeTest {
    @Test void includesCornerChunksAndTheirEntireFarEdge() {
        assertTrue(TracerRange.contains(0, 0, 143.99, 143.99, 8));
        assertTrue(TracerRange.contains(0, 0, -128, -128, 8));
        assertFalse(TracerRange.contains(0, 0, 144, 0, 8));
        assertFalse(TracerRange.contains(0, 0, -128.01, 0, 8));
    }
    @Test void negativeAndFractionalPositionsUseChunkFloors() {
        assertTrue(TracerRange.contains(-.01, -.01, -16, -16, 0));
        assertTrue(TracerRange.contains(-.01, -.01, -.001, -.001, 0));
        assertFalse(TracerRange.contains(-.01, -.01, 0, -.01, 0));
        assertTrue(TracerRange.contains(100, -100, 239.99, 31.99, 8));
        assertFalse(TracerRange.contains(100, -100, 240, 31.99, 8));
    }
    @Test void followsRenderDistanceAndRejectsInvalidCoordinates() {
        assertFalse(TracerRange.contains(0, 0, 100, 100, 4));
        assertTrue(TracerRange.contains(0, 0, 100, 100, 8));
        assertFalse(TracerRange.contains(0, 0, Double.NaN, 0, 8));
        assertFalse(TracerRange.contains(Double.NaN, 0, 0, 0, 8));
    }
}

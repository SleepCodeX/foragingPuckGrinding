package dev.sleepcodex.foragingpuckgrinding;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TracerTargetTest {
    @Test void selectorCyclesThroughExactlyTheThreeRequestedModes() {
        assertEquals(TracerTarget.BLUE_JAY, TracerTarget.VEX.next());
        assertEquals(TracerTarget.BOTH, TracerTarget.BLUE_JAY.next());
        assertEquals(TracerTarget.VEX, TracerTarget.BOTH.next());
        assertTrue(TracerTarget.BOTH.includesVex());
        assertTrue(TracerTarget.BOTH.includesBlueJay());
        assertFalse(TracerTarget.VEX.includesBlueJay());
        assertFalse(TracerTarget.BLUE_JAY.includesVex());
    }
}

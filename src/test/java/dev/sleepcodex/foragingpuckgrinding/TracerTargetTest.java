package dev.sleepcodex.foragingpuckgrinding;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TracerTargetTest {
    @Test void eachMobHasADistinctOpaqueColorAndStableSelectionName() {
        var colors = new java.util.HashSet<Integer>();
        for (var target : TracerTarget.values()) {
            assertEquals(255, target.color() >>> 24);
            assertTrue(colors.add(target.color()));
            assertEquals(target, TracerTarget.parse(target.name()));
        }
        assertEquals(TracerTarget.HIVE_THIEF, TracerTarget.parse("ghast"));
        assertEquals(TracerTarget.HIVE_THIEF, TracerTarget.parse("hivethief"));
        assertNull(TracerTarget.parse("unknown"));
    }
    @Test void wireBoxConnectsAllEightCornersWithEveryAxisEdgeExactlyOnce() {
        var edges = new java.util.HashSet<String>();int[] degrees = new int[8];
        for (int i = 0; i < BoxEdges.CORNERS.length; i += 2) {
            int a = BoxEdges.CORNERS[i], b = BoxEdges.CORNERS[i + 1];
            assertEquals(1, Integer.bitCount(a ^ b), "An edge changes exactly one coordinate");
            assertTrue(edges.add(Math.min(a,b) + ":" + Math.max(a,b)));
            degrees[a]++;degrees[b]++;
        }
        assertEquals(12, edges.size());
        for (int degree : degrees) assertEquals(3, degree);
    }
}

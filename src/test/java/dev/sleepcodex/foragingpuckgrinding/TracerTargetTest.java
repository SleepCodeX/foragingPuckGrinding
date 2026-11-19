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
        assertEquals(TracerTarget.PARCHED, TracerTarget.parse("parched"));
        assertEquals(TracerTarget.POLLENDART, TracerTarget.parse("Pollen Dart"));
        assertNull(TracerTarget.parse("unknown"));
    }
    @Test void onlyTheVexIsTrackedOutsideTorrhus() {
        for (var target : TracerTarget.values()) assertEquals(target != TracerTarget.VEX, target.torrhusOnly());
    }
    @Test void parchedAndPollendartAreNametagOnly() {
        for (var target : TracerTarget.values())
            assertEquals(target != TracerTarget.PARCHED && target != TracerTarget.POLLENDART, target.typeDetected());
    }
    @Test void hypixelNametagsNameTheirTarget() {
        assertEquals(TracerTarget.PARCHED, TracerTarget.fromNametag("[Lv58] Parched 25,000/25,000❤"));
        assertEquals(TracerTarget.POLLENDART, TracerTarget.fromNametag("§8[§7Lv24§8] §cPollendart§r §a300§f/§a300§c❤"));
        assertEquals(TracerTarget.BLUE_JAY, TracerTarget.fromNametag("[Lv10] blue jay 100/100"));
        assertEquals(TracerTarget.HIVE_THIEF, TracerTarget.fromNametag("[Lv40] Hivethief"));
        assertEquals(TracerTarget.VEX, TracerTarget.fromNametag("Vex 5/5"));
        assertNull(TracerTarget.fromNametag("[Lv5] Vexillum Guard"));
        assertNull(TracerTarget.fromNametag("[Lv12] Bee 50/50"));
        assertNull(TracerTarget.fromNametag("[Lv30] Bogged"));
        assertNull(TracerTarget.fromNametag(""));
        assertNull(TracerTarget.fromNametag(null));
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

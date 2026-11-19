package dev.sleepcodex.foragingpuckgrinding;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpatialBucketsTest {
    @Test void nearbyPairingCrossesPositiveAndNegativeCellEdgesWithoutLosingDistantMobs() {
        var index = new SpatialBuckets<String>();
        index.add("origin", 0, 0);
        index.add("negative edge", -.1, -.1);
        index.add("positive edge", 4.1, 4.1);
        index.add("far render corner", 511.9, -511.9);
        assertEquals(Set.of("origin", "negative edge"), adjacent(index, -.1, -.1));
        assertEquals(Set.of("origin", "positive edge"), adjacent(index, 4, 4));
        assertEquals(Set.of("far render corner"), adjacent(index, 512, -512));
        assertEquals(Set.of("origin", "negative edge", "positive edge"), adjacent(index, 3.9, 3.9));
    }

    @Test void clearingDropsOldWorldEntriesAndReusesBucketsWithoutLeakingContents() {
        var index = new SpatialBuckets<String>();
        index.add("old world", -4, 8);
        index.add("other column", 100, 100);
        index.clear();
        assertTrue(adjacent(index, -4, 8).isEmpty());
        index.add("new world", -4, 8);
        assertEquals(Set.of("new world"), adjacent(index, -4, 8));
        assertTrue(adjacent(index, 100, 100).isEmpty());
        index.clear();
        assertTrue(adjacent(index, -4, 8).isEmpty());
    }

    private static <T> Set<T> adjacent(SpatialBuckets<T> index, double x, double z) {
        Set<T> found = new HashSet<>();
        int cx = SpatialBuckets.cell(x), cz = SpatialBuckets.cell(z);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            found.addAll(index.column(cx + dx, cz + dz));
        return found;
    }
}

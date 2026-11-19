package dev.sleepcodex.foragingpuckgrinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reusable horizontal columns for nearby nametag/body pairing. This is an index, not a tracking
 * radius: callers add every loaded mob, at any height or distance, before querying adjacent cells.
 */
public final class SpatialBuckets<T> {
    private static final double CELL_SIZE = 4;
    private final Map<Long, List<T>> columns = new HashMap<>();
    private final List<List<T>> storage = new ArrayList<>();
    private int used;

    public static int cell(double position) { return (int) Math.floor(position / CELL_SIZE); }

    public void add(T value, double x, double z) {
        long key = key(cell(x), cell(z));
        List<T> bucket = columns.get(key);
        if (bucket == null) {
            if (used == storage.size()) storage.add(new ArrayList<>());
            bucket = storage.get(used++);
            columns.put(key, bucket);
        }
        bucket.add(value);
    }

    public List<T> column(int x, int z) {
        List<T> result = columns.get(key(x, z));
        return result == null ? List.of() : result;
    }

    public void clear() {
        for (int i = 0; i < used; i++) storage.get(i).clear();
        columns.clear();
        used = 0;
    }

    private static long key(int x, int z) { return ((long) x << 32) | (z & 0xFFFFFFFFL); }
}

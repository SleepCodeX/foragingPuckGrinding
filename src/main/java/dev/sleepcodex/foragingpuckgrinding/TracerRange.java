package dev.sleepcodex.foragingpuckgrinding;

/** The complete chunk square, including corners; no vertical-distance cap. */
public final class TracerRange {
    private TracerRange() { }

    public record Bounds(double minX, double minZ, double maxX, double maxZ) {
        public boolean contains(double x, double z) {
            return x >= minX && x < maxX && z >= minZ && z < maxZ;
        }
    }

    public static Bounds aroundChunk(int chunkX, int chunkZ, int chunks) {
        double radius = Math.max(0, chunks);
        return new Bounds((chunkX - radius) * 16, (chunkZ - radius) * 16,
                (chunkX + radius + 1) * 16, (chunkZ + radius + 1) * 16);
    }

    public static boolean contains(double anchorX, double anchorZ, double targetX, double targetZ, int chunks) {
        if (!Double.isFinite(anchorX) || !Double.isFinite(anchorZ)) return false;
        return aroundChunk((int) Math.floor(anchorX / 16), (int) Math.floor(anchorZ / 16), chunks)
                .contains(targetX, targetZ);
    }
}

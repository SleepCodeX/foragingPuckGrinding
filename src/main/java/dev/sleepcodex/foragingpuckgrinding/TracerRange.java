package dev.sleepcodex.foragingpuckgrinding;

/** Horizontal render radius; height does not exclude an otherwise loaded entity. */
public final class TracerRange {
    private TracerRange() { }

    public static boolean contains(double cameraX, double cameraZ, double targetX, double targetZ, int chunks) {
        double dx = targetX - cameraX;
        double dz = targetZ - cameraZ;
        double radius = Math.max(0, chunks) * 16.0;
        return dx * dx + dz * dz <= radius * radius;
    }
}

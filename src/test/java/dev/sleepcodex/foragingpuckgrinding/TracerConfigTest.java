package dev.sleepcodex.foragingpuckgrinding;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TracerConfigTest {
    @TempDir Path directory;

    @Test void defaultsToOffAndPersistsBothDirections() throws Exception {
        Path file = directory.resolve("config/tracers.properties");
        TracerConfig config = new TracerConfig(file);
        config.load();
        assertFalse(config.enabled());
        config.setEnabled(true);
        TracerConfig restarted = new TracerConfig(file);
        restarted.load();
        assertTrue(restarted.enabled());
        restarted.setEnabled(false);
        config.load();
        assertFalse(config.enabled());
        try (var files = Files.list(file.getParent())) {
            assertEquals(1, files.count());
        }
    }

    @Test void invalidOrMissingValueLoadsAsOff() throws Exception {
        Path file = directory.resolve("tracers.properties");
        Files.writeString(file, "enabled=garbage\n");
        TracerConfig config = new TracerConfig(file);
        config.load();
        assertFalse(config.enabled());
        Files.writeString(file, "unrelated=true\n");
        config.load();
        assertFalse(config.enabled());
    }

    @Test void persistsTargetAndEnabledIndependentlyAndMigratesOldConfig() throws Exception {
        Path file = directory.resolve("tracers.properties");
        Files.writeString(file, "enabled=true\n");
        TracerConfig config = new TracerConfig(file);
        config.load();
        assertTrue(config.enabled());
        assertEquals(TracerTarget.VEX, config.target());
        for (TracerTarget target : TracerTarget.values()) {
            config.setTarget(target);
            config.setEnabled(false);
            TracerConfig restarted = new TracerConfig(file);
            restarted.load();
            assertEquals(target, restarted.target());
            assertFalse(restarted.enabled());
            config.setEnabled(true);
        }
        Files.writeString(file, "enabled=true\ntarget=unknown\n");
        config.load();
        assertTrue(config.enabled());
        assertEquals(TracerTarget.VEX, config.target());
    }
}

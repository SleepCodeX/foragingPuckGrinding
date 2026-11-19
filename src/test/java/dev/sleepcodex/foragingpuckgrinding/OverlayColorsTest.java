package dev.sleepcodex.foragingpuckgrinding;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class OverlayColorsTest {
    @TempDir Path dir;

    @Test void oldSettingsKeepTheirTargetsAndGainMatchingGlows() throws Exception {
        Path file = dir.resolve("settings.properties");
        Files.writeString(file, "enabled=true\ntarget=BOTH\n");
        var config = new TracerConfig(file); config.load();
        assertTrue(config.enabled());
        assertEquals(EnumSet.of(TracerTarget.VEX, TracerTarget.BLUE_JAY), config.selectedTargets());
        for (TracerTarget target : TracerTarget.values()) {
            assertEquals(target.color(), config.lineColor(target));
            assertEquals(config.lineColor(target), config.glowColor(target));
        }
    }

    @Test void independentChoicesAndOffSurviveReload() throws Exception {
        Path file = dir.resolve("settings.properties");
        var config = new TracerConfig(file);
        config.setTargets(EnumSet.allOf(TracerTarget.class));
        config.setColor(TracerTarget.VEX, false, OverlayColor.GREEN);
        config.setColor(TracerTarget.VEX, true, OverlayColor.PINK);
        config.setColor(TracerTarget.HIVE_THIEF, true, OverlayColor.OFF);
        config.setEnabled(false);
        var restored = new TracerConfig(file); restored.load();
        assertFalse(restored.enabled());
        assertEquals(OverlayColor.GREEN.argb(), restored.lineColor(TracerTarget.VEX));
        assertEquals(OverlayColor.PINK.argb(), restored.glowColor(TracerTarget.VEX));
        assertEquals(0, restored.glowColor(TracerTarget.HIVE_THIEF));
        assertEquals(OverlayColor.GOLD.argb(), restored.lineColor(TracerTarget.HIVE_THIEF));
        assertEquals(OverlayColor.CYAN.argb(), restored.glowColor(TracerTarget.BLUE_JAY));
    }

    @Test void matchLineFollowsChangesAndInvalidStoredColorsFallBack() throws Exception {
        Path file = dir.resolve("settings.properties");
        var config = new TracerConfig(file);
        config.setColor(TracerTarget.VEX, false, OverlayColor.BLUE);
        assertEquals(OverlayColor.BLUE.argb(), config.glowColor(TracerTarget.VEX));
        assertThrows(IllegalArgumentException.class, () -> config.setColor(TracerTarget.VEX, false, OverlayColor.OFF));
        Files.writeString(file, "targets=HIVE_THIEF\nline.VEX=OFF\nglow.VEX=broken\nline.HIVE_THIEF=GREEN\n");
        config.load();
        assertEquals(EnumSet.of(TracerTarget.HIVE_THIEF), config.selectedTargets());
        assertEquals(OverlayColor.PURPLE.argb(), config.lineColor(TracerTarget.VEX));
        assertEquals(config.lineColor(TracerTarget.VEX), config.glowColor(TracerTarget.VEX));
        assertEquals(OverlayColor.GREEN.argb(), config.glowColor(TracerTarget.HIVE_THIEF));
    }
}

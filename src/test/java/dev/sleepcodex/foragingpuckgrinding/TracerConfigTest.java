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

    @Test void everyIndependentSelectionPersistsAndLegacyBothDoesNotAddNewTargets() throws Exception {
        Path file = directory.resolve("tracers.properties");
        Files.writeString(file, "enabled=true\ntarget=BOTH\n");
        TracerConfig config = new TracerConfig(file); config.load();
        assertEquals(java.util.EnumSet.of(TracerTarget.VEX, TracerTarget.BLUE_JAY), config.selectedTargets());
        for (int mask=0;mask<(1<<TracerTarget.values().length);mask++) {
            var selection=java.util.EnumSet.noneOf(TracerTarget.class);
            for (var target:TracerTarget.values()) if((mask&(1<<target.ordinal()))!=0)selection.add(target);
            config.setTargets(selection);
            TracerConfig restarted=new TracerConfig(file);restarted.load();
            assertEquals(selection,restarted.selectedTargets());assertTrue(restarted.enabled());
            var copy=restarted.selectedTargets();copy.clear();assertEquals(selection,restarted.selectedTargets());
        }
        Files.writeString(file,"enabled=true\ntarget=unknown\n");config.load();
        assertEquals(java.util.EnumSet.of(TracerTarget.VEX),config.selectedTargets());
        Files.writeString(file,"enabled=true\n");config.load();
        assertEquals(java.util.EnumSet.of(TracerTarget.VEX),config.selectedTargets());
    }

    @Test void allKeywordSelectsParchedAndPollendartToo() throws Exception {
        Path file = directory.resolve("tracers.properties");
        Files.writeString(file, "enabled=true\ntargets=ALL\n");
        TracerConfig config = new TracerConfig(file); config.load();
        assertEquals(java.util.EnumSet.allOf(TracerTarget.class), config.selectedTargets());
        Files.writeString(file, "enabled=true\ntargets=PARCHED,pollendart\n");
        config.load();
        assertEquals(java.util.EnumSet.of(TracerTarget.PARCHED, TracerTarget.POLLENDART), config.selectedTargets());
    }
}

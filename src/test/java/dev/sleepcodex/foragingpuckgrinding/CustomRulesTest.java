package dev.sleepcodex.foragingpuckgrinding;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CustomRulesTest {
    @TempDir Path directory;
    private CustomTracker rule(CustomTracker.Mode mode, String query) {
        return new CustomTracker(UUID.randomUUID(), mode, query, true, OverlayColor.CYAN, OverlayColor.MATCH);
    }

    @Test void namesTypesPriorityAndPersistence() throws Exception {
        Path file = directory.resolve("settings.properties");
        Files.writeString(file, "enabled=true\ntarget=BOTH\ncustom.ids=broken\n");
        var config = new TracerConfig(file);
        config.load();
        assertEquals(Set.of(TracerTarget.VEX, TracerTarget.BLUE_JAY), config.selectedTargets());
        assertTrue(config.custom().all.isEmpty());
        config.setTargets(Set.of());
        var family = rule(CustomTracker.Mode.NAMETAG, "Revenant");
        var boss = rule(CustomTracker.Mode.NAMETAG, "Revenant Horror");
        var mob = rule(CustomTracker.Mode.MOB_TYPE, "Wither Skeleton");
        config.putCustom(family); config.putCustom(boss); config.putCustom(mob);
        assertEquals("minecraft:wither_skeleton", mob.query());
        assertEquals(family, config.custom().name("§c[Lv 500] REVE§bNANT  Horror 2M❤"));
        assertEquals(family, config.custom().name("Revenant Sycophant"));
        assertNull(config.custom().name("Tarantula Beast"));
        assertEquals(mob, config.custom().type("minecraft:wither_skeleton"));
        assertNull(config.custom().type("minecraft:skeleton"));
        config.putCustom(family.toggled());
        assertEquals(boss, config.custom().name("Revenant Horror"));
        assertFalse(config.isSelected(family));
        var edit = new CustomTracker(boss.id(), boss.mode(), "Honeybuzz", true, OverlayColor.RED, OverlayColor.OFF);
        config.putCustom(edit);
        config.removeCustom(mob.id());
        config.load();
        assertEquals(List.of(family.toggled(), edit), config.custom().all);
        assertTrue(config.hasTargets());
        assertNull(config.custom().name("Revenant Horror"));
        assertEquals(0, config.glowColor(edit));
        config.removeCustom(edit.id());
        assertFalse(config.hasTargets());
        assertThrows(IllegalArgumentException.class, () -> rule(CustomTracker.Mode.NAMETAG, "§c "));
    }

    @Test void tierFiveUsesItsSlayerNameAsAnAlias() {
        for (String query : List.of("ReV", "Revenant", "Revenant Horror", "Revenant Horror Tier V")) {
            var tracker = rule(CustomTracker.Mode.NAMETAG, query);
            var rules = new CustomRules(List.of(tracker));
            assertSame(tracker, rules.name("☠ [Lv377] §cAtoned Horror 10M❤"));
            assertNull(rules.name("[Lv271] Atoned Champion"));
            assertNull(rules.name("Unatoned Horror"));
        }
        var narrow = rule(CustomTracker.Mode.NAMETAG, "Revenant Sycophant");
        assertNull(new CustomRules(List.of(narrow)).name("Atoned Horror"));
    }

    @Test void failedSaveKeepsPreviousSnapshot() throws Exception {
        Path blocked = Files.writeString(directory.resolve("file"), "occupied");
        var config = new TracerConfig(blocked.resolve("subfile"));
        assertThrows(java.io.IOException.class, () -> config.putCustom(rule(CustomTracker.Mode.NAMETAG, "Revenant")));
        assertTrue(config.custom().all.isEmpty());
    }
}

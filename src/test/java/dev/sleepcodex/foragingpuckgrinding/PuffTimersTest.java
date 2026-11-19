package dev.sleepcodex.foragingpuckgrinding;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PuffTimersTest {
    @Test void acceptsMobAndAreaLabels() {
        assertTrue(PuffTimers.isPuff("§8[Lv7] §aPuff §c100/100❤"));
        assertTrue(PuffTimers.isPuff("Puff"));
        assertFalse(PuffTimers.isPuff("Puff Crux"));
        assertFalse(PuffTimers.isPuff("Puffercloud"));
        assertTrue(PuffTimers.isRift("§7ф §aThe Mountaintop"));
        assertTrue(PuffTimers.isRift("Area: The Rift"));
        assertFalse(PuffTimers.isRift("Objective: Enter The Rift"));
        assertFalse(PuffTimers.isRift(null));
    }

    @Test void keepsEachAgeAndWarnsOnceUntilTheOldPuffsLeave() {
        var clock = new PuffTimers();
        var first = UUID.randomUUID();
        var later = UUID.randomUUID();
        assertFalse(clock.update(Set.of(first), 0));
        assertFalse(clock.update(Set.of(first, later), 20_000));
        assertFalse(clock.update(Set.of(first, later), 74_999));
        assertEquals(26, clock.secondsLeft());
        assertTrue(clock.update(Set.of(first, later), 75_000));
        assertFalse(clock.update(Set.of(first, later), 75_001));
        assertEquals(2, clock.visible());
        assertFalse(clock.update(Set.of(later), 76_000));
        assertEquals(44, clock.secondsLeft());
        assertFalse(clock.update(Set.of(), 79_000));
        assertEquals(0, clock.visible());
        assertFalse(clock.update(Set.of(later), 80_000));
        assertEquals(40, clock.secondsLeft());
        assertTrue(clock.update(Set.of(later), 95_000));
        assertFalse(clock.update(Set.of(later), 122_000));
        assertEquals(0, clock.secondsLeft());
        clock.clear();
        assertFalse(clock.update(Set.of(later), 123_000));
        assertEquals(100, clock.secondsLeft());
    }
}

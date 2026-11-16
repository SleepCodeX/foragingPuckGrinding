package dev.sleepcodex.foragingpuckgrinding;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TorrhusNamesTest {
    @Test void acceptsActualLocationLabelsWithFormatting() {
        assertTrue(TorrhusNames.isLocationLabel("§bArea: §aTorrhus Canyon"));
        assertTrue(TorrhusNames.isLocationLabel(" ⏣ Torrhus Heights"));
        assertTrue(TorrhusNames.isLocationLabel("⏣ Torrhus Springs"));
    }

    @Test void rejectsUnrelatedPlacesAndIncidentalText() {
        assertFalse(TorrhusNames.isLocationLabel("Blue Jay"));
        assertFalse(TorrhusNames.isLocationLabel("Visit Torrhus Canyon"));
        assertFalse(TorrhusNames.isLocationLabel("Area: Safari"));
        assertFalse(TorrhusNames.isLocationLabel("⏣ Private Island"));
        assertFalse(TorrhusNames.isLocationLabel(null));
    }
}

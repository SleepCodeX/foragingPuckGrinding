package dev.sleepcodex.foragingpuckgrinding;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/** First sighting is the latest possible spawn time. The server never sends a despawn deadline. */
final class PuffTimers {
    static final int LIFETIME_SECONDS = 100;
    static final int WARNING_SECONDS = 25;
    private static final Pattern FORMATTING = Pattern.compile("§.|[\\p{Cf}]");
    private static final Pattern NAME = Pattern.compile(
            "(?:\\[Lv\\s*\\d+\\]\\s*)?Puff(?:\\s+[\\d,.kKmM/]+\\s*[❤♥]?)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern AREA = Pattern.compile(
            "(?:(?:Area|Zone):|[фϕφ⏣])?\\s*(?:The\\s+)?(?:Mountaintop|Rift)", Pattern.CASE_INSENSITIVE);
    private final Map<UUID, Long> firstSeen = new HashMap<>();
    private int visible;
    private int secondsLeft;
    private boolean warningActive;

    static boolean isPuff(String name) {
        return NAME.matcher(FORMATTING.matcher(name).replaceAll("").strip()).matches();
    }

    static boolean isRift(String label) {
        return label != null && AREA.matcher(FORMATTING.matcher(label).replaceAll("").strip()).matches();
    }

    /** Returns true once as the oldest visible Puff enters the warning window. */
    boolean update(Set<UUID> seen, long now) {
        long lifetime = LIFETIME_SECONDS * 1000L;
        firstSeen.entrySet().removeIf(e -> !seen.contains(e.getKey()) && now - e.getValue() >= lifetime);
        long oldest = now;
        for (UUID id : seen) {
            Long start = firstSeen.putIfAbsent(id, now);
            if (start != null) oldest = Math.min(oldest, start);
        }
        visible = seen.size();
        long remaining = Math.max(0, lifetime - (now - oldest));
        secondsLeft = (int) Math.ceilDiv(remaining, 1000L);
        boolean warn = visible > 0 && secondsLeft <= WARNING_SECONDS;
        boolean entered = warn && !warningActive;
        warningActive = warn;
        return entered;
    }

    int visible() { return visible; }
    int secondsLeft() { return secondsLeft; }
    boolean warningActive() { return warningActive; }

    void clear() {
        firstSeen.clear();
        visible = 0;
        secondsLeft = 0;
        warningActive = false;
    }
}

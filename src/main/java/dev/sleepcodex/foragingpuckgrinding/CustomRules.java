package dev.sleepcodex.foragingpuckgrinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/** Immutable configuration snapshot. Rebuild the match indexes only when the user changes rules. */
final class CustomRules {
    private record Named(String needle, CustomTracker rule) { }
    final List<CustomTracker> all;
    private final List<Named> names = new ArrayList<>();
    private final Map<String, CustomTracker> types = new HashMap<>();
    private final Map<CustomTracker, Integer> order = new HashMap<>();
    private final Map<UUID, CustomTracker> byId = new HashMap<>();

    CustomRules(List<CustomTracker> rules) {
        all = List.copyOf(rules);
        for (CustomTracker rule : all) {
            order.put(rule, order.size());
            byId.put(rule.id(), rule);
            if (!rule.enabled()) continue;
            if (rule.mode() == CustomTracker.Mode.MOB_TYPE) types.putIfAbsent(rule.query(), rule);
            else names.add(new Named(CustomTracker.normalizedName(rule.query()), rule));
        }
    }

    boolean preferName(CustomTracker candidate, TrackingRule previous) {
        return !(previous instanceof CustomTracker other) || order.get(candidate) < order.get(other);
    }
    boolean hasNames() { return !names.isEmpty(); }
    boolean hasTypes() { return !types.isEmpty(); }
    boolean current(CustomTracker rule) { return rule.enabled() && byId.get(rule.id()) == rule; }
    CustomTracker type(String key) { return types.get(key); }
    CustomTracker name(String raw) {
        if (names.isEmpty()) return null;
        String normalized = CustomTracker.normalizedName(raw);
        for (Named name : names) if (normalized.contains(name.needle())) return name.rule();
        return null;
    }

    static CustomRules read(Properties p) {
        var rules = new ArrayList<CustomTracker>();
        var seen = new HashSet<UUID>();
        for (String token : p.getProperty("custom.ids", "").split(",")) {
            if (token.isBlank()) continue;
            try {
                UUID id = UUID.fromString(token.trim());
                String prefix = "custom." + id + ".";
                var rule = new CustomTracker(id, CustomTracker.Mode.valueOf(p.getProperty(prefix + "mode", "")),
                        p.getProperty(prefix + "query", ""), Boolean.parseBoolean(p.getProperty(prefix + "enabled", "true")),
                        OverlayColor.parse(p.getProperty(prefix + "line"), OverlayColor.CYAN),
                        OverlayColor.parse(p.getProperty(prefix + "glow"), OverlayColor.MATCH));
                if (seen.add(id)) rules.add(rule);
            } catch (IllegalArgumentException ignored) {
                // An invalid entry must not hide other valid custom or built-in rules.
            }
        }
        return new CustomRules(rules);
    }

    void write(Properties p) {
        var ids = new java.util.StringJoiner(",");
        for (CustomTracker rule : all) {
            ids.add(rule.id().toString());
            String prefix = "custom." + rule.id() + ".";
            p.setProperty(prefix + "mode", rule.mode().name());
            p.setProperty(prefix + "query", rule.query());
            p.setProperty(prefix + "enabled", Boolean.toString(rule.enabled()));
            p.setProperty(prefix + "line", rule.line().name());
            p.setProperty(prefix + "glow", rule.glow().name());
        }
        p.setProperty("custom.ids", ids.toString());
    }
}

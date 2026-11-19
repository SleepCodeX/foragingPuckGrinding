package dev.sleepcodex.foragingpuckgrinding;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Master visibility and independent mob choices, with migration from the older selector. */
public final class TracerConfig {
    private CustomRules custom = new CustomRules(java.util.List.of());
    private final Path path;
    private boolean enabled;
    private boolean puffTimer;
    private boolean puffReminder = true;
    private EnumSet<TracerTarget> targets = EnumSet.of(TracerTarget.VEX);
    private final EnumMap<TracerTarget, OverlayColor> lines = new EnumMap<>(TracerTarget.class);
    private final EnumMap<TracerTarget, OverlayColor> glows = new EnumMap<>(TracerTarget.class);

    public TracerConfig(Path path) {
        this.path = path;
        resetColors();
    }

    private void resetColors() {
        for (TracerTarget target : TracerTarget.values()) {
            lines.put(target, OverlayColor.defaultFor(target));
            glows.put(target, OverlayColor.MATCH);
        }
    }

    public void load() throws IOException {
        enabled = false;
        puffTimer = false;
        puffReminder = true;
        targets = EnumSet.of(TracerTarget.VEX);
        resetColors();
        custom = new CustomRules(java.util.List.of());
        if (!Files.exists(path)) return;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        custom = CustomRules.read(properties);
        puffTimer = Boolean.parseBoolean(properties.getProperty("puff.timer", "false").trim());
        puffReminder = Boolean.parseBoolean(properties.getProperty("puff.reminder", "true").trim());
        enabled = Boolean.parseBoolean(properties.getProperty("enabled", "false").trim());
        for (TracerTarget target : TracerTarget.values()) {
            OverlayColor line = OverlayColor.parse(properties.getProperty("line." + target.name()), OverlayColor.defaultFor(target));
            lines.put(target, line.isSolid() ? line : OverlayColor.defaultFor(target));
            glows.put(target, OverlayColor.parse(properties.getProperty("glow." + target.name()), OverlayColor.MATCH));
        }
        String value = properties.getProperty("targets");
        if (value == null) value = properties.getProperty("target", "VEX");
        value = value.strip().toUpperCase(Locale.ROOT);
        targets.clear();
        if (value.equals("BOTH")) {
            targets.add(TracerTarget.VEX); targets.add(TracerTarget.BLUE_JAY);
        } else if (value.equals("ALL")) {
            targets.addAll(EnumSet.allOf(TracerTarget.class));
        } else if (!value.isBlank() && !value.equals("NONE")) {
            for (String token : value.split(",")) {
                TracerTarget target = TracerTarget.parse(token);
                if (target != null) targets.add(target);
            }
            if (targets.isEmpty()) targets.add(TracerTarget.VEX);
        }
    }

    public boolean enabled() {
        return enabled;
    }

    boolean puffTimer() { return puffTimer; }
    boolean puffReminder() { return puffReminder; }

    void setPuffOptions(boolean timer, boolean reminder) throws IOException {
        save(custom, timer, reminder);
        puffTimer = timer;
        puffReminder = reminder;
    }

    public void setEnabled(boolean value) throws IOException {
        enabled = value;
        save();
    }

    public boolean isSelected(TracerTarget target) { return targets.contains(target); }
    public boolean hasTargets() { return !targets.isEmpty() || custom.hasNames() || custom.hasTypes(); }
    public EnumSet<TracerTarget> selectedTargets() { return targets.clone(); }

    public OverlayColor lineChoice(TracerTarget target) { return lines.get(target); }
    public OverlayColor glowChoice(TracerTarget target) { return glows.get(target); }
    public int lineColor(TracerTarget target) { return lineChoice(target).argb(); }
    public int glowColor(TracerTarget target) {
        OverlayColor color = glowChoice(target);
        return color == OverlayColor.MATCH ? lineColor(target) : color.argb();
    }
    public void setColor(TracerTarget target, boolean glow, OverlayColor value) throws IOException {
        java.util.Objects.requireNonNull(target);
        java.util.Objects.requireNonNull(value);
        if (!glow && !value.isSolid()) throw new IllegalArgumentException("Lines need a visible color");
        (glow ? glows : lines).put(target, value);
        save();
    }

    public void setTargets(Set<TracerTarget> values) throws IOException {
        java.util.Objects.requireNonNull(values);
        targets = values.isEmpty() ? EnumSet.noneOf(TracerTarget.class) : EnumSet.copyOf(values);
        save();
    }

    CustomRules custom() { return custom; }

    void putCustom(CustomTracker rule) throws IOException {
        var next = new java.util.ArrayList<>(custom.all);
        for (int i = 0; i < next.size(); i++) {
            if (next.get(i).id().equals(rule.id())) {
                next.set(i, rule);
                storeCustom(next);
                return;
            }
        }
        next.add(rule);
        storeCustom(next);
    }

    void removeCustom(java.util.UUID id) throws IOException {
        var next = new java.util.ArrayList<>(custom.all);
        if (next.removeIf(rule -> rule.id().equals(id))) storeCustom(next);
    }

    private void storeCustom(java.util.List<CustomTracker> rules) throws IOException {
        var next = new CustomRules(rules);
        save(next);
        custom = next;
    }

    boolean isSelected(TrackingRule rule) {
        return rule instanceof TracerTarget preset ? isSelected(preset) : custom.current((CustomTracker) rule);
    }

    int lineColor(TrackingRule rule) {
        return rule instanceof TracerTarget preset ? lineColor(preset) : ((CustomTracker) rule).line().argb();
    }

    int glowColor(TrackingRule rule) {
        if (rule instanceof TracerTarget preset) return glowColor(preset);
        var tracker = (CustomTracker) rule;
        return (tracker.glow() == OverlayColor.MATCH ? tracker.line() : tracker.glow()).argb();
    }

    private void save() throws IOException { save(custom); }

    private void save(CustomRules savedCustom) throws IOException {
        save(savedCustom, puffTimer, puffReminder);
    }

    private void save(CustomRules savedCustom, boolean timer, boolean reminder) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "vex-tracer-", ".tmp");
        try {
            Properties properties = new Properties();
            savedCustom.write(properties);
            properties.setProperty("puff.timer", Boolean.toString(timer));
            properties.setProperty("puff.reminder", Boolean.toString(reminder));
            properties.setProperty("enabled", Boolean.toString(enabled));
            properties.setProperty("targets", targets.stream().map(Enum::name).collect(Collectors.joining(",")));
            for (TracerTarget target : TracerTarget.values()) {
                properties.setProperty("line." + target.name(), lineChoice(target).name());
                properties.setProperty("glow." + target.name(), glowChoice(target).name());
            }
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                properties.store(writer, "Hunting Helper: tracers");
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}

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
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Master visibility and independent mob choices, with migration from the older selector. */
public final class TracerConfig {
    private final Path path;
    private boolean enabled;
    private EnumSet<TracerTarget> targets = EnumSet.of(TracerTarget.VEX);

    public TracerConfig(Path path) {
        this.path = path;
    }

    public void load() throws IOException {
        enabled = false;
        targets = EnumSet.of(TracerTarget.VEX);
        if (!Files.exists(path)) return;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        enabled = Boolean.parseBoolean(properties.getProperty("enabled", "false").trim());
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

    public void setEnabled(boolean value) throws IOException {
        enabled = value;
        save();
    }

    public boolean isSelected(TracerTarget target) { return targets.contains(target); }
    public boolean hasTargets() { return !targets.isEmpty(); }
    public EnumSet<TracerTarget> selectedTargets() { return targets.clone(); }

    public void setTargets(Set<TracerTarget> values) throws IOException {
        java.util.Objects.requireNonNull(values);
        targets = values.isEmpty() ? EnumSet.noneOf(TracerTarget.class) : EnumSet.copyOf(values);
        save();
    }

    private void save() throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "vex-tracer-", ".tmp");
        try {
            Properties properties = new Properties();
            properties.setProperty("enabled", Boolean.toString(enabled));
            properties.setProperty("targets", targets.stream().map(Enum::name).collect(Collectors.joining(",")));
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

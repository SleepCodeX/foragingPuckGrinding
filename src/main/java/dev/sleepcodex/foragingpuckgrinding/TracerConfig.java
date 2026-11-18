package dev.sleepcodex.foragingpuckgrinding;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** A persistent setting shared by the keybind and command. */
public final class TracerConfig {
    private final Path path;
    private boolean enabled;
    private TracerTarget target = TracerTarget.VEX;

    public TracerConfig(Path path) {
        this.path = path;
    }

    public void load() throws IOException {
        enabled = false;
        target = TracerTarget.VEX;
        if (!Files.exists(path)) return;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        enabled = Boolean.parseBoolean(properties.getProperty("enabled", "false").trim());
        target = TracerTarget.parse(properties.getProperty("target", "VEX"));
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean value) throws IOException {
        enabled = value;
        save();
    }

    public TracerTarget target() { return target; }

    public void setTarget(TracerTarget value) throws IOException {
        target = java.util.Objects.requireNonNull(value);
        save();
    }

    private void save() throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "vex-tracer-", ".tmp");
        try {
            Properties properties = new Properties();
            properties.setProperty("enabled", Boolean.toString(enabled));
            properties.setProperty("target", target.name());
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

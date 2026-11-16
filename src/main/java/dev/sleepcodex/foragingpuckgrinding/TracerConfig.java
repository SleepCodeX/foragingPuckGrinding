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

    public TracerConfig(Path path) {
        this.path = path;
    }

    public void load() throws IOException {
        enabled = false;
        if (!Files.exists(path)) return;
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        enabled = Boolean.parseBoolean(properties.getProperty("enabled", "false").trim());
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean value) throws IOException {
        enabled = value;
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "vex-tracer-", ".tmp");
        try {
            Properties properties = new Properties();
            properties.setProperty("enabled", Boolean.toString(value));
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                properties.store(writer, "Foraging Puck Grinding: Vex tracers");
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

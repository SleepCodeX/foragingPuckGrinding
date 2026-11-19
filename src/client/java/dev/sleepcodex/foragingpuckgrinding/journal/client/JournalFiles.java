package dev.sleepcodex.foragingpuckgrinding.journal.client;

import java.io.IOException;
import java.nio.file.*;

public final class JournalFiles {
    private JournalFiles() {}
    public static void write(Path file, String json) throws IOException {
        Files.createDirectories(file.getParent());
        Path staging = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
        try {
            Files.writeString(staging, json);
            try {
                Files.move(staging, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(staging, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(staging);
        }
    }
}

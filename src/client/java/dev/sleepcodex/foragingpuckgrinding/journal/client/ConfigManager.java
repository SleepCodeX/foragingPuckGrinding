package dev.sleepcodex.foragingpuckgrinding.journal.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.sleepcodex.foragingpuckgrinding.FieldJournalScreen;
import dev.sleepcodex.foragingpuckgrinding.journal.CritterMod;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hunting-helper-field-journal.json");
    private static final CritterConfig INSTANCE = load();

    private ConfigManager() {}
    public static CritterConfig get() { return INSTANCE; }
    public static Screen createScreen(Screen parent) { return new FieldJournalScreen(parent); }

    private static CritterConfig load() {
        if (Files.isRegularFile(FILE)) {
            try (var reader = Files.newBufferedReader(FILE)) {
                CritterConfig c = GSON.fromJson(reader, CritterConfig.class);
                if (c != null && c.display != null && c.alerts != null && c.party != null && c.profit != null && c.advanced != null) return c;
            } catch (IOException | RuntimeException error) {
                CritterMod.LOGGER.warn("Could not read field journal settings", error);
            }
        }
        return new CritterConfig();
    }

    public static boolean save() {
        try {
            JournalFiles.write(FILE, GSON.toJson(INSTANCE));
            return true;
        } catch (IOException error) {
            CritterMod.LOGGER.warn("Could not save field journal settings", error);
            return false;
        }
    }
}

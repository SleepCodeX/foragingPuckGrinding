package dev.sleepcodex.foragingpuckgrinding;

import java.nio.file.Files;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.johnceo.sparklingmutuals.config.ModUpdates;
import net.johnceo.sparklingmutuals.config.SettingsScreens;
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels;

/** The Sparkling Mutuals source compiled into Hunting Helper initialized like the standalone mod, with HH's UI. */
final class SparklingMutualsGameChecks {
    static void run(ClientGameTestContext c) {
        c.runOnClient(client -> {
            var loader = FabricLoader.getInstance();
            if (loader.isModLoaded("sparkling-mutuals"))
                throw new AssertionError("Sparkling Mutuals must ship as Hunting Helper source, not as a separate mod");
            if (!Files.isDirectory(loader.getConfigDir().resolve("sparkling-mutuals")))
                throw new AssertionError("Sparkling Mutuals did not initialize its config folder");
            if (!Files.isRegularFile(loader.getConfigDir().resolve("sparkling-mutuals/sparkling-mutuals-hunting.properties")))
                throw new AssertionError("Hunting settings were not written next to Sparkling Mutuals' settings");
            var dispatcher = ClientCommands.getActiveDispatcher();
            if (dispatcher == null || dispatcher.getRoot().getChild("sm") == null)
                throw new AssertionError("Sparkling Mutuals /sm command is not registered");
            if (!(SettingsScreens.INSTANCE.create(null, null) instanceof SafariSettingsScreen))
                throw new AssertionError("Sparkling Mutuals settings must open Hunting Helper's screen");
            if (!ModUpdates.REPOSITORY.endsWith("SleepCodeX/foragingPuckGrinding"))
                throw new AssertionError("The update check must follow Hunting Helper releases");
            // SM's own panels are SM's again: no Hunting Helper override in full-clear mode.
            var fields = java.util.Arrays.stream(SafariPanels.class.getDeclaredFields()).map(java.lang.reflect.Field::getName).toList();
            if (fields.contains("fullClearProgress") || fields.contains("fullClearMissing"))
                throw new AssertionError("Full-clear panel hooks should be gone");
        });
    }
}

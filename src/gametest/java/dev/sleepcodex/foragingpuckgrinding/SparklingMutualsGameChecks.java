package dev.sleepcodex.foragingpuckgrinding;

import java.nio.file.Files;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.johnceo.sparklingmutuals.safari.ui.SafariPanels;

/** The Sparkling Mutuals source compiled into Hunting Helper initialized like the standalone mod. */
final class SparklingMutualsGameChecks {
    static void run(ClientGameTestContext c) {
        c.runOnClient(client -> {
            var loader = FabricLoader.getInstance();
            if (loader.isModLoaded("sparkling-mutuals"))
                throw new AssertionError("Sparkling Mutuals must ship as Hunting Helper source, not as a separate mod");
            if (!Files.isDirectory(loader.getConfigDir().resolve("sparkling-mutuals")))
                throw new AssertionError("Sparkling Mutuals did not initialize its config folder");
            var dispatcher = ClientCommands.getActiveDispatcher();
            if (dispatcher == null || dispatcher.getRoot().getChild("sm") == null)
                throw new AssertionError("Sparkling Mutuals /sm command is not registered");
            if (SafariPanels.fullClearProgress == null || SafariPanels.fullClearMissing == null)
                throw new AssertionError("Hunting Helper full-clear panels are not installed");
        });
    }
}

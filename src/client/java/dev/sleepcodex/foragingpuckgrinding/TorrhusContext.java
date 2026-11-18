package dev.sleepcodex.foragingpuckgrinding;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.scores.DisplaySlot;

/** A small tick-side location cache; no scoreboard or tab matching in frame rendering. */
final class TorrhusContext {
    private ClientLevel level;
    private int remainingTicks;
    private boolean present;

    boolean update(Minecraft client, boolean needed) {
        if (client.level != level || !needed) {
            level = client.level;
            present = false;
            remainingTicks = 0;
        }
        if (!needed || level == null) return false;
        if (remainingTicks-- <= 0) {
            present = detect(client);
            remainingTicks = 9;
        }
        return present;
    }

    private boolean detect(Minecraft client) {
        var scoreboard = level.getScoreboard();
        var sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar != null) {
            for (var entry : scoreboard.listPlayerScores(sidebar)) {
                if (TorrhusNames.isLocationLabel(entry.ownerName().getString())) return true;
                var team = scoreboard.getPlayersTeam(entry.owner());
                if (team != null && TorrhusNames.isLocationLabel(
                        team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString())) return true;
            }
        }
        var connection = client.getConnection();
        if (connection != null) {
            for (var player : connection.getOnlinePlayers()) {
                var label = player.getTabListDisplayName();
                if (label != null && TorrhusNames.isLocationLabel(label.getString())) return true;
            }
        }
        return false;
    }
}

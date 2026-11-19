package dev.sleepcodex.foragingpuckgrinding;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.scores.DisplaySlot;

/** A small tick-side location cache; no scoreboard or tab matching in frame rendering. */
final class TorrhusContext {
    private ClientLevel level;
    private int remainingTicks;
    private boolean present;
    /** The sidebar or tab text that matched last, for the debug log. */
    private String matched = "";

    boolean update(Minecraft client, boolean needed) {
        if (client.level != level || !needed) {
            level = client.level;
            present = false;
            remainingTicks = 0;
        }
        if (!needed || level == null) return false;
        if (remainingTicks-- <= 0) {
            boolean was = present;
            present = detect(client);
            remainingTicks = 9;
            if (present != was) {
                if (present) HuntingDebug.LOG.info("[Hunting Helper] Entered Torrhus via {}", matched);
                else HuntingDebug.LOG.info("[Hunting Helper] Left Torrhus (no matching Area/Zone label any more)");
            }
        }
        return present;
    }

    String matched() { return present ? matched : ""; }

    private boolean detect(Minecraft client) {
        var scoreboard = level.getScoreboard();
        var sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar != null) {
            for (var entry : scoreboard.listPlayerScores(sidebar)) {
                String owner = entry.ownerName().getString();
                if (TorrhusNames.isLocationLabel(owner)) { matched = "sidebar: " + owner; return true; }
                var team = scoreboard.getPlayersTeam(entry.owner());
                if (team == null) continue;
                String text = team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();
                if (TorrhusNames.isLocationLabel(text)) { matched = "sidebar: " + text; return true; }
            }
        }
        var connection = client.getConnection();
        if (connection != null) {
            for (var player : connection.getOnlinePlayers()) {
                var label = player.getTabListDisplayName();
                if (label != null && TorrhusNames.isLocationLabel(label.getString())) {
                    matched = "tab list: " + label.getString();
                    return true;
                }
            }
        }
        return false;
    }
}

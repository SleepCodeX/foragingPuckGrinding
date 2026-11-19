package dev.sleepcodex.foragingpuckgrinding;

import net.johnceo.sparklingmutuals.hud.SkyblockSidebar;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

final class PuffCruxTracker {
    private final TracerConfig config;
    private final PuffTimers timers = new PuffTimers();
    private final Set<UUID> seen = new HashSet<>();
    private ClientLevel level;
    private boolean inRift;
    private int untilAreaCheck;
    private int untilScan;

    PuffCruxTracker(TracerConfig config) { this.config = config; }

    int visibleCount() { return timers.visible(); }

    void tick(Minecraft client) {
        ClientLevel current = config.puffTimer() && client.player != null ? client.level : null;
        if (level != current) {
            level = current;
            inRift = false;
            untilAreaCheck = untilScan = 0;
            seen.clear();
            timers.clear();
        }
        if (level == null) return;
        if (untilAreaCheck-- <= 0) {
            untilAreaCheck = 9;
            inRift = PuffTimers.isRift(SkyblockSidebar.INSTANCE.areaName(client))
                    || SkyblockSidebar.INSTANCE.lines(client).stream().anyMatch(PuffTimers::isRift);
            if (!inRift) {
                timers.clear();
                seen.clear();
                untilScan = 0;
            }
        }
        if (!inRift || untilScan-- > 0) return;
        untilScan = 3;
        seen.clear();
        for (var entity : level.entitiesForRendering()) {
            if (!entity.isAlive() || entity instanceof Player || entity instanceof ItemEntity) continue;
            var label = entity instanceof Display.TextDisplay display ? display.getText() : entity.getCustomName();
            if (label != null && PuffTimers.isPuff(label.getString())) seen.add(entity.getUUID());
        }
        if (timers.update(seen, Util.getMillis()) && config.puffReminder()) {
            client.player.sendOverlayMessage(Component.literal("Pop your Puffs soon"));
        }
    }

    void draw(GuiGraphicsExtractor g) {
        var client = Minecraft.getInstance();
        if (!config.puffTimer() || timers.visible() == 0 || client.gui.hud.isHidden()) return;
        int total = timers.visible();
        String count = total == 1 ? "1 Puff" : total + " Puffs";
        String expiry = timers.secondsLeft() == 0 ? "Despawn due" : "Oldest ~" + timers.secondsLeft() + "s";
        int x = 8, y = 8;
        int width = 18 + Math.max(client.font.width(count), client.font.width(expiry));
        var canvas = new MenuCanvas(g, 1);
        canvas.round(x, y, width, 29, 5, 0xEC202328);
        canvas.round(x + 3, y + 5, 2, 19, 1, timers.warningActive() ? 0xFFFFB46B : MenuTheme.ACCENT);
        g.text(client.font, count, x + 9, y + 4, MenuTheme.TEXT, false);
        g.text(client.font, expiry, x + 9, y + 16, timers.warningActive() ? 0xFFFFB46B : MenuTheme.MUTED, false);
    }
}

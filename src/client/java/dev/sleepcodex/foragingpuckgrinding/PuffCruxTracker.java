package dev.sleepcodex.foragingpuckgrinding;

import net.johnceo.sparklingmutuals.hud.SkyblockSidebar;
import net.johnceo.sparklingmutuals.compat.WorldRenderCompat;
import net.johnceo.sparklingmutuals.safari.SafariIsland;
import net.johnceo.sparklingmutuals.safari.esp.SafariEspRules;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class PuffCruxTracker {
    // NEU-REPO mobs/puff.json equips PUFF_MONSTER on an invisible stand (zero-size markers included).
    private static final String PUFF_SKIN = "ba5f715a62a29054622412eb673dff548e35061ad845caf4317b1b2ee8f408f0";
    private final TracerConfig config;
    private final PuffTimers timers = new PuffTimers();
    private final LinkedHashMap<UUID, Entity> visible = new LinkedHashMap<>();
    private final ArrayList<Entity> models = new ArrayList<>();
    private final ArrayList<Entity> labels = new ArrayList<>();
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
            visible.clear();
            timers.clear();
        }
        if (level == null) return;
        String mode = SafariIsland.INSTANCE.getCurrentMode();
        if (untilAreaCheck-- <= 0) {
            untilAreaCheck = 9;
            inRift = PuffTimers.isRift(mode) || PuffTimers.isRift(SkyblockSidebar.INSTANCE.areaName(client))
                    || SkyblockSidebar.INSTANCE.lines(client).stream().anyMatch(PuffTimers::isRift);
        }
        if (mode != null && !PuffTimers.isRift(mode)) {
            visible.clear();
            timers.clear();
            return;
        }
        if (untilScan-- > 0) return;
        untilScan = 3;
        visible.clear();
        long now = Util.getMillis();
        for (var entity : level.entitiesForRendering()) {
            if (!entity.isAlive() || entity instanceof Player || entity instanceof ItemEntity) continue;
            if (isPuffModel(entity)) {
                models.add(entity);
                visible.put(entity.getUUID(), entity);
            } else if (inRift) {
                var label = entity instanceof Display.TextDisplay display ? display.getText() : entity.getCustomName();
                if (label != null && PuffTimers.isPuff(label.getString())) labels.add(entity);
            }
        }
        for (Entity label : labels) {
            Entity model = modelFor(label);
            if (model == null) visible.put(label.getUUID(), label);
            else timers.link(model.getUUID(), label.getUUID(), now);
        }
        labels.clear();
        models.clear();
        if (timers.update(visible.keySet(), now) && config.puffReminder()) {
            client.player.sendOverlayMessage(Component.literal("Pop your Puffs soon"));
        }
    }

    private static boolean isPuffModel(Entity entity) {
        ItemStack head;
        if (entity instanceof ArmorStand stand) head = stand.getItemBySlot(EquipmentSlot.HEAD);
        else if (entity instanceof Display.ItemDisplay display) head = display.getItemStack();
        else return false;
        if (!head.is(Items.PLAYER_HEAD)) return false;
        var profile = head.get(DataComponents.PROFILE);
        if (profile == null) return false;
        for (var property : profile.partialProfile().properties().get("textures")) {
            if (PUFF_SKIN.equals(SafariEspRules.INSTANCE.textureHash(property.value()))) return true;
        }
        return false;
    }

    private Entity modelFor(Entity label) {
        Entity best = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (Entity model : models) {
            if (label.getVehicle() == model || model.getVehicle() == label) return model;
            double dx = model.getX() - label.getX(), dz = model.getZ() - label.getZ();
            double height = label.getY() - model.getY();
            double horizontal = dx * dx + dz * dz;
            if (horizontal > .75 * .75 || height < -.5 || height > 2.75) continue;
            double distance = horizontal + height * height;
            if (distance < nearest) {
                best = model;
                nearest = distance;
            }
        }
        return best;
    }

    void drawLabels(LevelRenderContext context) {
        var client = Minecraft.getInstance();
        if (!config.puffTimer() || client.level != level || client.gui.hud.isHidden()) return;
        float delta = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        long now = Util.getMillis();
        for (Entity entity : visible.values()) {
            if (entity.isRemoved()) continue;
            var position = entity.getPosition(delta);
            double offset = entity instanceof ArmorStand stand ? (stand.isSmall() ? 1.2 : 2.1) : .45;
            int seconds = timers.remaining(entity.getUUID(), now);
            WorldRenderCompat.INSTANCE.label(context, seconds == 0 ? "Puff · due" : "Puff ~" + seconds + "s",
                    seconds <= PuffTimers.WARNING_SECONDS ? 0xFFFFB46B : MenuTheme.TEXT,
                    position.x, position.y + offset, position.z, .024F);
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

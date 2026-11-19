package dev.sleepcodex.foragingpuckgrinding;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import net.johnceo.sparklingmutuals.config.SafariConfigScreen;
import net.johnceo.sparklingmutuals.config.SafariSettings;
import net.johnceo.sparklingmutuals.config.SettingsTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The shared front door. Safari screens edit the upstream settings model directly. */
final class HuntingHomeScreen extends Screen {
    enum Section {
        MODES("Safari modes", "Unique, Sparkling and Full Clear", "Modes", "unique"),
        ESP("Critter highlights", "Glow, boxes and floor drops", "ESP General", "glow"),
        HUD("HUDs & alerts", "Progress panels and appearance", "HUDs", "progressHud"),
        CAPTURE("Capture tools", "Capsule paths and catch helpers", "Safari helpers", "capture"),
        PARTY("Party & sync", "Shared runs and party interactions", "Party Interactions", "biomeReports"),
        CONTEST("Miria contests", "Tickets, alerts and Honeyhives", "Miria contest", "ticketMode"),
        HISTORY("Run history", "Catches, profit and achievements", "History", "open"),
        MARKET("SkyBlock helpers", "Hunting Box, shards and skills", "SkyBlock", "helpers");

        final String label, detail, page, field;
        Section(String label, String detail, String page, String field) {
            this.label = label; this.detail = detail; this.page = page; this.field = field;
        }
        SettingsTarget target() { return new SettingsTarget(page, List.of(field)); }
    }

    private record Tile(Button button, String detail, boolean featured) { }
    private final List<Tile> tiles = new ArrayList<>();
    private final TracerSettingsScreen presets;
    private final TracerConfig config;
    private final LongSupplier clock;
    private long opened;
    private Button all, appearance, done;
    private int x, y, w, h, safariY;
    private boolean compact;

    HuntingHomeScreen(TracerSettingsScreen presets, TracerConfig config) {
        this(presets, config, System::nanoTime);
    }
    private HuntingHomeScreen(TracerSettingsScreen presets, TracerConfig config, LongSupplier clock) {
        super(Component.literal("Hunting Helper"));
        this.presets = presets; this.config = config; this.clock = clock; opened = clock.getAsLong();
    }
    HuntingHomeScreen withClock(LongSupplier source) { return new HuntingHomeScreen(presets, config, source); }

    void replay() { opened = clock.getAsLong(); Minecraft.getInstance().gui.setScreen(this); }

    void openCustom() { Minecraft.getInstance().gui.setScreen(new CustomLibraryScreen(this, config)); }

    private void openSafari(Section section) {
        Minecraft.getInstance().gui.setScreen(new SafariConfigScreen(new SafariSettings(),
                section == null ? null : section.target(), this));
    }

    @Override protected void init() {
        tiles.clear();
        w = Math.min(570, width - 20); h = Math.min(372, height - 16);
        x = (width - w) / 2; y = (height - h) / 2;
        compact = h < 285 || w < 402;
        int gutter = compact ? 5 : 9;
        int inset = compact ? 11 : 16;
        int col = (w - inset * 2 - gutter) / 2;
        int top = y + (compact ? 39 : 55);
        int featureHeight = compact ? 29 : 53;
        tile("Hunting presets", "Vex, Blue Jay and Torrhus mobs", x + inset, top, col, featureHeight, true, () -> {
            presets.settle();
            Minecraft.getInstance().gui.setScreen(presets);
        });
        String detail = config.custom().all.isEmpty() ? "Build a nametag or mob type rule" : config.custom().all.size() + " saved";
        tile("Custom trackers", detail, x + inset + col + gutter, top, col, featureHeight, true, this::openCustom);
        safariY = top + featureHeight + (compact ? 6 : 10);
        int listY = safariY + (compact ? 12 : 16);
        int footer = y + h - (compact ? 29 : 37);
        int rowGap = compact ? 3 : 5;
        int rowH = (footer - 7 - listY - rowGap * 3) / 4;
        for (Section section : Section.values()) {
            int i = section.ordinal();
            tile(section.label, section.detail, x + inset + (i % 2) * (col + gutter),
                    listY + (i / 2) * (rowH + rowGap), col, rowH, false, () -> openSafari(section));
        }
        all = button("All Safari settings", x + inset, footer, 116, compact ? 20 : 25, () -> openSafari(null));
        appearance = button("Appearance", all.getRight() + 6, footer, 76, compact ? 20 : 25,
                () -> Minecraft.getInstance().gui.setScreen(new AppearanceScreen(this, config)));
        done = button("Done", x + w - inset - 66, footer, 66, compact ? 20 : 25, this::onClose);
    }

    private Button button(String label, int x, int y, int w, int h, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> action.run()).bounds(x,y,w,h).build());
    }
    private void tile(String label, String detail, int x, int y, int w, int h, boolean featured, Runnable action) {
        tiles.add(new Tile(button(label,x,y,w,h,action), detail, featured));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        long elapsed = clock.getAsLong() - opened;
        double backdrop = MenuOpening.backdrop(elapsed);
        double travel = MenuOpening.travel(elapsed);
        double opacity = MenuOpening.content(elapsed);
        int hx = x + 27, hy = y + (compact ? 19 : 26);
        int badgeSize = compact ? 48 : 60;
        int sw = blend(badgeSize, w, travel), sh = blend(badgeSize, h, travel);
        int sx = (width - sw) / 2, sy = (height - sh) / 2;
        g.fill(0, 0, width, height, ((int) (backdrop * 151) << 24) | 0x090B10);
        var surface = new MenuCanvas(g, backdrop);
        surface.round(sx, sy + 4, sw, sh, 13, 0x69000000);
        surface.round(sx, sy, sw, sh, 13, MenuTheme.BORDER);
        surface.round(sx + 1, sy + 1, sw - 2, sh - 2, 12, MenuTheme.PANEL);
        surface.round(sx + 16, sy + 1, sw - 32, 1, 0, 0x5B91A1B1);
        var c = new MenuCanvas(g, opacity);
        g.enableScissor(sx + 2, sy + 2, sx + sw - 2, sy + sh - 2);
        MenuFont.opacity(opacity);
        try {
            MenuFont.text(g,"Hunting Helper",x+49,y+(compact?8:12),compact?1.15:1.45,MenuTheme.TEXT);
            MenuFont.text(g,"Settings",x+50,y+(compact?23:32),.74,MenuTheme.MUTED);
            String status=config.enabled()?"Tracers on":"Tracers off";
            int statusX = x + w - 21 - (int) MenuFont.width(status);
            c.round(statusX-10,hy-1,4,4,2,config.enabled()?OverlayColor.GREEN.argb():MenuTheme.MUTED);
            MenuFont.text(g,status,statusX,hy-3,.81,MenuTheme.MUTED);
            MenuFont.text(g,"Critter Safari",x+(compact?12:17),safariY,.84,MenuTheme.ACCENT);
            for (Tile tile:tiles) drawTile(g,c,tile,mx,my);
            footer(g,c,all,mx,my,false);
            footer(g,c,appearance,mx,my,false);
            footer(g,c,done,mx,my,true);
        } finally {
            MenuFont.opacity(1);
            g.disableScissor();
        }
        // The same icon is drawn exactly once, including the final, stationary header frame.
        int cx = blend(width / 2, hx, travel), cy = blend(height / 2, hy, travel);
        new MenuCanvas(g, backdrop).hivethief(config.style(), cx, cy,
                blend(badgeSize - 7, compact ? 28 : 36, travel));
    }

    private static int blend(int from, int to, double progress) {
        return (int) Math.round(from + (to - from) * progress);
    }

    private void drawTile(GuiGraphicsExtractor g, MenuCanvas c, Tile tile, int mx, int my) {
        Button b=tile.button();
        int bx=b.getX(), by=b.getY(), bw=b.getWidth(), bh=b.getHeight();
        boolean over=b.isFocused()||b.isMouseOver(mx,my);
        c.round(bx,by,bw,bh,7,over?MenuTheme.ACCENT:MenuTheme.BORDER);
        c.round(bx+1,by+1,bw-2,bh-2,6,over?MenuTheme.HOVER:tile.featured()?0xFF292E35:MenuTheme.CARD);
        int start=bx+(compact?8:12);
        if (tile.featured() && !compact) {
            int cy=by+bh/2;
            c.round(start,cy-13,26,26,6,0xFF1E2228);
            if (b==tiles.getFirst().button()) MobSymbols.draw(g,c,TracerTarget.VEX,start+13,cy,18);
            else {
                c.round(start+6,cy-6,13,9,2,MenuTheme.MUTED);
                c.round(start+8,cy-3,13,9,2,MenuTheme.ACCENT);
                c.round(start+11,cy,6,1,0,MenuTheme.CONTROL);
            }
            start+=34;
        }
        boolean twoLines=!compact && bh>=31;
        double scale=tile.featured()?1.02:.92;
        String label=MenuFont.ellipsis(b.getMessage().getString(),(b.getRight()-start-18)/scale);
        MenuFont.text(g,label,start,by+bh/2.-(twoLines?10:4),scale,MenuTheme.TEXT);
        if (twoLines) MenuFont.text(g,MenuFont.ellipsis(tile.detail(),(b.getRight()-start-16)/.75),
                start,by+bh/2.+3,.75,MenuTheme.MUTED);
        MenuFont.text(g,"›",b.getRight()-13,by+bh/2.-5,1,MenuTheme.ACCENT);
    }

    private void footer(GuiGraphicsExtractor g,MenuCanvas c,Button b,int mx,int my,boolean primary) {
        boolean over=b.isFocused()||b.isMouseOver(mx,my);
        c.round(b.getX(),b.getY(),b.getWidth(),b.getHeight(),5,primary?(over?MenuTheme.TEXT:MenuTheme.ACCENT)
                :(over?MenuTheme.CONTROL_HOVER:MenuTheme.CONTROL));
        String title=b.getMessage().getString();
        MenuFont.text(g,title,b.getX()+(b.getWidth()-MenuFont.width(title)*.84)/2,b.getY()+(b.getHeight()-9)/2.,
                .84,primary?0xFF171B20:MenuTheme.TEXT);
    }
    @Override public boolean isPauseScreen() { return false; }
}

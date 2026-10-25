package dev.drevss.drevclient.hud;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.module.client.ClickGuiModule;
import dev.drevss.drevclient.module.client.HudModule;
import dev.drevss.drevclient.util.DrevRender;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Hud {
    private final List<HudElement> elements = new ArrayList<>();

    public Hud() {
        elements.add(new Watermark());
        elements.add(new ModuleList());
        elements.add(new TextLine("Coords", 2, 400, true, this::coords));
        elements.add(new TextLine("FPS", 2, 14, true, () -> "FPS §f" + mc().getFps()));
        elements.add(new TextLine("Ping", 2, 24, true, this::ping));
        elements.add(new TextLine("Speed", 2, 34, true, this::speed));
        elements.add(new TextLine("Direction", 2, 390, true, this::direction));
        elements.add(new TextLine("Time", 2, 44, false, () -> "Time §f" + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))));
        elements.add(new TextLine("Server", 2, 54, false, this::server));
        elements.add(new TextLine("Durability", 2, 64, false, this::durability));
        elements.add(new TextLine("Totems", 2, 74, false, () -> "Totems §f" + InvUtil.count(s -> s.is(Items.TOTEM_OF_UNDYING))));
        elements.add(new Armor());
        elements.add(new Compass());
        elements.add(new Potions());
    }

    private static net.minecraft.client.Minecraft mc() { return net.minecraft.client.Minecraft.getInstance(); }

    public List<HudElement> elements() { return elements; }

    public HudElement get(String name) {
        for (HudElement e : elements) if (e.getName().equalsIgnoreCase(name)) return e;
        return null;
    }

    public void render(GuiGraphics g, float pt) {
        HudModule hud = DrevClient.get().modules().get(HudModule.class);
        if (!hud.isEnabled() || mc().screen instanceof dev.drevss.drevclient.gui.HudEditorScreen) return;
        for (HudElement e : elements) if (e.isVisible()) e.render(g, pt, false);
    }

    // ------------------------------------------------------------------
    // shared helpers
    // ------------------------------------------------------------------

    static HudModule settings() { return DrevClient.get().modules().get(HudModule.class); }

    static int color(int index) {
        HudModule s = settings();
        if (s.colorMode.is("White")) return 0xFFFFFFFF;
        if (s.colorMode.is("Accent")) return DrevClient.get().modules().get(ClickGuiModule.class).accent.getColor();
        return DrevRender.rainbow(index * 150L);
    }

    static void text(GuiGraphics g, String s, int x, int y, int color) {
        g.drawString(mc().font, s, x, y, color, settings().shadow.isOn());
    }

    static void background(GuiGraphics g, int x, int y, int w, int h) {
        if (settings().background.isOn()) g.fill(x - 2, y - 1, x + w + 2, y + h + 1, 0x70000000);
    }

    // ------------------------------------------------------------------
    // text providers
    // ------------------------------------------------------------------

    private String coords() {
        var p = mc().player;
        boolean nether = p.level().dimension() == Level.NETHER;
        double f = nether ? 8 : 0.125;
        String other = (nether ? "§7 [OW " : "§7 [N ") + String.format("%.0f, %.0f]", p.getX() * f, p.getZ() * f);
        return String.format("XYZ §f%.1f %.1f %.1f", p.getX(), p.getY(), p.getZ()) + other;
    }

    private String ping() {
        var conn = mc().getConnection();
        if (conn == null) return "Ping §f0ms";
        PlayerInfo info = conn.getPlayerInfo(mc().player.getUUID());
        return "Ping §f" + (info == null ? 0 : info.getLatency()) + "ms";
    }

    private String speed() {
        var p = mc().player;
        Vec3 d = p.position().subtract(p.xOld, p.yOld, p.zOld);
        double bps = Math.sqrt(d.x * d.x + d.z * d.z) * 20;
        return String.format("Speed §f%.1f b/s", bps);
    }

    private String direction() {
        Direction dir = mc().player.getDirection();
        String axis = switch (dir) {
            case NORTH -> "-Z";
            case SOUTH -> "+Z";
            case EAST -> "+X";
            case WEST -> "-X";
            default -> "";
        };
        String name = dir.getName();
        return "Facing §f" + Character.toUpperCase(name.charAt(0)) + name.substring(1) + " §7(" + axis + ")";
    }

    private String server() {
        if (mc().hasSingleplayerServer()) return "Server §fSingleplayer";
        var data = mc().getCurrentServer();
        int online = mc().getConnection() != null ? mc().getConnection().getOnlinePlayers().size() : 0;
        return "Server §f" + (data != null ? data.ip : "?") + " §7(" + online + " online)";
    }

    private String durability() {
        ItemStack s = mc().player.getMainHandItem();
        if (!s.isDamageableItem()) return "Durability §f-";
        int left = s.getMaxDamage() - s.getDamageValue();
        return "Durability §f" + left + "/" + s.getMaxDamage();
    }

    // ------------------------------------------------------------------
    // elements
    // ------------------------------------------------------------------

    static class TextLine extends HudElement {
        private final Supplier<String> supplier;

        TextLine(String name, int x, int y, boolean visible, Supplier<String> supplier) {
            super(name, x, y, visible);
            this.supplier = supplier;
        }

        @Override
        public void render(GuiGraphics g, float pt, boolean editing) {
            String s = supplier.get();
            Font font = mc.font;
            width = font.width(s);
            height = 9;
            int x = drawX(), y = drawY();
            // move up out of the way when chat is open at the bottom
            if (mc.screen instanceof ChatScreen && y > mc.getWindow().getGuiScaledHeight() - 30) y -= 14;
            background(g, x, y, width, height);
            text(g, s, x, y, color(y / 10));
        }
    }

    static class Watermark extends HudElement {
        Watermark() { super("Watermark", 2, 2, true); }

        @Override
        public void render(GuiGraphics g, float pt, boolean editing) {
            String s = "Drev§fClient §7v" + DrevClient.VERSION;
            width = mc.font.width(s);
            height = 9;
            int x = drawX(), y = drawY();
            background(g, x, y, width, height);
            text(g, s, x, y, color(0));
        }
    }

    static class ModuleList extends HudElement {
        ModuleList() { super("ModuleList", 10000, 2, true); }

        @Override
        public void render(GuiGraphics g, float pt, boolean editing) {
            Font font = mc.font;
            List<String> lines = new ArrayList<>();
            for (Module m : DrevClient.get().modules().all()) {
                if (!m.isEnabled() || !m.isDrawn()) continue;
                String info = m.getInfo();
                lines.add(info == null ? m.getName() : m.getName() + " §7" + info);
            }
            if (settings().sorting.is("Length")) lines.sort((a, b) -> font.width(b) - font.width(a));
            else lines.sort(String.CASE_INSENSITIVE_ORDER);
            if (lines.isEmpty() && editing) lines.add("Module list");

            width = 60;
            for (String l : lines) width = Math.max(width, font.width(l));
            height = Math.max(10, lines.size() * 10);

            int x = drawX(), y = drawY();
            boolean right = rightAligned();
            boolean bottom = y + height / 2 > mc.getWindow().getGuiScaledHeight() / 2;
            for (int i = 0; i < lines.size(); i++) {
                String l = lines.get(i);
                int w = font.width(l);
                int lx = right ? x + width - w : x;
                int ly = bottom ? y + height - (i + 1) * 10 : y + i * 10;
                if (settings().background.isOn()) g.fill(lx - 2, ly - 1, lx + w + 2, ly + 9, 0x70000000);
                int c = color(i);
                g.fill(right ? x + width + 2 : x - 3, ly - 1, right ? x + width + 3 : x - 2, ly + 9, c);
                text(g, l, lx, ly, c);
            }
        }
    }

    static class Armor extends HudElement {
        private static final EquipmentSlot[] SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

        Armor() { super("Armor", 10000, 10000, true); }

        @Override
        public void render(GuiGraphics g, float pt, boolean editing) {
            width = 4 * 18;
            height = 24;
            int x = drawX(), y = drawY();
            for (int i = 0; i < 4; i++) {
                ItemStack s = mc.player.getItemBySlot(SLOTS[i]);
                int ix = x + i * 18;
                if (s.isEmpty()) {
                    if (editing) g.fill(ix, y, ix + 16, y + 16, 0x40FFFFFF);
                    continue;
                }
                g.renderItem(s, ix, y);
                if (s.isDamageableItem()) {
                    float f = InvUtil.durabilityFraction(s);
                    String p = Math.round(f * 100) + "%";
                    g.pose().pushMatrix();
                    g.pose().translate(ix + 8f, y + 17f);
                    g.pose().scale(0.6f, 0.6f);
                    g.drawString(mc.font, p, -mc.font.width(p) / 2, 0, DrevRender.healthColor(f), true);
                    g.pose().popMatrix();
                }
            }
        }
    }

    static class Compass extends HudElement {
        private static final String[] MARKS = { "S", "SW", "W", "NW", "N", "NE", "E", "SE" };

        Compass() { super("Compass", 10000, 2, false); }

        @Override
        public void render(GuiGraphics g, float pt, boolean editing) {
            width = 160;
            height = 12;
            int x = drawX(), y = drawY();
            g.fill(x, y, x + width, y + height, 0x70000000);
            float yaw = Mth.wrapDegrees(mc.player.getYRot());
            int center = x + width / 2;
            g.enableScissor(x, y, x + width, y + height);
            for (int i = -2; i < MARKS.length + 2; i++) {
                int idx = Math.floorMod(i, MARKS.length);
                float angle = i * 45f;
                float diff = Mth.wrapDegrees(angle - yaw);
                int px = center + (int) (diff * 1.2f);
                String m = MARKS[idx];
                int c = m.length() == 1 ? color(0) : 0xFFAAAAAA;
                g.drawString(mc.font, m, px - mc.font.width(m) / 2, y + 2, c, false);
            }
            g.disableScissor();
            g.fill(center, y, center + 1, y + 3, 0xFFFFFFFF);
        }
    }

    static class Potions extends HudElement {
        Potions() { super("Potions", 10000, 300, true); }

        @Override
        public void render(GuiGraphics g, float pt, boolean editing) {
            List<String> lines = new ArrayList<>();
            for (MobEffectInstance e : mc.player.getActiveEffects()) {
                String name = e.getEffect().value().getDisplayName().getString();
                int amp = e.getAmplifier() + 1;
                int secs = e.getDuration() / 20;
                String time = e.isInfiniteDuration() ? "**:**" : String.format("%d:%02d", secs / 60, secs % 60);
                lines.add(name + (amp > 1 ? " " + amp : "") + " §7" + time);
            }
            if (lines.isEmpty() && editing) lines.add("Potion effects");
            width = 50;
            for (String l : lines) width = Math.max(width, mc.font.width(l));
            height = Math.max(10, lines.size() * 10);
            int x = drawX(), y = drawY();
            boolean right = rightAligned();
            for (int i = 0; i < lines.size(); i++) {
                String l = lines.get(i);
                int w = mc.font.width(l);
                int lx = right ? x + width - w : x;
                background(g, lx, y + i * 10, w, 9);
                text(g, l, lx, y + i * 10, color(i + 3));
            }
        }
    }
}

package dev.drevss.drevclient.module.render;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.DrevRender;
import dev.drevss.drevclient.util.EntityUtil;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Nametags extends Module {
    private final NumberSetting scale = add(new NumberSetting("Scale", "Tag size", 1.0, 0.5, 2.5, 0.1));
    private final BoolSetting health = add(new BoolSetting("Health", "Show health", true));
    private final BoolSetting distance = add(new BoolSetting("Distance", "Show distance", true));
    private final BoolSetting ping = add(new BoolSetting("Ping", "Show ping", true));
    private final BoolSetting items = add(new BoolSetting("Items", "Show held items and armor", true));
    private final BoolSetting durability = add(new BoolSetting("Durability", "Show armor durability %", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Tags for mobs too", false));
    private final NumberSetting range = add(new NumberSetting("Range", "Max distance", 1000, 16, 1000, 8));

    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    public Nametags() {
        super("Nametags", "Bigger nametags with health, ping and gear", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphics g, float pt) {
        Font font = mc.font;
        double max = range.getValue();
        List<Entity> targets = new ArrayList<>();
        EntityUtil.all().forEach(e -> {
            if (e == mc.player || !(e instanceof LivingEntity) || !e.isAlive()) return;
            if (!(e instanceof Player) && !mobs.isOn()) return;
            if (e.distanceTo(mc.player) > max) return;
            targets.add(e);
        });
        // draw far ones first so near tags end up on top
        targets.sort(Comparator.comparingDouble(e -> -e.distanceToSqr(mc.player)));

        for (Entity e : targets) {
            LivingEntity le = (LivingEntity) e;
            Vec3 head = EntityUtil.lerpPos(e, pt).add(0, e.getBbHeight() + 0.45, 0);
            Vec3 screen = DrevRender.toScreen(head);
            if (screen == null) continue;

            StringBuilder text = new StringBuilder();
            if (EntityUtil.isFriend(e)) text.append("§b");
            else if (e.isInvisible()) text.append("§7");
            else text.append("§f");
            text.append(e.getName().getString());
            if (ping.isOn() && e instanceof Player p && mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(p.getUUID());
                if (info != null) text.append(" §7").append(info.getLatency()).append("ms");
            }
            if (health.isOn()) {
                float hp = EntityUtil.totalHealth(le);
                float frac = hp / le.getMaxHealth();
                String c = frac > 0.66f ? "§a" : frac > 0.33f ? "§e" : "§c";
                text.append(" ").append(c).append(String.format("%.1f", hp));
            }
            if (distance.isOn()) text.append(" §7").append(String.format("%.0fm", e.distanceTo(mc.player)));

            String s = text.toString();
            float sc = scale.getFloat();
            int w = font.width(s);

            g.pose().pushMatrix();
            g.pose().translate((float) screen.x, (float) screen.y);
            g.pose().scale(sc, sc);
            g.fill(-w / 2 - 2, -10, w / 2 + 2, 1, 0x90000000);
            g.drawString(font, s, -w / 2, -9, 0xFFFFFFFF, true);

            if (items.isOn()) {
                List<ItemStack> gear = new ArrayList<>();
                gear.add(le.getMainHandItem());
                for (EquipmentSlot slot : ARMOR) gear.add(le.getItemBySlot(slot));
                gear.add(le.getOffhandItem());
                gear.removeIf(ItemStack::isEmpty);
                int x = -gear.size() * 9;
                for (ItemStack stack : gear) {
                    g.renderItem(stack, x, -28);
                    if (durability.isOn() && stack.isDamageableItem()) {
                        int pct = Math.round(InvUtil.durabilityFraction(stack) * 100);
                        String d = pct + "%";
                        g.pose().pushMatrix();
                        g.pose().translate(x + 8f, -34f);
                        g.pose().scale(0.5f, 0.5f);
                        g.drawString(font, d, -font.width(d) / 2, 0, DrevRender.healthColor(pct / 100f), true);
                        g.pose().popMatrix();
                    } else if (stack.getCount() > 1) {
                        g.drawString(font, String.valueOf(stack.getCount()), x + 10, -20, 0xFFFFFFFF, true);
                    }
                    x += 18;
                }
            }
            g.pose().popMatrix();
        }
    }
}

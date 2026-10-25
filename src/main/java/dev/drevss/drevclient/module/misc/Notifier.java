package dev.drevss.drevclient.module.misc;

import dev.drevss.drevclient.event.PacketEvent;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.util.ChatUtil;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Notifier extends Module {
    private final BoolSetting visualRange = add(new BoolSetting("VisualRange", "Players entering/leaving render distance", true));
    private final BoolSetting joinLeave = add(new BoolSetting("JoinLeave", "Players joining/leaving the server", false));
    private final BoolSetting totemPops = add(new BoolSetting("TotemPops", "Count totem pops", true));
    private final BoolSetting armor = add(new BoolSetting("ArmorWarning", "Warn when your armor is almost broken", true));
    private final BoolSetting lowHealth = add(new BoolSetting("LowHealth", "Warn when your health is low", true));

    private final Set<String> inRange = new HashSet<>();
    private final Set<String> online = new HashSet<>();
    private final Map<String, Integer> pops = new HashMap<>();
    private final Set<EquipmentSlot> warned = new HashSet<>();
    private boolean healthWarned;
    private boolean firstTick = true;

    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    public Notifier() {
        super("Notifier", "Chat alerts for players, totem pops and danger", Category.MISC);
    }

    @Override
    public void onEnable() {
        inRange.clear();
        online.clear();
        pops.clear();
        firstTick = true;
    }

    @Override
    public void onTick() {
        if (visualRange.isOn()) {
            Set<String> now = new HashSet<>();
            for (Player p : mc.level.players()) if (p != mc.player) now.add(p.getName().getString());
            if (!firstTick) {
                for (String n : now) if (!inRange.contains(n)) ChatUtil.info("§e" + n + "§7 entered visual range");
                for (String n : inRange) if (!now.contains(n)) ChatUtil.info("§e" + n + "§7 left visual range");
            }
            inRange.clear();
            inRange.addAll(now);
        }

        if (joinLeave.isOn() && mc.getConnection() != null) {
            Set<String> now = new HashSet<>();
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) now.add(info.getProfile().name());
            if (!firstTick) {
                for (String n : now) if (!online.contains(n)) ChatUtil.info("§a+ §f" + n);
                for (String n : online) if (!now.contains(n)) ChatUtil.info("§c- §f" + n);
            }
            online.clear();
            online.addAll(now);
        }

        if (armor.isOn()) {
            for (EquipmentSlot slot : ARMOR) {
                ItemStack s = mc.player.getItemBySlot(slot);
                boolean low = s.isDamageableItem() && InvUtil.durabilityFraction(s) < 0.1f;
                if (low && warned.add(slot)) ChatUtil.error("Your " + s.getHoverName().getString() + " is almost broken!");
                if (!low) warned.remove(slot);
            }
        }

        if (lowHealth.isOn()) {
            float hp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (hp <= 6 && !healthWarned) { ChatUtil.error("Low health: " + String.format("%.1f", hp)); healthWarned = true; }
            if (hp > 10) healthWarned = false;
        }
        firstTick = false;
    }

    @Override
    public void onPacketReceive(PacketEvent event) {
        if (!totemPops.isOn() || !(event.getPacket() instanceof ClientboundEntityEventPacket packet)) return;
        if (packet.getEventId() != 35) return;
        mc.execute(() -> {
            if (mc.level == null) return;
            Entity e = packet.getEntity(mc.level);
            if (!(e instanceof Player p)) return;
            String name = p.getName().getString();
            int count = pops.merge(name, 1, Integer::sum);
            ChatUtil.info("§e" + name + "§7 popped §c" + count + "§7 totem" + (count == 1 ? "" : "s"));
        });
    }
}

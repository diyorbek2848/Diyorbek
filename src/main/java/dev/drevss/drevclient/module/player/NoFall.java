package dev.drevss.drevclient.module.player;

import dev.drevss.drevclient.event.PacketEvent;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.util.MovePackets;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.Items;

/**
 * Servers take fall damage from the on-ground flag the client sends. While
 * falling, every movement packet is sent as "on ground", so the server keeps
 * resetting the fall distance and never applies damage.
 */
public class NoFall extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Packet: spoof every falling packet. Smart: only when the fall would hurt",
        "Packet", "Packet", "Smart"));
    private final BoolSetting pauseForMace = add(new BoolSetting("PauseForMace", "Allow mace smash attacks while holding a mace", true));

    public NoFall() {
        super("NoFall", "Prevents fall damage", Category.PLAYER);
    }

    private boolean active() {
        var p = mc.player;
        if (p == null || p.getAbilities().invulnerable || p.isFallFlying() || p.isInWater() || p.isInLava()) return false;
        if (pauseForMace.isOn() && p.getMainHandItem().is(Items.MACE)) return false;
        if (p.onGround()) return false;
        if (mode.is("Smart")) return p.fallDistance > 2.5;
        return p.getDeltaMovement().y < -0.08 || p.fallDistance > 0.5;
    }

    @Override
    public void onPacketSend(PacketEvent event) {
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket packet) || packet.isOnGround()) return;
        if (!active()) return;
        event.setPacket(MovePackets.withOnGround(packet, true));
    }

    @Override
    public String getInfo() { return mode.get(); }
}

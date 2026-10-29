package dev.drevss.drevclient.module.combat;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ModeSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A hit is critical when the server thinks you're falling and not sprinting.
 * Packet mode sends a tiny fake hop right before the attack packet.
 */
public class Criticals extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Packet fakes a tiny hop, Jump really hops", "Packet", "Packet", "Jump"));
    private final BoolSetting stopSprint = add(new BoolSetting("StopSprint", "Sprinting cancels crits; stop it for the hit", true));

    public Criticals() {
        super("Criticals", "Turns your hits into critical hits", Category.COMBAT);
    }

    @Override
    public void onAttack(Entity target) {
        if (!(target instanceof LivingEntity)) return;
        var p = mc.player;
        if (!p.onGround() || p.isInWater() || p.isInLava() || p.onClimbable() || p.isPassenger()) return;

        if (stopSprint.isOn() && p.isSprinting()) {
            p.setSprinting(false);
            p.connection.send(new ServerboundPlayerCommandPacket(p, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        }

        if (mode.is("Packet")) {
            sendY(0.0625, true);
            sendY(0, false);
            sendY(1.1e-5, false);
            sendY(0, false);
        } else {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, 0.25, v.z);
        }
    }

    private void sendY(double offset, boolean onGround) {
        var p = mc.player;
        p.connection.send(new ServerboundMovePlayerPacket.Pos(p.getX(), p.getY() + offset, p.getZ(), onGround, p.horizontalCollision));
    }

    @Override
    public String getInfo() { return mode.get(); }
}

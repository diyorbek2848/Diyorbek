package dev.drevss.drevclient.module.combat;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.ModeSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class Criticals extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Packet fakes a tiny hop, Jump really hops", "Packet", "Packet", "Jump"));

    public Criticals() {
        super("Criticals", "Turns your hits into critical hits", Category.COMBAT);
    }

    @Override
    public void onAttack(Entity target) {
        if (!(target instanceof LivingEntity)) return;
        if (!mc.player.onGround() || mc.player.isInWater() || mc.player.isInLava() || mc.player.onClimbable()) return;

        if (mode.is("Packet")) {
            sendY(0.0625, true);
            sendY(0, false);
            sendY(1.1e-5, false);
            sendY(0, false);
        } else {
            Vec3 v = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(v.x, 0.25, v.z);
        }
    }

    private void sendY(double offset, boolean onGround) {
        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(
            mc.player.getX(), mc.player.getY() + offset, mc.player.getZ(), onGround, mc.player.horizontalCollision));
    }

    @Override
    public String getInfo() { return mode.get(); }
}

package dev.drevss.drevclient.module.movement;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.PlayerUtil;
import net.minecraft.world.phys.Vec3;

public class Speed extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Strafe bunny-hops, Ground stays on the floor", "Strafe", "Strafe", "Ground"));
    private final NumberSetting speed = add(new NumberSetting("Speed", "Blocks per tick", 0.45, 0.1, 2, 0.01));

    public Speed() {
        super("Speed", "Makes you move faster", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (!PlayerUtil.isMoving() || mc.player.isShiftKeyDown() || mc.player.isInWater() || mc.player.isInLava()
            || mc.player.isFallFlying() || mc.player.onClimbable() || mc.player.isPassenger()) return;

        Vec3 v = PlayerUtil.inputVelocity(speed.getValue());
        if (mode.is("Strafe")) {
            double y = mc.player.getDeltaMovement().y;
            if (mc.player.onGround()) y = 0.42;
            mc.player.setDeltaMovement(v.x, y, v.z);
        } else if (mc.player.onGround()) {
            mc.player.setDeltaMovement(v.x, mc.player.getDeltaMovement().y, v.z);
        }
        mc.player.setSprinting(true);
    }

    @Override
    public String getInfo() { return mode.get(); }
}

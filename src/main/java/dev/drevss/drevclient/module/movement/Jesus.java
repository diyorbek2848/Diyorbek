package dev.drevss.drevclient.module.movement;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import net.minecraft.world.phys.Vec3;

public class Jesus extends Module {
    private final BoolSetting lava = add(new BoolSetting("Lava", "Walk on lava as well", true));

    public Jesus() {
        super("Jesus", "Walk on water and lava", Category.MOVEMENT);
    }

    /** Used by the block collision mixin. */
    public boolean shouldBeSolid() {
        if (!isEnabled() || mc.player == null) return false;
        if (mc.options.keyShift.isDown() || mc.player.fallDistance > 3) return false;
        if (mc.player.isInLava()) return false;
        return !mc.player.isInWater();
    }

    @Override
    public void onTick() {
        if (mc.options.keyShift.isDown()) return;
        boolean inLiquid = mc.player.isInWater() || (lava.isOn() && mc.player.isInLava());
        if (inLiquid) {
            Vec3 v = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(v.x, 0.11, v.z);
        }
    }
}

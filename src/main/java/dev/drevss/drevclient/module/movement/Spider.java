package dev.drevss.drevclient.module.movement;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.NumberSetting;
import net.minecraft.world.phys.Vec3;

public class Spider extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", "Climb speed", 0.2, 0.05, 1, 0.05));

    public Spider() {
        super("Spider", "Climb walls like a spider", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (!mc.player.horizontalCollision) return;
        Vec3 v = mc.player.getDeltaMovement();
        if (mc.options.keyShift.isDown()) {
            mc.player.setDeltaMovement(v.x, 0, v.z);
        } else {
            mc.player.setDeltaMovement(v.x, speed.getValue(), v.z);
        }
        mc.player.fallDistance = 0;
    }
}

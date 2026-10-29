package dev.drevss.drevclient.module.movement;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.util.PlayerUtil;

public class Sprint extends Module {
    private final BoolSetting omni = add(new BoolSetting("AllDirections", "Sprint sideways and backwards too", false));

    public boolean allDirections() { return omni.isOn(); }

    public Sprint() {
        super("Sprint", "Sprints automatically", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player.getFoodData().getFoodLevel() <= 6 && !mc.player.getAbilities().mayfly) return;
        if (mc.player.isShiftKeyDown() || mc.player.horizontalCollision) return;
        boolean moving = omni.isOn() ? PlayerUtil.isMoving() : mc.options.keyUp.isDown();
        if (moving) mc.player.setSprinting(true);
    }
}

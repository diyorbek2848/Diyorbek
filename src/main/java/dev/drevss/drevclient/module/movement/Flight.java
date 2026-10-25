package dev.drevss.drevclient.module.movement;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.PlayerUtil;
import net.minecraft.world.phys.Vec3;

public class Flight extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Motion sets velocity, Vanilla uses creative flight", "Motion", "Motion", "Vanilla"));
    private final NumberSetting speed = add(new NumberSetting("Speed", "Horizontal speed", 1.0, 0.1, 10, 0.1));
    private final NumberSetting vSpeed = add(new NumberSetting("VerticalSpeed", "Up/down speed", 0.6, 0.1, 5, 0.1));
    private final BoolSetting antiKick = add(new BoolSetting("AntiKick", "Drop slightly every few seconds", true));

    private int ticks;

    public Flight() {
        super("Flight", "Lets you fly", Category.MOVEMENT);
    }

    @Override
    public void onDisable() {
        if (!inGame()) return;
        if (mode.is("Vanilla") && !mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().mayfly = false;
        }
        mc.player.getAbilities().setFlyingSpeed(0.05f);
    }

    @Override
    public void onTick() {
        ticks++;
        if (mode.is("Vanilla")) {
            mc.player.getAbilities().mayfly = true;
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlyingSpeed((float) (speed.getValue() * 0.05));
            return;
        }

        mc.player.getAbilities().flying = false;
        double y = 0;
        if (mc.options.keyJump.isDown()) y += vSpeed.getValue();
        if (mc.options.keyShift.isDown()) y -= vSpeed.getValue();
        if (antiKick.isOn() && ticks % 40 == 0 && y == 0) y = -0.04;

        Vec3 horizontal = PlayerUtil.inputVelocity(speed.getValue());
        mc.player.setDeltaMovement(horizontal.x, y, horizontal.z);
        mc.player.fallDistance = 0;
    }

    @Override
    public String getInfo() { return mode.get(); }
}

package dev.drevss.drevclient.module.movement;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class ElytraFly extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Control: full control, Boost: speeds up gliding", "Control", "Control", "Boost"));
    private final NumberSetting speed = add(new NumberSetting("Speed", "Flight speed", 1.8, 0.1, 5, 0.1));
    private final NumberSetting vSpeed = add(new NumberSetting("VerticalSpeed", "Up/down speed in Control mode", 1.0, 0.1, 3, 0.1));
    private final BoolSetting autoStart = add(new BoolSetting("AutoStart", "Open the elytra when you jump while falling", true));

    private int startCooldown;

    public ElytraFly() {
        super("ElytraFly", "Better elytra flight", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (startCooldown > 0) startCooldown--;
        boolean wearing = mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        if (!wearing) return;

        if (!mc.player.isFallFlying()) {
            if (autoStart.isOn() && startCooldown == 0 && !mc.player.onGround() && mc.player.getDeltaMovement().y < 0
                && mc.options.keyJump.isDown()) {
                mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                startCooldown = 10;
            }
            return;
        }

        Vec3 look = mc.player.getLookAngle();
        if (mode.is("Boost")) {
            if (mc.options.keyUp.isDown()) {
                Vec3 v = mc.player.getDeltaMovement();
                double boost = 0.05 * speed.getValue();
                mc.player.setDeltaMovement(v.add(look.x * boost, look.y * boost * 0.5, look.z * boost));
            }
            return;
        }

        double y = 0;
        if (mc.options.keyJump.isDown()) y = vSpeed.getValue();
        else if (mc.options.keyShift.isDown()) y = -vSpeed.getValue();

        if (mc.options.keyUp.isDown()) {
            Vec3 flat = new Vec3(look.x, 0, look.z).normalize().scale(speed.getValue());
            mc.player.setDeltaMovement(flat.x, y, flat.z);
        } else {
            mc.player.setDeltaMovement(0, y, 0);
        }
    }

    @Override
    public String getInfo() { return mode.get(); }
}

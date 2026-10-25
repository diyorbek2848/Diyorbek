package dev.drevss.drevclient.module.combat;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.NumberSetting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Velocity extends Module {
    private final NumberSetting horizontal = add(new NumberSetting("Horizontal", "Percent of horizontal knockback kept", 0, 0, 100, 5));
    private final NumberSetting vertical = add(new NumberSetting("Vertical", "Percent of vertical knockback kept", 0, 0, 100, 5));

    public Velocity() {
        super("Velocity", "Reduces knockback from hits and explosions", Category.COMBAT);
    }

    /** Scales the change between our current motion and the server's new motion. */
    public Vec3 modify(Entity self, Vec3 incoming) {
        if (!isEnabled()) return incoming;
        Vec3 current = self.getDeltaMovement();
        double h = horizontal.getValue() / 100.0;
        double v = vertical.getValue() / 100.0;
        return new Vec3(
            current.x + (incoming.x - current.x) * h,
            current.y + (incoming.y - current.y) * v,
            current.z + (incoming.z - current.z) * h);
    }

    public Vec3 scaleExplosion(Vec3 push) {
        return new Vec3(push.x * horizontal.getValue() / 100.0, push.y * vertical.getValue() / 100.0, push.z * horizontal.getValue() / 100.0);
    }

    @Override
    public String getInfo() { return "H" + horizontal.getDisplayValue() + "% V" + vertical.getDisplayValue() + "%"; }
}

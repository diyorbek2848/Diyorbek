package dev.drevss.drevclient.module.combat;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.EntityUtil;
import dev.drevss.drevclient.util.PlayerUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public class BowAimbot extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Max target distance", 60, 5, 120, 1));
    private final NumberSetting fov = add(new NumberSetting("FOV", "Only aim at targets within this angle", 90, 10, 360, 5));
    private final BoolSetting players = add(new BoolSetting("Players", "Aim at players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Aim at hostile mobs", true));
    private final BoolSetting animals = add(new BoolSetting("Animals", "Aim at animals", false));
    private final BoolSetting predict = add(new BoolSetting("Predict", "Lead moving targets", true));

    public BowAimbot() {
        super("BowAimbot", "Aims your bow, crossbow or trident for you", Category.COMBAT);
    }

    @Override
    public void onTick() {
        ItemStack using = mc.player.getUseItem();
        boolean bow = using.is(Items.BOW);
        boolean trident = using.is(Items.TRIDENT);
        boolean crossbow = mc.player.getMainHandItem().is(Items.CROSSBOW);
        if (!(mc.player.isUsingItem() && (bow || trident)) && !crossbow) return;

        double r = range.getValue();
        Entity target = EntityUtil.all()
            .filter(e -> EntityUtil.isValidTarget(e, players.isOn(), mobs.isOn(), animals.isOn(), false))
            .filter(e -> e.distanceToSqr(mc.player) <= r * r)
            .filter(e -> mc.player.hasLineOfSight(e))
            .filter(e -> PlayerUtil.angleTo(e.getBoundingBox().getCenter()) <= fov.getValue() / 2)
            .min(Comparator.comparingDouble(e -> PlayerUtil.angleTo(e.getBoundingBox().getCenter())))
            .orElse(null);
        if (target == null) return;

        double velocity;
        if (bow) {
            float charge = mc.player.getTicksUsingItem() / 20f;
            charge = (charge * charge + charge * 2f) / 3f;
            velocity = Math.min(charge, 1f) * 3.0;
        } else if (trident) {
            velocity = 2.5;
        } else {
            velocity = 3.15;
        }
        if (velocity < 0.1) return;

        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0);
        if (predict.isOn()) {
            double flightTicks = mc.player.getEyePosition().distanceTo(aim) / velocity;
            Vec3 motion = target.position().subtract(target.xOld, target.yOld, target.zOld);
            aim = aim.add(motion.scale(flightTicks));
        }

        Vec3 eyes = mc.player.getEyePosition();
        double dx = aim.x - eyes.x, dz = aim.z - eyes.z, dy = aim.y - eyes.y;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double g = 0.05;
        double v2 = velocity * velocity;
        double root = v2 * v2 - g * (g * horizontal * horizontal + 2 * dy * v2);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = root < 0
            ? -45f
            : (float) -Math.toDegrees(Math.atan((v2 - Math.sqrt(root)) / (g * horizontal)));
        PlayerUtil.face(yaw, pitch);
    }
}

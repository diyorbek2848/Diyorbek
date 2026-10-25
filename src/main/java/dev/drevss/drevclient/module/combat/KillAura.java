package dev.drevss.drevclient.module.combat;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.EntityUtil;
import dev.drevss.drevclient.util.PlayerUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;

public class KillAura extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Attack reach in blocks", 4.2, 1, 6, 0.1));
    private final ModeSetting priority = add(new ModeSetting("Priority", "Which target to pick first", "Distance", "Distance", "Health", "Angle"));
    private final ModeSetting rotation = add(new ModeSetting("Rotate", "How to face the target", "Silent", "Silent", "Camera", "None"));
    private final BoolSetting cooldown = add(new BoolSetting("Cooldown", "Wait for full attack charge", true));
    private final NumberSetting cps = add(new NumberSetting("CPS", "Clicks per second when cooldown is off", 10, 1, 20, 1)
        .visibleWhen(() -> !cooldownOn()));
    private final BoolSetting players = add(new BoolSetting("Players", "Attack players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Attack hostile mobs", true));
    private final BoolSetting animals = add(new BoolSetting("Animals", "Attack animals", false));
    private final BoolSetting invisibles = add(new BoolSetting("Invisibles", "Attack invisible entities", false));
    private final BoolSetting walls = add(new BoolSetting("ThroughWalls", "Attack without line of sight", false));
    private final BoolSetting onlyWeapon = add(new BoolSetting("OnlyWeapon", "Only attack while holding a sword/axe", false));

    private Entity target;
    private int ticksSinceAttack;

    public KillAura() {
        super("KillAura", "Automatically attacks entities around you", Category.COMBAT);
    }

    private boolean cooldownOn() { return cooldown == null || cooldown.isOn(); }

    public Entity getTarget() { return isEnabled() ? target : null; }

    @Override
    public void onDisable() { target = null; }

    @Override
    public void onTick() {
        ticksSinceAttack++;
        if (mc.screen != null || mc.player.isUsingItem() || mc.player.isSpectator()) return;
        if (onlyWeapon.isOn()) {
            String id = dev.drevss.drevclient.util.InvUtil.id(mc.player.getMainHandItem());
            if (!id.endsWith("_sword") && !id.endsWith("_axe") && !id.equals("mace") && !id.equals("trident")) return;
        }

        double r = range.getValue();
        Comparator<Entity> order = switch (priority.get()) {
            case "Health" -> Comparator.comparingDouble(e -> ((LivingEntity) e).getHealth());
            case "Angle" -> Comparator.comparingDouble(e -> PlayerUtil.angleTo(e.getBoundingBox().getCenter()));
            default -> Comparator.comparingDouble(EntityUtil::hitboxDistanceSq);
        };
        target = EntityUtil.all()
            .filter(e -> EntityUtil.isValidTarget(e, players.isOn(), mobs.isOn(), animals.isOn(), invisibles.isOn()))
            .filter(e -> EntityUtil.hitboxDistanceSq(e) <= r * r)
            .filter(e -> walls.isOn() || mc.player.hasLineOfSight(e))
            .min(order).orElse(null);
        if (target == null) return;

        if (cooldown.isOn()) {
            if (mc.player.getAttackStrengthScale(0f) < 1f) return;
        } else if (ticksSinceAttack < 20.0 / cps.getValue()) {
            return;
        }

        if (!rotation.is("None")) PlayerUtil.lookAt(target.getBoundingBox().getCenter(), rotation.is("Silent"));
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(InteractionHand.MAIN_HAND);
        ticksSinceAttack = 0;
    }

    @Override
    public String getInfo() { return priority.get(); }
}

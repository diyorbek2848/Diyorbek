package dev.drevss.drevclient.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.ColorSetting;
import dev.drevss.drevclient.util.DrevRender;
import dev.drevss.drevclient.util.EntityUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class Trajectories extends Module {
    private final ColorSetting color = add(new ColorSetting("Color", "Path color", 0xFFB06BFF));

    public Trajectories() {
        super("Trajectories", "Shows where arrows, pearls and potions will land", Category.RENDER);
    }

    @Override
    public void onRender3D(PoseStack matrices, float pt) {
        ItemStack stack = mc.player.getMainHandItem();
        double speed, gravity, drag = 0.99;
        if (stack.is(Items.BOW)) {
            if (!mc.player.isUsingItem()) return;
            float charge = mc.player.getTicksUsingItem() / 20f;
            charge = Math.min(1f, (charge * charge + charge * 2f) / 3f);
            if (charge < 0.1f) return;
            speed = charge * 3.0;
            gravity = 0.05;
        } else if (stack.is(Items.CROSSBOW)) {
            speed = 3.15;
            gravity = 0.05;
        } else if (stack.is(Items.TRIDENT)) {
            if (!mc.player.isUsingItem()) return;
            speed = 2.5;
            gravity = 0.05;
        } else if (stack.is(Items.ENDER_PEARL) || stack.is(Items.SNOWBALL) || stack.is(Items.EGG)) {
            speed = 1.5;
            gravity = 0.03;
        } else if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            speed = 0.5;
            gravity = 0.05;
        } else if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            speed = 0.7;
            gravity = 0.07;
        } else {
            return;
        }

        Vec3 pos = EntityUtil.lerpPos(mc.player, pt).add(0, mc.player.getEyeHeight() - 0.1, 0);
        Vec3 vel = Vec3.directionFromRotation(mc.player.getXRot(), mc.player.getYRot()).scale(speed);

        List<Vec3> path = new ArrayList<>();
        path.add(pos);
        Vec3 hitPos = null;
        for (int i = 0; i < 300; i++) {
            Vec3 next = pos.add(vel);
            BlockHitResult hit = mc.level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (hit.getType() != HitResult.Type.MISS) {
                path.add(hit.getLocation());
                hitPos = hit.getLocation();
                break;
            }
            Entity hitEntity = entityOnSegment(pos, next);
            if (hitEntity != null) {
                path.add(next);
                hitPos = next;
                break;
            }
            pos = next;
            path.add(pos);
            vel = vel.scale(drag).subtract(0, gravity, 0);
            if (pos.y < mc.level.getMinY() - 16) break;
        }

        int c = color.getColor();
        DrevRender.drawPath(matrices, path, c, false);
        if (hitPos != null) {
            DrevRender.drawBox(matrices, new AABB(hitPos.subtract(0.25, 0.25, 0.25), hitPos.add(0.25, 0.25, 0.25)),
                DrevRender.withAlpha(c, 60), c);
        }
    }

    private Entity entityOnSegment(Vec3 a, Vec3 b) {
        AABB area = new AABB(a, b).inflate(1);
        for (Entity e : mc.level.getEntities(mc.player, area, ent -> ent.isPickable() && ent.isAlive())) {
            if (e.getBoundingBox().inflate(0.3).clip(a, b).isPresent()) return e;
        }
        return null;
    }
}

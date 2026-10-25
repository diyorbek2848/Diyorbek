package dev.drevss.drevclient.util;

import dev.drevss.drevclient.DrevClient;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class EntityUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    private EntityUtil() { }

    public static Stream<Entity> all() {
        return StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false);
    }

    public static Vec3 lerpPos(Entity e, float pt) {
        if (e.isRemoved()) return e.position();
        return new Vec3(Mth.lerp(pt, e.xOld, e.getX()), Mth.lerp(pt, e.yOld, e.getY()), Mth.lerp(pt, e.zOld, e.getZ()));
    }

    public static AABB lerpBox(Entity e, float pt) {
        Vec3 offset = lerpPos(e, pt).subtract(e.position());
        return e.getBoundingBox().move(offset);
    }

    /** Distance from the eyes to the closest point of the hitbox, squared. */
    public static double hitboxDistanceSq(Entity e) {
        Vec3 eyes = mc.player.getEyePosition();
        AABB box = e.getBoundingBox();
        double x = Mth.clamp(eyes.x, box.minX, box.maxX);
        double y = Mth.clamp(eyes.y, box.minY, box.maxY);
        double z = Mth.clamp(eyes.z, box.minZ, box.maxZ);
        return eyes.distanceToSqr(x, y, z);
    }

    public static boolean isFriend(Entity e) {
        return e instanceof Player p && DrevClient.get().friends().isFriend(p.getGameProfile().name());
    }

    public static boolean isHostile(Entity e) { return e instanceof Enemy; }
    public static boolean isPassive(Entity e) { return e instanceof Animal; }

    /** Common target filter used by combat modules. */
    public static boolean isValidTarget(Entity e, boolean players, boolean mobs, boolean animals, boolean invisibles) {
        if (e == mc.player || !(e instanceof LivingEntity living) || !living.isAlive() || living.getHealth() <= 0) return false;
        if (!invisibles && e.isInvisible()) return false;
        if (e instanceof Player p) {
            if (!players || p.isSpectator()) return false;
            return !isFriend(p);
        }
        if (isHostile(e)) return mobs;
        if (isPassive(e)) return animals;
        return mobs; // golems, bats, etc. count as "mobs"
    }

    public static float totalHealth(LivingEntity e) {
        return e.getHealth() + e.getAbsorptionAmount();
    }
}

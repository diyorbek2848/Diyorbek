package dev.drevss.drevclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class PlayerUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    private PlayerUtil() { }

    /** True when any WASD key is held. */
    public static boolean isMoving() {
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
            || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
    }

    /**
     * Horizontal velocity for the current WASD input at the given speed
     * (blocks per tick), relative to the player's yaw.
     */
    public static Vec3 inputVelocity(double speed) {
        LocalPlayer p = mc.player;
        double forward = 0, strafe = 0;
        if (mc.options.keyUp.isDown()) forward++;
        if (mc.options.keyDown.isDown()) forward--;
        if (mc.options.keyLeft.isDown()) strafe++;
        if (mc.options.keyRight.isDown()) strafe--;
        if (forward == 0 && strafe == 0) return Vec3.ZERO;

        double len = Math.sqrt(forward * forward + strafe * strafe);
        forward /= len;
        strafe /= len;
        double yaw = Math.toRadians(p.getYRot());
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        double x = (strafe * cos - forward * sin) * speed;
        double z = (forward * cos + strafe * sin) * speed;
        return new Vec3(x, 0, z);
    }

    public static double horizontalSpeed() {
        Vec3 v = mc.player.getDeltaMovement();
        return Math.sqrt(v.x * v.x + v.z * v.z);
    }

    /** yaw/pitch needed to look from the eyes at a point. */
    public static float[] rotationsTo(Vec3 target) {
        Vec3 eyes = mc.player.getEyePosition();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new float[] { Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90f, 90f) };
    }

    /** Angle between the crosshair and a point, in degrees. */
    public static double angleTo(Vec3 target) {
        float[] rot = rotationsTo(target);
        double dYaw = Math.abs(Mth.wrapDegrees(rot[0] - mc.player.getYRot()));
        double dPitch = Math.abs(rot[1] - mc.player.getXRot());
        return Math.sqrt(dYaw * dYaw + dPitch * dPitch);
    }

    /** Server-side rotation only: the camera does not move. */
    public static void sendRotation(float yaw, float pitch) {
        LocalPlayer p = mc.player;
        p.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, p.onGround(), p.horizontalCollision));
    }

    /** Rotates the actual camera. */
    public static void face(float yaw, float pitch) {
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }

    public static void lookAt(Vec3 target, boolean silent) {
        float[] r = rotationsTo(target);
        if (silent) sendRotation(r[0], r[1]);
        else face(r[0], r[1]);
    }
}

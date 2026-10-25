package dev.drevss.drevclient.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.DrevRender;
import dev.drevss.drevclient.util.EntityUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class Freecam extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", "Camera speed", 1.0, 0.1, 5, 0.1));
    private final BoolSetting showPlayer = add(new BoolSetting("Highlight", "Box around your real body", true));
    private final BoolSetting disableOnDamage = add(new BoolSetting("DisableOnDamage", "Leave freecam if you get hurt", true));

    private Vec3 camPos = Vec3.ZERO, prevCamPos = Vec3.ZERO;
    private float yaw, pitch, lastHealth;
    private Vec2 move = Vec2.ZERO;

    public Freecam() {
        super("Freecam", "Fly the camera around while your body stays put", Category.RENDER);
    }

    @Override
    public void onEnable() {
        if (!inGame()) { setEnabledSilently(false); return; }
        camPos = mc.player.getEyePosition();
        prevCamPos = camPos;
        yaw = mc.player.getYRot();
        pitch = mc.player.getXRot();
        lastHealth = mc.player.getHealth();
        move = Vec2.ZERO;
    }

    @Override
    public void onDisable() {
        move = Vec2.ZERO;
    }

    public void setMoveVector(Vec2 v) { move = v; }

    public void turn(double dYaw, double dPitch) {
        yaw += (float) dYaw * 0.15f;
        pitch = Mth.clamp(pitch + (float) dPitch * 0.15f, -90f, 90f);
    }

    public Vec3 getCamPos(float pt) {
        return new Vec3(Mth.lerp(pt, prevCamPos.x, camPos.x), Mth.lerp(pt, prevCamPos.y, camPos.y), Mth.lerp(pt, prevCamPos.z, camPos.z));
    }

    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    @Override
    public void onTick() {
        if (disableOnDamage.isOn() && mc.player.getHealth() < lastHealth) {
            setEnabled(false);
            return;
        }
        lastHealth = mc.player.getHealth();
        prevCamPos = camPos;
        if (mc.screen != null) return;

        double rad = Math.toRadians(yaw);
        double sin = Math.sin(rad), cos = Math.cos(rad);
        double x = move.x * cos - move.y * sin;
        double z = move.x * sin + move.y * cos;
        double y = 0;
        if (mc.options.keyJump.isDown()) y += 1;
        if (mc.options.keyShift.isDown()) y -= 1;
        camPos = camPos.add(new Vec3(x, y, z).scale(speed.getValue()));
    }

    @Override
    public void onRender3D(PoseStack matrices, float pt) {
        if (!showPlayer.isOn()) return;
        DrevRender.drawBoxes(matrices, List.of(new DrevRender.ColoredBox(
            EntityUtil.lerpBox(mc.player, pt), 0x30B06BFF, 0xFFB06BFF)), true, true);
    }
}

package dev.drevss.drevclient.mixin;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.render.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean detached;

    @Shadow protected abstract void setPosition(Vec3 pos);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;ZZF)V", at = @At("RETURN"))
    private void drev$freecam(Level level, Entity entity, boolean thirdPerson, boolean inverse, float pt, CallbackInfo ci) {
        if (DrevClient.get() == null) return;
        Freecam freecam = DrevClient.get().modules().get(Freecam.class);
        if (!freecam.isEnabled()) return;
        detached = true;
        setPosition(freecam.getCamPos(pt));
        setRotation(freecam.getYaw(), freecam.getPitch());
    }
}

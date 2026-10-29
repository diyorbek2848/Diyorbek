package dev.drevss.drevclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.render.Freecam;
import dev.drevss.drevclient.module.render.NoRender;
import dev.drevss.drevclient.module.render.Tracers;
import dev.drevss.drevclient.module.render.Zoom;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @ModifyReturnValue(method = "getFov(Lnet/minecraft/client/Camera;FZ)F", at = @At("RETURN"), require = 0)
    private float drev$zoom(float original) {
        if (DrevClient.get() == null) return original;
        return DrevClient.get().modules().get(Zoom.class).apply(original);
    }

    @Inject(method = "bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void drev$noHurtCam(PoseStack matrices, float pt, CallbackInfo ci) {
        if (DrevClient.get() == null) return;
        NoRender nr = DrevClient.get().modules().get(NoRender.class);
        if (nr.isEnabled() && nr.hurtCam.isOn()) ci.cancel();
    }

    @Inject(method = "renderItemInHand(FZLorg/joml/Matrix4f;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void drev$hideHand(float pt, boolean b, org.joml.Matrix4f m, CallbackInfo ci) {
        if (DrevClient.get() != null && DrevClient.get().modules().get(Freecam.class).isEnabled()) ci.cancel();
    }

    /** View bobbing makes tracers wobble and moves the freecam camera, so it's off for those. */
    @Inject(method = "bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void drev$noBob(PoseStack matrices, float pt, CallbackInfo ci) {
        if (DrevClient.get() == null) return;
        if (DrevClient.get().modules().get(Freecam.class).isEnabled() || DrevClient.get().modules().get(Tracers.class).isEnabled()) ci.cancel();
    }
}

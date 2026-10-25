package dev.drevss.drevclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.render.NoRender;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @Inject(method = "renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
        at = @At("HEAD"), cancellable = true, require = 0)
    private static void drev$fire(PoseStack matrices, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (DrevClient.get() == null) return;
        NoRender nr = DrevClient.get().modules().get(NoRender.class);
        if (nr.isEnabled() && nr.fire.isOn()) ci.cancel();
    }
}

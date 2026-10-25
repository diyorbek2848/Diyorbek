package dev.drevss.drevclient.mixin;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.render.NoRender;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void drev$overlay(GuiGraphics graphics, Identifier texture, float opacity, CallbackInfo ci) {
        if (texture == null || DrevClient.get() == null) return;
        NoRender nr = DrevClient.get().modules().get(NoRender.class);
        if (nr.isEnabled() && nr.pumpkin.isOn() && texture.getPath().contains("pumpkinblur")) ci.cancel();
    }
}

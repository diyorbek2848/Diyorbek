package dev.drevss.drevclient.mixin;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.render.ESP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import dev.drevss.drevclient.gui.DrevTitleScreen;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /** Replaces the vanilla main menu with the DrevClient one. */
    @Inject(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"), cancellable = true)
    private void drev$titleScreen(Screen screen, CallbackInfo ci) {
        if (screen == null || screen.getClass() != TitleScreen.class || DrevClient.get() == null) return;
        if (!DrevClient.get().modules().clientSettings().customMenu.isOn()) return;
        ((Minecraft) (Object) this).setScreen(new DrevTitleScreen());
        ci.cancel();
    }

    /** ESP "Glow" mode uses the vanilla spectral outline. */
    @Inject(method = "shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void drev$glow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (DrevClient.get() == null) return;
        ESP esp = DrevClient.get().modules().get(ESP.class);
        if (esp.isEnabled() && esp.shouldGlow(entity)) cir.setReturnValue(true);
    }
}

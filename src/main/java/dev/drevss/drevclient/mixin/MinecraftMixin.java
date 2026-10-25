package dev.drevss.drevclient.mixin;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.render.ESP;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /** ESP "Glow" mode uses the vanilla spectral outline. */
    @Inject(method = "shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void drev$glow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (DrevClient.get() == null) return;
        ESP esp = DrevClient.get().modules().get(ESP.class);
        if (esp.isEnabled() && esp.shouldGlow(entity)) cir.setReturnValue(true);
    }
}

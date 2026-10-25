package dev.drevss.drevclient.mixin;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.render.NoRender;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {
    @Inject(method = "getRainLevel(F)F", at = @At("HEAD"), cancellable = true, require = 0)
    private void drev$rain(float delta, CallbackInfoReturnable<Float> cir) {
        if ((Object) this != Minecraft.getInstance().level || DrevClient.get() == null) return;
        NoRender nr = DrevClient.get().modules().get(NoRender.class);
        if (nr.isEnabled() && nr.weather.isOn()) cir.setReturnValue(0f);
    }
}

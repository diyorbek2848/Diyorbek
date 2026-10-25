package dev.drevss.drevclient.mixin;

import dev.drevss.drevclient.DrevClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", at = @At("HEAD"))
    private void drev$onKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (DrevClient.get() != null) DrevClient.get().modules().onKey(event.key(), action);
    }
}

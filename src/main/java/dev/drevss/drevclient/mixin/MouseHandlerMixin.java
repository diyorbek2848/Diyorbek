package dev.drevss.drevclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.misc.MiddleClickFriend;
import dev.drevss.drevclient.module.render.Freecam;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V", at = @At("HEAD"))
    private void drev$onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (DrevClient.get() == null) return;
        if (action == GLFW.GLFW_PRESS && info.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            DrevClient.get().modules().get(MiddleClickFriend.class).onMiddleClick();
        }
    }

    @WrapOperation(method = "turnPlayer(D)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void drev$turn(LocalPlayer player, double dYaw, double dPitch, Operation<Void> original) {
        Freecam freecam = DrevClient.get().modules().get(Freecam.class);
        if (freecam.isEnabled()) {
            freecam.turn(dYaw, dPitch);
            return;
        }
        original.call(player, dYaw, dPitch);
    }
}

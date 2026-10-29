package dev.drevss.drevclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.movement.NoSlow;
import dev.drevss.drevclient.module.render.Freecam;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    private static boolean drev$noSlowItems() {
        if (DrevClient.get() == null) return false;
        NoSlow n = DrevClient.get().modules().get(NoSlow.class);
        return n.isEnabled() && n.items.isOn();
    }

    @WrapOperation(method = "aiStep()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z", ordinal = 0),
        require = 0)
    private boolean drev$aiStepItemUse(LocalPlayer instance, Operation<Boolean> original) {
        return !drev$noSlowItems() && original.call(instance);
    }

    @WrapOperation(method = "modifyInput(Lnet/minecraft/world/phys/Vec2;)Lnet/minecraft/world/phys/Vec2;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0),
        require = 0)
    private boolean drev$modifyInputItemUse(LocalPlayer instance, Operation<Boolean> original) {
        return !drev$noSlowItems() && original.call(instance);
    }

    @WrapOperation(method = "canStartSprinting()Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z", ordinal = 0),
        require = 0)
    private boolean drev$sprintItemUse(LocalPlayer instance, Operation<Boolean> original) {
        return !drev$noSlowItems() && original.call(instance);
    }

    /** Freecam: read WASD for the camera, then feed the player an empty input. */
    @WrapOperation(method = "aiStep()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick()V"),
        require = 0)
    private void drev$inputTick(ClientInput input, Operation<Void> original) {
        original.call(input);
        if (DrevClient.get() == null) return;
        Freecam freecam = DrevClient.get().modules().get(Freecam.class);
        if (!freecam.isEnabled()) return;
        freecam.setMoveVector(input.getMoveVector());
        input.keyPresses = Input.EMPTY;
        input.moveVector = Vec2.ZERO;
    }

    @Inject(method = "isShiftKeyDown()Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void drev$shift(CallbackInfoReturnable<Boolean> cir) {
        if (DrevClient.get() != null && DrevClient.get().modules().get(Freecam.class).isEnabled()) cir.setReturnValue(false);
    }

    /** Sprint "AllDirections": keep sprinting while moving sideways or backwards. */
    @WrapOperation(method = "shouldStopRunSprinting()Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z"),
        require = 0)
    private boolean drev$omniSprint(ClientInput input, Operation<Boolean> original) {
        if (DrevClient.get() != null) {
            var sprint = DrevClient.get().modules().get(dev.drevss.drevclient.module.movement.Sprint.class);
            if (sprint.isEnabled() && sprint.allDirections() && dev.drevss.drevclient.util.PlayerUtil.isMoving()) return true;
        }
        return original.call(input);
    }
}

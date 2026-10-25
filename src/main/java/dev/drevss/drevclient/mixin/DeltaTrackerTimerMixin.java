package dev.drevss.drevclient.mixin;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.world.Timer;
import net.minecraft.client.DeltaTracker;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeltaTracker.Timer.class)
public abstract class DeltaTrackerTimerMixin {
    @Shadow public float deltaTicks;

    @Inject(method = "advanceGameTime(J)I",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/DeltaTracker$Timer;lastMs:J", opcode = Opcodes.PUTFIELD, ordinal = 0))
    private void drev$timer(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        if (DrevClient.get() == null) return;
        deltaTicks *= DrevClient.get().modules().get(Timer.class).getMultiplier();
    }
}

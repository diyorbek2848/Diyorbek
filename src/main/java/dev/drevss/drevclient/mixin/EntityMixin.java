package dev.drevss.drevclient.mixin;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.combat.Velocity;
import dev.drevss.drevclient.module.movement.NoSlow;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /** Knockback from hits arrives as a motion update for our own player. */
    @ModifyVariable(method = "lerpMotion(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), argsOnly = true)
    private Vec3 drev$velocity(Vec3 motion) {
        if ((Object) this != Minecraft.getInstance().player || DrevClient.get() == null) return motion;
        return DrevClient.get().modules().get(Velocity.class).modify((Entity) (Object) this, motion);
    }

    /** NoSlow: cobwebs, berry bushes and powder snow. */
    @Inject(method = "makeStuckInBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At("HEAD"), cancellable = true)
    private void drev$stuck(BlockState state, Vec3 multiplier, CallbackInfo ci) {
        if ((Object) this != Minecraft.getInstance().player || DrevClient.get() == null) return;
        NoSlow noSlow = DrevClient.get().modules().get(NoSlow.class);
        if (noSlow.isEnabled() && noSlow.webs.isOn()) ci.cancel();
    }
}

package dev.drevss.drevclient.mixin;

import dev.drevss.drevclient.module.render.XRay;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** XRay for the vanilla block renderer. */
@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
    @Inject(method = "shouldRenderFace(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;ZLnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z",
        at = @At("HEAD"), cancellable = true, require = 0)
    private static void drev$xray(BlockAndTintGetter level, BlockState state, boolean cull, Direction side, BlockPos pos,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (XRay.isActive()) cir.setReturnValue(XRay.isVisible(state));
    }
}

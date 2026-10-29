package dev.drevss.drevclient.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.drevss.drevclient.module.render.XRay;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LiquidBlockRenderer.class)
public class LiquidBlockRendererMixin {
    @Inject(method = "tesselate(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)V",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void drev$xray(BlockAndTintGetter level, BlockPos pos, VertexConsumer consumer, BlockState state, FluidState fluid, CallbackInfo ci) {
        if (XRay.isActive() && !XRay.showsFluids()) ci.cancel();
    }
}

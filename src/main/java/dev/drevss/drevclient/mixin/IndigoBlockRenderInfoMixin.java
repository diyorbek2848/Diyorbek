package dev.drevss.drevclient.mixin;

import dev.drevss.drevclient.module.render.XRay;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** XRay for Fabric API's Indigo renderer, which builds chunks when Fabric API is installed. */
@Pseudo
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo", remap = false)
public class IndigoBlockRenderInfoMixin {
    @Shadow public BlockState blockState;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, require = 0)
    private void drev$xray(Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (XRay.isActive()) cir.setReturnValue(XRay.isVisible(blockState));
    }
}

package dev.drevss.drevclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.render.Freecam;
import dev.drevss.drevclient.module.render.XRay;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Normally chunk sections hidden behind solid stone are skipped. With XRay or
 * Freecam that would leave holes, so every section counts as see-through.
 */
@Mixin(SectionOcclusionGraph.class)
public class SectionOcclusionGraphMixin {
    @WrapOperation(method = "runUpdates",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionMesh;facesCanSeeEachother(Lnet/minecraft/core/Direction;Lnet/minecraft/core/Direction;)Z"),
        require = 0)
    private boolean drev$seeThrough(SectionMesh mesh, Direction from, Direction to, Operation<Boolean> original) {
        if (XRay.isActive()) return true;
        if (DrevClient.get() != null && DrevClient.get().modules().get(Freecam.class).isEnabled()) return true;
        return original.call(mesh, from, to);
    }
}

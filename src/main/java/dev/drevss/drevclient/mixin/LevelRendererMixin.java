package dev.drevss.drevclient.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.util.DrevRender;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(
        method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
        at = @At("RETURN"))
    private void drev$onRender(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker, boolean renderBlockOutline,
                               Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f matrix3,
                               GpuBufferSlice slice, Vector4f fogColor, boolean flag, CallbackInfo ci) {
        DrevRender.captureMatrices(positionMatrix, projectionMatrix);
        if (DrevClient.get() == null || Minecraft.getInstance().player == null) return;
        PoseStack matrices = new PoseStack();
        matrices.mulPose(positionMatrix);
        float pt = deltaTracker.getGameTimeDeltaPartialTick(false);
        DrevClient.get().modules().onRender3D(matrices, pt);
    }
}

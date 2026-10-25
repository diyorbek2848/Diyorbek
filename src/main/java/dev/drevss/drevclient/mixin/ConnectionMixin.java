package dev.drevss.drevclient.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.event.PacketEvent;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin extends SimpleChannelInboundHandler<Packet<?>> {
    @Inject(
        method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V",
            ordinal = 0),
        cancellable = true)
    private void drev$onReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (DrevClient.get() == null || Minecraft.getInstance().player == null) return;
        PacketEvent event = new PacketEvent(packet);
        DrevClient.get().modules().onPacketReceive(event);
        if (event.isCancelled()) ci.cancel();
    }

    @WrapMethod(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V")
    private void drev$onSend(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush, Operation<Void> original) {
        if (DrevClient.get() != null && Minecraft.getInstance().player != null) {
            PacketEvent event = new PacketEvent(packet);
            DrevClient.get().modules().onPacketSend(event);
            if (event.isCancelled()) return;
        }
        original.call(packet, listener, flush);
    }
}

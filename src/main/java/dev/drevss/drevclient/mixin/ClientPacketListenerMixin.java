package dev.drevss.drevclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.combat.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @ModifyExpressionValue(
        method = "handleExplosion(Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;playerKnockback()Ljava/util/Optional;"))
    private Optional<Vec3> drev$explosionKnockback(Optional<Vec3> original) {
        if (DrevClient.get() == null) return original;
        Velocity velocity = DrevClient.get().modules().get(Velocity.class);
        if (!velocity.isEnabled()) return original;
        return original.map(v -> velocity.scaleExplosion(v));
    }
}

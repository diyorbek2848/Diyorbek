package dev.drevss.drevclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.drevss.drevclient.DrevClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @ModifyReturnValue(method = "getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;", at = @At("RETURN"))
    private PlayerSkin drev$customSkin(PlayerSkin original) {
        if (DrevClient.get() == null || (Object) this != Minecraft.getInstance().player) return original;
        return DrevClient.get().skins().override(original);
    }
}

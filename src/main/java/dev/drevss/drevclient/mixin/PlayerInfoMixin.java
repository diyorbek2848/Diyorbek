package dev.drevss.drevclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.drevss.drevclient.DrevClient;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Your custom skin in the tab list and player heads that use your profile. */
@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin {
    @ModifyReturnValue(method = "getSkin()Lnet/minecraft/world/entity/player/PlayerSkin;", at = @At("RETURN"))
    private PlayerSkin drev$customSkin(PlayerSkin original) {
        if (DrevClient.get() == null) return original;
        var skins = DrevClient.get().skins();
        if (!skins.isLocalName(((PlayerInfo) (Object) this).getProfile().name())) return original;
        return skins.override(original);
    }
}

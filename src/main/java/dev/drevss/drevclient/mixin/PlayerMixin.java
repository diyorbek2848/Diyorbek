package dev.drevss.drevclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.player.Reach;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @ModifyReturnValue(method = "blockInteractionRange()D", at = @At("RETURN"))
    private double drev$blockReach(double original) {
        if ((Object) this != Minecraft.getInstance().player || DrevClient.get() == null) return original;
        Reach reach = DrevClient.get().modules().get(Reach.class);
        return reach.isEnabled() ? original + reach.blockBonus.getValue() : original;
    }

    @ModifyReturnValue(method = "entityInteractionRange()D", at = @At("RETURN"))
    private double drev$entityReach(double original) {
        if ((Object) this != Minecraft.getInstance().player || DrevClient.get() == null) return original;
        Reach reach = DrevClient.get().modules().get(Reach.class);
        return reach.isEnabled() ? original + reach.entityBonus.getValue() : original;
    }
}

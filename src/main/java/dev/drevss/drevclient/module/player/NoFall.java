package dev.drevss.drevclient.module.player;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class NoFall extends Module {
    private final NumberSetting minFall = add(new NumberSetting("MinFall", "Only act after falling this far", 2.5, 1, 10, 0.5));

    public NoFall() {
        super("NoFall", "Prevents fall damage", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player.getAbilities().invulnerable || mc.player.isFallFlying()) return;
        if (mc.player.fallDistance <= minFall.getValue()) return;
        if (mc.player.getDeltaMovement().y > -0.5) return;
        mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
    }
}

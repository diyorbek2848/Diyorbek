package dev.drevss.drevclient.module.player;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.util.ChatUtil;
import net.minecraft.client.gui.screens.DeathScreen;

public class AutoRespawn extends Module {
    private final BoolSetting coords = add(new BoolSetting("DeathCoords", "Print where you died", true));

    public AutoRespawn() {
        super("AutoRespawn", "Respawns instantly when you die", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (!(mc.screen instanceof DeathScreen)) return;
        if (coords.isOn()) {
            ChatUtil.info(String.format("You died at %d, %d, %d", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ()));
        }
        mc.player.respawn();
        mc.setScreen(null);
    }
}

package dev.drevss.drevclient.module.movement;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;

public class AutoWalk extends Module {
    public AutoWalk() {
        super("AutoWalk", "Holds the forward key for you", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.screen == null) mc.options.keyUp.setDown(true);
    }

    @Override
    public void onDisable() {
        if (inGame()) mc.options.keyUp.setDown(false);
    }
}

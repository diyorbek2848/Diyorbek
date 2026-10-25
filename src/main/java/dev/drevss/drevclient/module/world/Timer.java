package dev.drevss.drevclient.module.world;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.NumberSetting;

public class Timer extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", "Game speed multiplier", 2.0, 0.1, 10, 0.1));

    public Timer() {
        super("Timer", "Speeds up or slows down the game on your side", Category.WORLD);
    }

    public float getMultiplier() {
        return isEnabled() ? speed.getFloat() : 1f;
    }

    @Override
    public String getInfo() { return speed.getDisplayValue() + "x"; }
}

package dev.drevss.drevclient.module.render;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;

public class NoRender extends Module {
    public final BoolSetting hurtCam = add(new BoolSetting("HurtCam", "No screen shake when hit", true));
    public final BoolSetting fire = add(new BoolSetting("FireOverlay", "Hide the fire overlay", true));
    public final BoolSetting pumpkin = add(new BoolSetting("Pumpkin", "Hide the pumpkin overlay", true));
    public final BoolSetting weather = add(new BoolSetting("Weather", "Hide rain and snow", true));
    public final BoolSetting fog = add(new BoolSetting("Fog", "Remove distance fog", false));

    public NoRender() {
        super("NoRender", "Hides annoying visual effects", Category.RENDER);
    }
}

package dev.drevss.drevclient.module.client;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ModeSetting;

public class HudModule extends Module {
    public final ModeSetting colorMode = add(new ModeSetting("Colors", "HUD text colors", "Rainbow", "Rainbow", "Accent", "White"));
    public final BoolSetting background = add(new BoolSetting("Background", "Dark box behind HUD text", true));
    public final BoolSetting shadow = add(new BoolSetting("Shadow", "Text shadow", true));
    public final ModeSetting sorting = add(new ModeSetting("ListSort", "Module list order", "Length", "Length", "Alphabetical"));

    public HudModule() {
        super("HUD", "Overlay with coords, FPS, armor, modules and more. Edit layout in the HUD editor", Category.CLIENT);
        setEnabledSilently(true);
        setDrawn(false);
    }

    @Override
    protected boolean showToggleMessage() { return false; }
}

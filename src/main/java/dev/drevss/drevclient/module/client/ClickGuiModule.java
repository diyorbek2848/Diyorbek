package dev.drevss.drevclient.module.client;

import dev.drevss.drevclient.gui.ClickGuiScreen;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ColorSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class ClickGuiModule extends Module {
    public final ColorSetting accent = add(new ColorSetting("Accent", "Main GUI color", 0xFF9B5CFF));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", "Panel background opacity", 210, 60, 255, 5));
    public final BoolSetting blur = add(new BoolSetting("DimBackground", "Darken the game behind the GUI", true));
    public final BoolSetting descriptions = add(new BoolSetting("Descriptions", "Show tooltips when hovering", true));

    public ClickGuiModule() {
        super("ClickGUI", "Opens this menu", Category.CLIENT);
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
        setDrawn(false);
    }

    @Override
    public void onEnable() {
        if (mc.player != null) mc.setScreen(new ClickGuiScreen());
        setEnabledSilently(false);
    }

    @Override
    protected boolean showToggleMessage() { return false; }
}

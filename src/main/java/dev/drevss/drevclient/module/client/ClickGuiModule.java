package dev.drevss.drevclient.module.client;

import dev.drevss.drevclient.gui.ClickGuiScreen;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ColorSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class ClickGuiModule extends Module {
    public final ColorSetting accent = add(new ColorSetting("Accent", "Main GUI color", 0xFF8C7BFF));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", "Menu background opacity", 242, 120, 255, 5));
    public final BoolSetting blur = add(new BoolSetting("BlurBackground", "Blur the game behind the menu", true));
    public final BoolSetting descriptions = add(new BoolSetting("Descriptions", "Show tooltips when hovering", true));

    public ClickGuiModule() {
        super("ClickGUI", "Opens this menu", Category.CLIENT);
        setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
        setDrawn(false);
    }

    @Override
    public void onEnable() {
        // open on the next frame so the key press that opened it doesn't also reach the new screen
        if (mc.player != null) mc.execute(() -> mc.setScreen(new ClickGuiScreen()));
        setEnabledSilently(false);
    }

    @Override
    protected boolean showToggleMessage() { return false; }
}

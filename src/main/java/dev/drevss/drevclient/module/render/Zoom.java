package dev.drevss.drevclient.module.render;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class Zoom extends Module {
    private final NumberSetting factor = add(new NumberSetting("Factor", "How much to zoom in", 4, 1.5, 20, 0.5));

    public Zoom() {
        super("Zoom", "OptiFine-style zoom (hold C by default)", Category.RENDER);
        setKey(GLFW.GLFW_KEY_C);
        setHoldMode(true);
        setDrawn(false);
    }

    public float apply(float fov) {
        return isEnabled() ? (float) (fov / factor.getValue()) : fov;
    }

    @Override
    protected boolean showToggleMessage() { return false; }
}

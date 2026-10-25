package dev.drevss.drevclient.module.movement;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.NumberSetting;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class Step extends Module {
    private final NumberSetting height = add(new NumberSetting("Height", "Max step height in blocks", 1.0, 0.6, 4, 0.5));

    public Step() {
        super("Step", "Walk straight up blocks", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        AttributeInstance attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (attr != null && attr.getBaseValue() != height.getValue()) attr.setBaseValue(height.getValue());
    }

    @Override
    public void onDisable() {
        if (!inGame()) return;
        AttributeInstance attr = mc.player.getAttribute(Attributes.STEP_HEIGHT);
        if (attr != null) attr.setBaseValue(0.6);
    }

    @Override
    public String getInfo() { return height.getDisplayValue(); }
}

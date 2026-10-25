package dev.drevss.drevclient.module.player;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.NumberSetting;

/** Only adds client-side range; a server can still refuse interactions it thinks are too far. */
public class Reach extends Module {
    public final NumberSetting blockBonus = add(new NumberSetting("BlockBonus", "Extra block reach", 1, 0, 6, 0.5));
    public final NumberSetting entityBonus = add(new NumberSetting("EntityBonus", "Extra attack reach", 0.5, 0, 6, 0.1));

    public Reach() {
        super("Reach", "Extends your interaction range", Category.PLAYER);
    }

    @Override
    public String getInfo() { return "+" + blockBonus.getDisplayValue(); }
}

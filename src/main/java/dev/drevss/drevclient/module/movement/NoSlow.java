package dev.drevss.drevclient.module.movement;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;

public class NoSlow extends Module {
    public final BoolSetting items = add(new BoolSetting("Items", "No slowdown while eating, blocking or drawing a bow", true));
    public final BoolSetting webs = add(new BoolSetting("Webs", "No slowdown in cobwebs, berry bushes and powder snow", true));

    public NoSlow() {
        super("NoSlow", "Removes movement slowdowns", Category.MOVEMENT);
    }
}

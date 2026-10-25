package dev.drevss.drevclient.module.player;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.ModeSetting;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FastUse extends Module {
    private final ModeSetting items = add(new ModeSetting("Items", "What gets the faster use rate", "All", "All", "Blocks", "XP", "Crystals"));

    public FastUse() {
        super("FastUse", "Removes the right-click delay", Category.PLAYER);
    }

    @Override
    public void onTick() {
        ItemStack main = mc.player.getMainHandItem();
        ItemStack off = mc.player.getOffhandItem();
        boolean apply = switch (items.get()) {
            case "Blocks" -> main.getItem() instanceof BlockItem || off.getItem() instanceof BlockItem;
            case "XP" -> main.is(Items.EXPERIENCE_BOTTLE) || off.is(Items.EXPERIENCE_BOTTLE);
            case "Crystals" -> main.is(Items.END_CRYSTAL) || off.is(Items.END_CRYSTAL);
            default -> true;
        };
        if (apply) mc.rightClickDelay = 0;
    }

    @Override
    public String getInfo() { return items.get(); }
}

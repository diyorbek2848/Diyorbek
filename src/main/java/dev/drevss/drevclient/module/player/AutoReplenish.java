package dev.drevss.drevclient.module.player;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

public class AutoReplenish extends Module {
    private final NumberSetting threshold = add(new NumberSetting("Threshold", "Refill when a stack drops to this size", 8, 1, 63, 1));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between refills", 2, 0, 20, 1));

    private int timer;

    public AutoReplenish() {
        super("AutoReplenish", "Refills hotbar stacks from your inventory", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (timer > 0) { timer--; return; }
        if (mc.screen != null) return;

        for (int hb = 0; hb < 9; hb++) {
            ItemStack stack = InvUtil.inv().getItem(hb);
            if (stack.isEmpty() || !stack.isStackable()) continue;
            if (stack.getCount() > threshold.getValue() || stack.getCount() >= stack.getMaxStackSize()) continue;

            for (int i = 9; i < 36; i++) {
                ItemStack other = InvUtil.inv().getItem(i);
                if (!ItemStack.isSameItemSameComponents(stack, other)) continue;
                int from = InvUtil.toMenuSlot(i);
                int to = InvUtil.toMenuSlot(hb);
                InvUtil.click(from, 0, ClickType.PICKUP);
                InvUtil.click(to, 0, ClickType.PICKUP);
                if (!mc.player.inventoryMenu.getCarried().isEmpty()) InvUtil.click(from, 0, ClickType.PICKUP);
                timer = delay.getInt();
                return;
            }
        }
    }
}

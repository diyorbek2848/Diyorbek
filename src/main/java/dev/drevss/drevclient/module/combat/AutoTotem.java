package dev.drevss.drevclient.module.combat;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

public class AutoTotem extends Module {
    private final NumberSetting health = add(new NumberSetting("Health", "Only swap below this health (0 = always)", 0, 0, 20, 1));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between swaps", 0, 0, 10, 1));

    private int timer;

    public AutoTotem() {
        super("AutoTotem", "Keeps a Totem of Undying in your offhand", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (timer > 0) { timer--; return; }
        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        if (health.getValue() > 0 && mc.player.getHealth() + mc.player.getAbsorptionAmount() > health.getValue()) return;
        if (mc.screen instanceof AbstractContainerScreen<?> && !(mc.screen instanceof InventoryScreen)) return;

        int index = InvUtil.findInventory(s -> s.is(Items.TOTEM_OF_UNDYING));
        if (index == -1) return;

        int from = InvUtil.toMenuSlot(index);
        InvUtil.click(from, 0, ClickType.PICKUP);
        InvUtil.click(45, 0, ClickType.PICKUP);
        if (!mc.player.inventoryMenu.getCarried().isEmpty()) InvUtil.click(from, 0, ClickType.PICKUP);
        timer = delay.getInt();
    }

    @Override
    public String getInfo() {
        return inGame() ? String.valueOf(InvUtil.count(s -> s.is(Items.TOTEM_OF_UNDYING))) : null;
    }
}

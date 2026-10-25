package dev.drevss.drevclient.module.combat;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;

public class AutoArmor extends Module {
    private final BoolSetting keepElytra = add(new BoolSetting("KeepElytra", "Don't replace a worn elytra", true));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between equips", 3, 0, 20, 1));

    private static final EquipmentSlot[] SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private int timer;

    public AutoArmor() {
        super("AutoArmor", "Equips the best armor in your inventory", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (timer > 0) { timer--; return; }
        if (mc.screen instanceof AbstractContainerScreen<?> && !(mc.screen instanceof InventoryScreen)) return;

        for (EquipmentSlot slot : SLOTS) {
            ItemStack worn = mc.player.getItemBySlot(slot);
            if (keepElytra.isOn() && worn.is(Items.ELYTRA)) continue;

            int bestIndex = -1;
            int bestScore = score(worn, slot);
            for (int i = 0; i < 36; i++) {
                int s = score(InvUtil.inv().getItem(i), slot);
                if (s > bestScore) { bestScore = s; bestIndex = i; }
            }
            if (bestIndex == -1) continue;

            int armorMenuSlot = switch (slot) {
                case HEAD -> 5;
                case CHEST -> 6;
                case LEGS -> 7;
                default -> 8;
            };
            int from = InvUtil.toMenuSlot(bestIndex);
            if (worn.isEmpty()) {
                InvUtil.click(from, 0, ClickType.QUICK_MOVE);
            } else {
                InvUtil.click(from, 0, ClickType.PICKUP);
                InvUtil.click(armorMenuSlot, 0, ClickType.PICKUP);
                InvUtil.click(from, 0, ClickType.PICKUP);
            }
            timer = delay.getInt();
            return;
        }
    }

    /** -1 when the stack doesn't fit the slot. */
    private int score(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) return 0;
        Equippable eq = stack.get(DataComponents.EQUIPPABLE);
        if (eq == null || eq.slot() != slot) return -1;
        if (stack.is(Items.ELYTRA)) return 1;
        int score = InvUtil.tier(stack) * 100 + 10;
        if (stack.isEnchanted()) score += 20;
        score += (int) (InvUtil.durabilityFraction(stack) * 9);
        return score;
    }
}

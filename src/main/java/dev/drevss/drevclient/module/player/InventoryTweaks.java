package dev.drevss.drevclient.module.player;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.setting.TextSetting;
import dev.drevss.drevclient.util.InvUtil;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Deque;

public class InventoryTweaks extends Module {
    private final BoolSetting buttons = add(new BoolSetting("Buttons", "Steal/Dump buttons on containers", true));
    private final BoolSetting autoSteal = add(new BoolSetting("AutoSteal", "Take everything when a chest opens", false));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between item moves", 1, 0, 10, 1));
    private final BoolSetting dropJunk = add(new BoolSetting("DropJunk", "Throw away items from the junk list", false));
    private final TextSetting junk = add(new TextSetting("Junk", "Comma separated item ids", "rotten_flesh,poisonous_potato,spider_eye"));

    private final Deque<Integer> queue = new ArrayDeque<>();
    private int timer;

    public InventoryTweaks() {
        super("InventoryTweaks", "Chest stealing, dumping and junk dropping", Category.PLAYER);
    }

    public void onScreenInit(Screen screen) {
        if (!isEnabled() || !(screen instanceof AbstractContainerScreen<?> cs)) return;
        if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) return;
        if (buttons.isOn()) {
            Screens.getButtons(screen).add(Button.builder(Component.literal("Steal"), b -> steal(cs.getMenu()))
                .bounds(screen.width / 2 - 92, 4, 60, 16).build());
            Screens.getButtons(screen).add(Button.builder(Component.literal("Dump"), b -> dump(cs.getMenu()))
                .bounds(screen.width / 2 - 30, 4, 60, 16).build());
        }
        if (autoSteal.isOn()) steal(cs.getMenu());
    }

    private void steal(AbstractContainerMenu menu) {
        queue.clear();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (!(s.container instanceof Inventory) && s.hasItem()) queue.add(i);
        }
    }

    private void dump(AbstractContainerMenu menu) {
        queue.clear();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (s.container instanceof Inventory && s.hasItem()) queue.add(i);
        }
    }

    @Override
    public void onTick() {
        if (timer > 0) { timer--; return; }

        if (!queue.isEmpty()) {
            if (!(mc.screen instanceof AbstractContainerScreen<?>)) { queue.clear(); return; }
            int slot = queue.poll();
            InvUtil.clickContainer(slot, 0, ClickType.QUICK_MOVE);
            timer = delay.getInt();
            return;
        }

        if (dropJunk.isOn() && mc.screen == null) {
            for (int i = 0; i < 36; i++) {
                ItemStack s = InvUtil.inv().getItem(i);
                if (s.isEmpty() || !junk.asList().contains(InvUtil.id(s))) continue;
                InvUtil.click(InvUtil.toMenuSlot(i), 1, ClickType.THROW);
                timer = delay.getInt();
                return;
            }
        }
    }
}

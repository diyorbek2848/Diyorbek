package dev.drevss.drevclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Inventory helpers. "Inventory index" = Inventory#getItem index
 * (0-8 hotbar, 9-35 main, 36-39 armor, 40 offhand).
 */
public final class InvUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    private InvUtil() { }

    public static Inventory inv() { return mc.player.getInventory(); }

    public static int selected() { return inv().getSelectedSlot(); }

    public static void select(int hotbarSlot) {
        if (hotbarSlot >= 0 && hotbarSlot < 9) inv().setSelectedSlot(hotbarSlot);
    }

    public static int findHotbar(Predicate<ItemStack> filter) {
        for (int i = 0; i < 9; i++) if (filter.test(inv().getItem(i))) return i;
        return -1;
    }

    public static int findInventory(Predicate<ItemStack> filter) {
        for (int i = 0; i < 36; i++) if (filter.test(inv().getItem(i))) return i;
        return -1;
    }

    public static int findMain(Predicate<ItemStack> filter) {
        for (int i = 9; i < 36; i++) if (filter.test(inv().getItem(i))) return i;
        return -1;
    }

    public static int count(Predicate<ItemStack> filter) {
        int total = 0;
        for (int i = 0; i <= 40; i++) {
            ItemStack s = inv().getItem(i);
            if (filter.test(s)) total += s.getCount();
        }
        return total;
    }

    public static boolean isFull() {
        for (int i = 0; i < 36; i++) if (inv().getItem(i).isEmpty()) return false;
        return true;
    }

    /** Inventory index → slot number inside the player's inventory menu. */
    public static int toMenuSlot(int index) {
        if (index >= 0 && index < 9) return index + 36;    // hotbar
        if (index >= 9 && index < 36) return index;         // main
        if (index >= 36 && index < 40) return 44 - index;   // armor: 36 feet -> 8 ... 39 head -> 5
        if (index == 40) return 45;                          // offhand
        return -1;
    }

    /** Clicks a slot of the player's own inventory menu. */
    public static void click(int menuSlot, int button, ClickType type) {
        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, menuSlot, button, type, mc.player);
    }

    /** Clicks a slot of whatever container is open. */
    public static void clickContainer(int menuSlot, int button, ClickType type) {
        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, menuSlot, button, type, mc.player);
    }

    /** Moves an inventory index into a hotbar slot with a single SWAP click. */
    public static void swapToHotbar(int index, int hotbarSlot) {
        click(toMenuSlot(index), hotbarSlot, ClickType.SWAP);
    }

    /** Swaps the contents of two menu slots using pickup clicks. */
    public static void move(int fromMenuSlot, int toMenuSlot) {
        click(fromMenuSlot, 0, ClickType.PICKUP);
        click(toMenuSlot, 0, ClickType.PICKUP);
        if (!mc.player.inventoryMenu.getCarried().isEmpty()) click(fromMenuSlot, 0, ClickType.PICKUP);
    }

    public static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    public static String id(ItemStack stack) {
        return id(stack.getItem());
    }

    /** Tier score from an item id: netherite > diamond > iron > ... */
    public static int tier(ItemStack stack) {
        String id = id(stack);
        if (id.startsWith("netherite")) return 6;
        if (id.startsWith("diamond")) return 5;
        if (id.startsWith("iron")) return 4;
        if (id.startsWith("copper")) return 3;
        if (id.startsWith("chainmail") || id.startsWith("turtle")) return 3;
        if (id.startsWith("stone")) return 2;
        if (id.startsWith("golden")) return 2;
        if (id.startsWith("leather") || id.startsWith("wooden")) return 1;
        return 0;
    }

    public static float durabilityFraction(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) return 1f;
        return 1f - (float) stack.getDamageValue() / stack.getMaxDamage();
    }
}

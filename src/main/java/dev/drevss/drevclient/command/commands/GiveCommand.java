package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.util.ChatUtil;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class GiveCommand extends Command {
    public GiveCommand() { super("give", "give <item> [count]", "Creative only: puts an item in your hand", "g"); }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1) throw usage();
        if (!mc.player.hasInfiniteMaterials()) throw new CommandException("You need to be in creative mode");
        Item item;
        try {
            item = BuiltInRegistries.ITEM.getValue(Identifier.parse(args[0].contains(":") ? args[0] : "minecraft:" + args[0]));
        } catch (Exception e) {
            throw new CommandException("Invalid item id");
        }
        if (item == null || item == Items.AIR) throw new CommandException("Unknown item '" + args[0] + "'");
        int count = 1;
        if (args.length > 1) {
            try { count = Integer.parseInt(args[1]); } catch (NumberFormatException e) { throw usage(); }
        }
        ItemStack stack = new ItemStack(item, Math.max(1, Math.min(count, 99)));
        placeInHand(stack);
        ChatUtil.success("Gave " + stack.getCount() + "x " + stack.getHoverName().getString());
    }

    static void placeInHand(ItemStack stack) {
        int slot = InvUtil.selected();
        mc.player.getInventory().setItem(slot, stack);
        mc.player.connection.send(new ServerboundSetCreativeModeSlotPacket(InvUtil.toMenuSlot(slot), stack));
    }
}

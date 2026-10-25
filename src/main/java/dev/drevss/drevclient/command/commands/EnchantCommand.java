package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.util.ChatUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Optional;

public class EnchantCommand extends Command {
    public EnchantCommand() { super("enchant", "enchant <enchantment|all> [level]", "Creative only: enchants the held item (any level up to 255)", "e"); }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1) throw usage();
        if (!mc.player.hasInfiniteMaterials()) throw new CommandException("You need to be in creative mode");
        ItemStack stack = mc.player.getMainHandItem().copy();
        if (stack.isEmpty()) throw new CommandException("Hold an item first");

        int level = -1;
        if (args.length > 1) {
            try { level = Math.max(1, Math.min(255, Integer.parseInt(args[1]))); }
            catch (NumberFormatException e) { throw usage(); }
        }

        Registry<Enchantment> registry = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if (args[0].equalsIgnoreCase("all")) {
            int count = 0;
            for (Identifier id : registry.keySet()) {
                Optional<? extends Holder<Enchantment>> h = registry.get(id);
                if (h.isEmpty()) continue;
                Holder<Enchantment> holder = h.get();
                if (!holder.value().canEnchant(stack)) continue;
                stack.enchant(holder, level > 0 ? level : holder.value().getMaxLevel());
                count++;
            }
            GiveCommand.placeInHand(stack);
            ChatUtil.success("Applied " + count + " enchantments");
            return;
        }

        String raw = args[0].contains(":") ? args[0] : "minecraft:" + args[0];
        Optional<? extends Holder<Enchantment>> h;
        try { h = registry.get(Identifier.parse(raw)); }
        catch (Exception e) { throw new CommandException("Invalid enchantment id"); }
        if (h.isEmpty()) throw new CommandException("Unknown enchantment '" + args[0] + "'");
        Holder<Enchantment> holder = h.get();
        stack.enchant(holder, level > 0 ? level : holder.value().getMaxLevel());
        GiveCommand.placeInHand(stack);
        ChatUtil.success("Enchanted with " + raw + " " + (level > 0 ? level : holder.value().getMaxLevel()));
    }
}

package dev.drevss.drevclient.module.combat;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class AutoWeapon extends Module {
    private final ModeSetting prefer = add(new ModeSetting("Prefer", "Weapon type to use", "Sword", "Sword", "Axe", "Mace"));

    public AutoWeapon() {
        super("AutoWeapon", "Switches to your best weapon when attacking", Category.COMBAT);
    }

    @Override
    public void onAttack(Entity target) {
        if (!(target instanceof LivingEntity)) return;
        int best = -1;
        int bestScore = score(mc.player.getMainHandItem());
        for (int i = 0; i < 9; i++) {
            int s = score(InvUtil.inv().getItem(i));
            if (s > bestScore) { bestScore = s; best = i; }
        }
        if (best != -1) InvUtil.select(best);
    }

    private int score(ItemStack stack) {
        String id = InvUtil.id(stack);
        int base;
        if (id.endsWith("_sword")) base = prefer.is("Sword") ? 300 : 200;
        else if (id.endsWith("_axe")) base = prefer.is("Axe") ? 300 : 190;
        else if (id.equals("mace")) base = prefer.is("Mace") ? 400 : 150;
        else if (id.equals("trident")) base = 180;
        else return 0;
        return base + InvUtil.tier(stack) * 10 + (stack.isEnchanted() ? 5 : 0);
    }
}

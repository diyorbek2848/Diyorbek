package dev.drevss.drevclient.module.player;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoEat extends Module {
    private final NumberSetting hunger = add(new NumberSetting("Hunger", "Eat when food is at or below this", 14, 1, 19, 1));
    private final NumberSetting health = add(new NumberSetting("Health", "Also eat when health is at or below this", 10, 0, 20, 1));
    private final BoolSetting gapples = add(new BoolSetting("Gapples", "Allow golden apples", false));
    private final BoolSetting chorus = add(new BoolSetting("Chorus", "Allow chorus fruit", false));

    private boolean eating;
    private int prevSlot = -1;

    public AutoEat() {
        super("AutoEat", "Eats food from your hotbar when hungry", Category.PLAYER);
    }

    public boolean isEating() { return isEnabled() && eating; }

    @Override
    public void onDisable() { stop(); }

    @Override
    public void onTick() {
        int food = mc.player.getFoodData().getFoodLevel();
        boolean needs = food <= hunger.getValue() || (mc.player.getHealth() <= health.getValue() && food < 20);
        if (mc.player.getAbilities().instabuild) needs = false;

        if (eating) {
            if (!needs || food >= 20 || !isFood(mc.player.getMainHandItem())) stop();
            else mc.options.keyUse.setDown(true);
            return;
        }
        if (!needs || mc.screen != null) return;

        int best = -1;
        float bestSat = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = InvUtil.inv().getItem(i);
            if (!isFood(s)) continue;
            FoodProperties fp = s.get(DataComponents.FOOD);
            float sat = fp.nutrition() + fp.saturation();
            if (sat > bestSat) { bestSat = sat; best = i; }
        }
        if (best == -1) return;

        prevSlot = InvUtil.selected();
        InvUtil.select(best);
        mc.options.keyUse.setDown(true);
        eating = true;
    }

    private void stop() {
        if (eating) {
            mc.options.keyUse.setDown(false);
            if (prevSlot != -1) InvUtil.select(prevSlot);
        }
        eating = false;
        prevSlot = -1;
    }

    private boolean isFood(ItemStack s) {
        if (s.isEmpty() || !s.has(DataComponents.FOOD)) return false;
        if (s.is(Items.ROTTEN_FLESH) || s.is(Items.SPIDER_EYE) || s.is(Items.POISONOUS_POTATO)
            || s.is(Items.PUFFERFISH) || s.is(Items.SUSPICIOUS_STEW)) return false;
        if (!gapples.isOn() && (s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE))) return false;
        if (!chorus.isOn() && s.is(Items.CHORUS_FRUIT)) return false;
        return true;
    }
}

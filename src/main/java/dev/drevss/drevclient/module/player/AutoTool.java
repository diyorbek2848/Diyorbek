package dev.drevss.drevclient.module.player;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AutoTool extends Module {
    private final BoolSetting switchBack = add(new BoolSetting("SwitchBack", "Return to the previous slot when done", true));
    private final NumberSetting minDurability = add(new NumberSetting("MinDurability", "Avoid tools below this % durability", 3, 0, 50, 1));

    private int prevSlot = -1;

    public AutoTool() {
        super("AutoTool", "Picks the fastest tool for the block you mine", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (!mc.gameMode.isDestroying() || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            if (switchBack.isOn() && prevSlot != -1 && !mc.options.keyAttack.isDown()) {
                InvUtil.select(prevSlot);
                prevSlot = -1;
            }
            return;
        }
        BlockPos pos = hit.getBlockPos();
        int best = bestTool(mc.level.getBlockState(pos));
        if (best != -1 && best != InvUtil.selected()) {
            if (prevSlot == -1) prevSlot = InvUtil.selected();
            InvUtil.select(best);
        }
    }

    /** Hotbar slot with the highest dig speed for this block, or -1. */
    public int bestTool(BlockState state) {
        int best = -1;
        float bestSpeed = mc.player.getMainHandItem().getDestroySpeed(state);
        for (int i = 0; i < 9; i++) {
            ItemStack s = InvUtil.inv().getItem(i);
            if (s.isEmpty()) continue;
            if (s.isDamageableItem() && InvUtil.durabilityFraction(s) * 100 < minDurability.getValue()) continue;
            float speed = s.getDestroySpeed(state);
            if (speed > bestSpeed + 0.01f) { bestSpeed = speed; best = i; }
        }
        return best;
    }
}

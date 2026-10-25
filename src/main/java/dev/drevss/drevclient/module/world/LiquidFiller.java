package dev.drevss.drevclient.module.world;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.BlockUtil;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.material.FluidState;

public class LiquidFiller extends Module {
    private final ModeSetting liquids = add(new ModeSetting("Liquids", "Which liquids to fill", "Both", "Both", "Lava", "Water"));
    private final NumberSetting range = add(new NumberSetting("Range", "Fill radius", 4.5, 1, 6, 0.5));
    private final NumberSetting perTick = add(new NumberSetting("PerTick", "Blocks per tick", 2, 1, 8, 1));

    public LiquidFiller() {
        super("LiquidFiller", "Fills liquid source blocks around you", Category.WORLD);
    }

    @Override
    public void onTick() {
        int slot = InvUtil.findHotbar(s -> s.getItem() instanceof BlockItem bi && !(bi.getBlock() instanceof FallingBlock)
            && bi.getBlock().defaultBlockState().isCollisionShapeFullBlock(mc.level, BlockPos.ZERO));
        if (slot == -1) return;

        int placed = 0;
        int r = (int) Math.ceil(range.getValue());
        BlockPos center = mc.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (placed >= perTick.getInt()) return;
            if (BlockUtil.distanceToEyes(pos) > range.getValue()) continue;
            FluidState fluid = mc.level.getFluidState(pos);
            if (fluid.isEmpty() || !fluid.isSource()) continue;
            boolean lava = fluid.is(FluidTags.LAVA);
            if (liquids.is("Lava") && !lava || liquids.is("Water") && lava) continue;
            if (pos.equals(mc.player.blockPosition()) || pos.equals(mc.player.blockPosition().above())) continue;
            if (BlockUtil.placeWithSlot(pos.immutable(), slot, true)) placed++;
        }
    }
}

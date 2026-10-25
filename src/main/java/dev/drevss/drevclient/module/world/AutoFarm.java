package dev.drevss.drevclient.module.world;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.BlockUtil;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class AutoFarm extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Farming radius", 4.5, 1, 6, 0.5));
    private final BoolSetting harvest = add(new BoolSetting("Harvest", "Break fully grown crops", true));
    private final BoolSetting replant = add(new BoolSetting("Replant", "Plant seeds on empty farmland", true));
    private final BoolSetting bonemeal = add(new BoolSetting("Bonemeal", "Use bone meal on young crops", false));
    private final NumberSetting perTick = add(new NumberSetting("ActionsPerTick", "Actions each tick", 2, 1, 10, 1));

    /** crop block that each seed item plants */
    private final Map<Item, Block> seeds = new HashMap<>();
    private final Map<BlockPos, Item> lastCrop = new HashMap<>();

    public AutoFarm() {
        super("AutoFarm", "Harvests and replants crops around you", Category.WORLD);
        seeds.put(Items.WHEAT_SEEDS, Blocks.WHEAT);
        seeds.put(Items.CARROT, Blocks.CARROTS);
        seeds.put(Items.POTATO, Blocks.POTATOES);
        seeds.put(Items.BEETROOT_SEEDS, Blocks.BEETROOTS);
        seeds.put(Items.NETHER_WART, Blocks.NETHER_WART);
    }

    @Override
    public void onTick() {
        int actions = 0;
        int r = (int) Math.ceil(range.getValue());
        BlockPos center = mc.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -2, -r), center.offset(r, 2, r))) {
            if (actions >= perTick.getInt()) return;
            if (BlockUtil.distanceToEyes(pos) > range.getValue()) continue;
            BlockState state = mc.level.getBlockState(pos);
            Block block = state.getBlock();

            if (harvest.isOn() && isMature(state)) {
                lastCrop.put(pos.immutable(), seedFor(block));
                mc.gameMode.startDestroyBlock(pos, Direction.UP);
                mc.player.swing(InteractionHand.MAIN_HAND);
                actions++;
                continue;
            }
            if (bonemeal.isOn() && block instanceof CropBlock crop && !crop.isMaxAge(state)) {
                int slot = InvUtil.findHotbar(s -> s.is(Items.BONE_MEAL));
                if (slot != -1 && use(pos, slot, Direction.UP)) { actions++; continue; }
            }
            if (replant.isOn() && state.isAir()) {
                BlockState soil = mc.level.getBlockState(pos.below());
                boolean farmland = soil.is(Blocks.FARMLAND);
                boolean soulSand = soil.is(Blocks.SOUL_SAND);
                if (!farmland && !soulSand) continue;
                Item preferred = lastCrop.get(pos.immutable());
                int slot = -1;
                if (preferred != null) slot = InvUtil.findHotbar(s -> s.is(preferred));
                if (slot == -1) slot = InvUtil.findHotbar(s -> soulSand ? s.is(Items.NETHER_WART)
                    : seeds.containsKey(s.getItem()) && !s.is(Items.NETHER_WART));
                if (slot != -1 && use(pos.below(), slot, Direction.UP)) actions++;
            }
        }
    }

    private boolean use(BlockPos target, int slot, Direction face) {
        int prev = InvUtil.selected();
        InvUtil.select(slot);
        Vec3 hit = Vec3.atCenterOf(target).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, target, false));
        mc.player.swing(InteractionHand.MAIN_HAND);
        InvUtil.select(prev);
        return true;
    }

    private boolean isMature(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
        if (state.getBlock() instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= 3;
        return state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN);
    }

    private Item seedFor(Block block) {
        for (Map.Entry<Item, Block> e : seeds.entrySet()) if (e.getValue() == block) return e.getKey();
        return null;
    }
}

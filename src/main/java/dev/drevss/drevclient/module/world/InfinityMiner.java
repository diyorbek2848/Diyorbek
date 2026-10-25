package dev.drevss.drevclient.module.world;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.module.player.AutoTool;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.BlockUtil;
import dev.drevss.drevclient.util.ChatUtil;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Digs a 1x2 tunnel straight ahead forever. Stops on lava, a full inventory,
 * a worn-out pickaxe or a drop it can't see the bottom of.
 */
public class InfinityMiner extends Module {
    private final NumberSetting minDurability = add(new NumberSetting("MinDurability", "Stop when the pickaxe drops below this %", 5, 0, 50, 1));
    private final BoolSetting stopWhenFull = add(new BoolSetting("StopWhenFull", "Stop when your inventory is full", true));
    private final BoolSetting placeFloor = add(new BoolSetting("PlaceFloor", "Fill holes in the floor with blocks", true));

    private Direction facing = Direction.NORTH;
    private BlockPos breaking;

    public InfinityMiner() {
        super("InfinityMiner", "Tunnels forward automatically", Category.WORLD);
    }

    @Override
    public void onEnable() {
        if (!inGame()) return;
        facing = mc.player.getDirection();
        float yaw = facing.toYRot();
        mc.player.setYRot(yaw);
        mc.player.setXRot(10);
        BlockPos p = mc.player.blockPosition();
        mc.player.setPos(p.getX() + 0.5, mc.player.getY(), p.getZ() + 0.5);
        ChatUtil.info("Tunneling " + facing.getName());
    }

    @Override
    public void onDisable() {
        if (!inGame()) return;
        mc.options.keyUp.setDown(false);
        if (breaking != null) mc.gameMode.stopDestroyBlock();
        breaking = null;
    }

    private void stop(String reason) {
        ChatUtil.error("InfinityMiner stopped: " + reason);
        setEnabled(false);
    }

    @Override
    public void onTick() {
        if (stopWhenFull.isOn() && InvUtil.isFull()) { stop("inventory full"); return; }
        ItemStack held = mc.player.getMainHandItem();
        if (held.isDamageableItem() && InvUtil.durabilityFraction(held) * 100 < minDurability.getValue()) {
            stop("pickaxe almost broken");
            return;
        }

        mc.player.setYRot(facing.toYRot());
        BlockPos feet = mc.player.blockPosition();
        BlockPos aheadFeet = feet.relative(facing);
        BlockPos aheadHead = aheadFeet.above();

        // danger checks
        for (BlockPos p : new BlockPos[] { aheadFeet, aheadHead, aheadFeet.relative(facing), aheadHead.above(), aheadFeet.below() }) {
            if (mc.level.getFluidState(p).is(FluidTags.LAVA)) { stop("lava ahead"); return; }
        }

        BlockPos target = solid(aheadHead) ? aheadHead : solid(aheadFeet) ? aheadFeet : null;
        if (target != null) {
            mc.options.keyUp.setDown(false);
            AutoTool tool = DrevClient.get().modules().get(AutoTool.class);
            int best = tool.bestTool(mc.level.getBlockState(target));
            if (best != -1) InvUtil.select(best);
            if (!target.equals(breaking)) {
                if (breaking != null) mc.gameMode.stopDestroyBlock();
                breaking = target;
                mc.gameMode.startDestroyBlock(target, facing.getOpposite());
            }
            BlockUtil.mine(target);
            return;
        }
        breaking = null;

        BlockPos floor = aheadFeet.below();
        if (BlockUtil.isReplaceable(floor)) {
            if (!placeFloor.isOn()) { stop("hole ahead"); return; }
            int slot = InvUtil.findHotbar(s -> s.getItem() instanceof net.minecraft.world.item.BlockItem);
            if (slot == -1 || !BlockUtil.placeWithSlot(floor, slot, true)) { stop("hole ahead and no blocks"); return; }
        }
        mc.options.keyUp.setDown(true);
    }

    private boolean solid(BlockPos pos) {
        BlockState s = mc.level.getBlockState(pos);
        return !s.isAir() && s.getFluidState().isEmpty() && s.getDestroySpeed(mc.level, pos) >= 0;
    }

    @Override
    public String getInfo() { return facing.getName(); }
}

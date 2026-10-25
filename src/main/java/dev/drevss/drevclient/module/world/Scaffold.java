package dev.drevss.drevclient.module.world;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.util.BlockUtil;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module {
    private final BoolSetting tower = add(new BoolSetting("Tower", "Go straight up while holding jump", true));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", "Send rotations to the server", true));
    private final BoolSetting expand = add(new BoolSetting("Expand", "Place ahead of you when moving fast", true));

    public Scaffold() {
        super("Scaffold", "Places blocks under your feet", Category.WORLD);
    }

    @Override
    public void onTick() {
        int slot = InvUtil.findHotbar(this::isGoodBlock);
        if (slot == -1) return;

        BlockPos below = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
        if (BlockUtil.isReplaceable(below)) {
            if (!BlockUtil.placeWithSlot(below, slot, rotate.isOn())) {
                // nothing to click on: try the block diagonally below first
                for (BlockPos p : new BlockPos[] { below.north(), below.south(), below.east(), below.west() }) {
                    if (BlockUtil.isReplaceable(p) && BlockUtil.placeWithSlot(p, slot, rotate.isOn())) break;
                }
            }
        } else if (expand.isOn()) {
            Vec3 v = mc.player.getDeltaMovement();
            BlockPos ahead = BlockPos.containing(mc.player.getX() + v.x * 2, mc.player.getY() - 0.5, mc.player.getZ() + v.z * 2);
            if (BlockUtil.isReplaceable(ahead)) BlockUtil.placeWithSlot(ahead, slot, rotate.isOn());
        }

        if (tower.isOn() && mc.options.keyJump.isDown() && !dev.drevss.drevclient.util.PlayerUtil.isMoving()) {
            Vec3 v = mc.player.getDeltaMovement();
            if (mc.player.onGround()) mc.player.setDeltaMovement(v.x, 0.42, v.z);
        }
    }

    private boolean isGoodBlock(ItemStack s) {
        if (!(s.getItem() instanceof BlockItem bi)) return false;
        if (bi.getBlock() instanceof FallingBlock) return false;
        return bi.getBlock().defaultBlockState().isCollisionShapeFullBlock(mc.level, BlockPos.ZERO);
    }
}

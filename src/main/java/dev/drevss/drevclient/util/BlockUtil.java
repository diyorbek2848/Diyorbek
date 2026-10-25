package dev.drevss.drevclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class BlockUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    private BlockUtil() { }

    public static BlockState state(BlockPos pos) {
        return mc.level.getBlockState(pos);
    }

    public static boolean isReplaceable(BlockPos pos) {
        return state(pos).canBeReplaced();
    }

    public static boolean isSolid(BlockPos pos) {
        BlockState s = state(pos);
        return !s.isAir() && !s.canBeReplaced() && s.getFluidState().isEmpty();
    }

    public static double distanceToEyes(BlockPos pos) {
        return mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos));
    }

    /**
     * Places whatever the player holds in the given hand at pos by clicking
     * a solid neighbour. Returns false when there is nothing to click on.
     */
    public static boolean place(BlockPos pos, InteractionHand hand, boolean rotate) {
        if (!isReplaceable(pos)) return false;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (!isSolid(neighbor)) continue;
            Direction face = dir.getOpposite();
            Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
                face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            if (mc.player.getEyePosition().distanceTo(hitVec) > 5.5) continue;
            if (rotate) PlayerUtil.lookAt(hitVec, true);
            BlockHitResult hit = new BlockHitResult(hitVec, face, neighbor, false);
            mc.gameMode.useItemOn(mc.player, hand, hit);
            mc.player.swing(hand);
            return true;
        }
        return false;
    }

    /** Places using a hotbar slot, switching back afterwards. */
    public static boolean placeWithSlot(BlockPos pos, int hotbarSlot, boolean rotate) {
        if (hotbarSlot < 0) return false;
        int prev = InvUtil.selected();
        InvUtil.select(hotbarSlot);
        boolean ok = place(pos, InteractionHand.MAIN_HAND, rotate);
        InvUtil.select(prev);
        return ok;
    }

    /** Best face of a block to mine from (the one facing the player). */
    public static Direction facingSide(BlockPos pos) {
        Vec3 eyes = mc.player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(pos);
        Direction best = Direction.UP;
        double bestDist = Double.MAX_VALUE;
        for (Direction d : Direction.values()) {
            Vec3 faceCenter = center.add(d.getStepX() * 0.5, d.getStepY() * 0.5, d.getStepZ() * 0.5);
            double dist = eyes.distanceToSqr(faceCenter);
            if (dist < bestDist) { bestDist = dist; best = d; }
        }
        return best;
    }

    /** Continues mining a block. Instant-break blocks finish immediately. */
    public static void mine(BlockPos pos) {
        Direction side = facingSide(pos);
        if (mc.player.getAbilities().instabuild) {
            mc.gameMode.startDestroyBlock(pos, side);
        } else {
            mc.gameMode.continueDestroyBlock(pos, side);
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
    }
}

package dev.drevss.drevclient.module.combat;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.BlockUtil;
import dev.drevss.drevclient.util.ChatUtil;
import dev.drevss.drevclient.util.InvUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;

public class Surround extends Module {
    private final BoolSetting center = add(new BoolSetting("Center", "Snap to the middle of the block first", true));
    private final BoolSetting disableOnJump = add(new BoolSetting("DisableOnJump", "Turn off when you jump or move up", true));
    private final BoolSetting enderChests = add(new BoolSetting("EnderChests", "Use ender chests if out of obsidian", true));
    private final NumberSetting perTick = add(new NumberSetting("BlocksPerTick", "Blocks placed each tick", 2, 1, 8, 1));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", "Send rotations to the server", true));

    private double startY;

    public Surround() {
        super("Surround", "Surrounds your feet with obsidian", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        if (!inGame()) return;
        startY = mc.player.getY();
        if (center.isOn()) {
            BlockPos p = mc.player.blockPosition();
            mc.player.setPos(p.getX() + 0.5, mc.player.getY(), p.getZ() + 0.5);
            mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, 0);
        }
    }

    @Override
    public void onTick() {
        if (disableOnJump.isOn() && mc.player.getY() > startY + 0.5) {
            setEnabled(false);
            return;
        }
        int slot = InvUtil.findHotbar(s -> s.is(Items.OBSIDIAN));
        if (slot == -1 && enderChests.isOn()) slot = InvUtil.findHotbar(s -> s.is(Items.ENDER_CHEST));
        if (slot == -1) {
            ChatUtil.error("Surround: no obsidian in your hotbar");
            setEnabled(false);
            return;
        }

        BlockPos feet = BlockPos.containing(mc.player.getX(), mc.player.getY() + 0.2, mc.player.getZ());
        int placed = 0;
        BlockPos below = feet.below();
        if (BlockUtil.isReplaceable(below) && BlockUtil.placeWithSlot(below, slot, rotate.isOn())) placed++;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (placed >= perTick.getInt()) break;
            BlockPos pos = feet.relative(dir);
            if (!BlockUtil.isReplaceable(pos)) continue;
            if (!BlockUtil.placeWithSlot(pos, slot, rotate.isOn())) {
                // no neighbour to click: support it from below first
                BlockUtil.placeWithSlot(pos.below(), slot, rotate.isOn());
            }
            placed++;
        }
    }
}

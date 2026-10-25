package dev.drevss.drevclient.module.world;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.setting.TextSetting;
import dev.drevss.drevclient.util.BlockUtil;
import dev.drevss.drevclient.util.DrevRender;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Nuker extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Break radius", 4.5, 1, 6, 0.5));
    private final ModeSetting mode = add(new ModeSetting("Mode", "All, Flatten (only above your feet) or a block list", "All", "All", "Flatten", "List"));
    private final TextSetting list = add(new TextSetting("List", "Block ids for List mode", "stone,dirt,netherrack"));
    private final NumberSetting perTick = add(new NumberSetting("CreativePerTick", "Blocks per tick in creative", 8, 1, 64, 1));
    private final BoolSetting render = add(new BoolSetting("Render", "Highlight the block being broken", true));

    private BlockPos current;

    public Nuker() {
        super("Nuker", "Breaks blocks around you", Category.WORLD);
    }

    @Override
    public void onDisable() {
        if (inGame() && current != null) mc.gameMode.stopDestroyBlock();
        current = null;
    }

    @Override
    public void onTick() {
        List<BlockPos> targets = findTargets();
        if (targets.isEmpty()) { current = null; return; }

        if (mc.player.getAbilities().instabuild) {
            int n = Math.min(perTick.getInt(), targets.size());
            for (int i = 0; i < n; i++) BlockUtil.mine(targets.get(i));
            current = targets.get(0);
            return;
        }
        if (current == null || !targets.contains(current)) {
            if (current != null) mc.gameMode.stopDestroyBlock();
            current = targets.get(0);
            mc.gameMode.startDestroyBlock(current, BlockUtil.facingSide(current));
        }
        BlockUtil.mine(current);
    }

    private List<BlockPos> findTargets() {
        double r = range.getValue();
        int ri = (int) Math.ceil(r);
        BlockPos center = mc.player.blockPosition();
        int feetY = center.getY();
        List<String> allowed = mode.is("List") ? normalized(list.asList()) : null;
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-ri, -ri, -ri), center.offset(ri, ri, ri))) {
            if (BlockUtil.distanceToEyes(pos) > r) continue;
            if (mode.is("Flatten") && pos.getY() < feetY) continue;
            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) continue;
            if (state.getDestroySpeed(mc.level, pos) < 0) continue; // bedrock etc.
            if (allowed != null && !allowed.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())) continue;
            out.add(pos.immutable());
        }
        out.sort(Comparator.comparingDouble(BlockUtil::distanceToEyes));
        return out;
    }

    static List<String> normalized(List<String> ids) {
        List<String> out = new ArrayList<>();
        for (String s : ids) out.add(s.contains(":") ? s : "minecraft:" + s);
        return out;
    }

    @Override
    public void onRender3D(PoseStack matrices, float pt) {
        if (render.isOn() && current != null) DrevRender.drawBox(matrices, new AABB(current), 0x30FF4040, 0xFFFF4040);
    }

    @Override
    public String getInfo() { return mode.get(); }
}

package dev.drevss.drevclient.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.util.DrevRender;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class StorageESP extends Module {
    private final BoolSetting chests = add(new BoolSetting("Chests", "Chests and trapped chests", true));
    private final BoolSetting enderChests = add(new BoolSetting("EnderChests", "Ender chests", true));
    private final BoolSetting shulkers = add(new BoolSetting("Shulkers", "Shulker boxes", true));
    private final BoolSetting barrels = add(new BoolSetting("Barrels", "Barrels", true));
    private final BoolSetting others = add(new BoolSetting("Others", "Furnaces, hoppers, dispensers", false));
    private final BoolSetting tracers = add(new BoolSetting("Tracers", "Lines to storage", false));

    public StorageESP() {
        super("StorageESP", "Highlights chests and other storage", Category.RENDER);
    }

    public static Stream<BlockEntity> loadedBlockEntities() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        int radius = Math.max(2, mc.options.getEffectiveRenderDistance()) + 2;
        return ChunkPos.rangeClosed(mc.player.chunkPosition(), radius)
            .filter(c -> mc.level.hasChunk(c.x, c.z))
            .map(c -> mc.level.getChunk(c.x, c.z))
            .filter(c -> c instanceof LevelChunk)
            .flatMap(c -> ((LevelChunk) c).getBlockEntities().values().stream());
    }

    private int colorOf(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return chests.isOn() ? 0xFFFF6A00 : 0;
        if (be instanceof ChestBlockEntity) return chests.isOn() ? 0xFFFFAA00 : 0;
        if (be instanceof EnderChestBlockEntity) return enderChests.isOn() ? 0xFFAA00FF : 0;
        if (be instanceof ShulkerBoxBlockEntity) return shulkers.isOn() ? 0xFFFF55FF : 0;
        if (be instanceof BarrelBlockEntity) return barrels.isOn() ? 0xFFB5793C : 0;
        if (be instanceof AbstractFurnaceBlockEntity || be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity)
            return others.isOn() ? 0xFF9A9A9A : 0;
        return 0;
    }

    @Override
    public void onRender3D(PoseStack matrices, float pt) {
        List<DrevRender.ColoredBox> boxes = new ArrayList<>();
        List<DrevRender.ColoredLine> lines = new ArrayList<>();
        loadedBlockEntities().forEach(be -> {
            int c = colorOf(be);
            if (c == 0) return;
            boxes.add(new DrevRender.ColoredBox(new AABB(be.getBlockPos()).deflate(0.0625), DrevRender.withAlpha(c, 40), c));
            if (tracers.isOn()) lines.add(new DrevRender.ColoredLine(null, Vec3.atCenterOf(be.getBlockPos()), c));
        });
        DrevRender.drawBoxes(matrices, boxes, true, true);
        DrevRender.drawTracers(matrices, lines);
    }
}

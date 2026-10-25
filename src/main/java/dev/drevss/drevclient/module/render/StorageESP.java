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
    private final dev.drevss.drevclient.setting.NumberSetting range = add(new dev.drevss.drevclient.setting.NumberSetting(
        "Range", "Max distance (only chunks your client has loaded can be shown)", 1000, 16, 1000, 8));

    public StorageESP() {
        super("StorageESP", "Highlights chests and other storage", Category.RENDER);
    }

    public static Stream<BlockEntity> loadedBlockEntities() {
        return loadedBlockEntities(0);
    }

    /** Block entities in every chunk the client has loaded, out to rangeBlocks (0 = render distance). */
    public static Stream<BlockEntity> loadedBlockEntities(double rangeBlocks) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        int radius = Math.max(Math.max(2, mc.options.getEffectiveRenderDistance()), (int) Math.ceil(rangeBlocks / 16.0)) + 2;
        radius = Math.min(radius, 66);
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

    private record Found(net.minecraft.core.BlockPos pos, int color) { }
    private List<Found> cache = new ArrayList<>();
    private int timer;

    @Override
    public void onEnable() { timer = 0; }

    /** Scanning up to 1000 blocks is too heavy for every frame, so it runs twice a second. */
    @Override
    public void onTick() {
        if (timer-- > 0) return;
        timer = 10;
        double max = range.getValue();
        double maxSq = max * max;
        List<Found> found = new ArrayList<>();
        loadedBlockEntities(max).forEach(be -> {
            int c = colorOf(be);
            if (c == 0 || be.getBlockPos().distToCenterSqr(mc.player.position()) > maxSq) return;
            found.add(new Found(be.getBlockPos().immutable(), c));
        });
        cache = found;
    }

    @Override
    public void onRender3D(PoseStack matrices, float pt) {
        List<DrevRender.ColoredBox> boxes = new ArrayList<>();
        List<DrevRender.ColoredLine> lines = new ArrayList<>();
        for (Found f : cache) {
            boxes.add(new DrevRender.ColoredBox(new AABB(f.pos()).deflate(0.0625), DrevRender.withAlpha(f.color(), 40), f.color()));
            if (tracers.isOn()) lines.add(new DrevRender.ColoredLine(null, Vec3.atCenterOf(f.pos()), f.color()));
        }
        DrevRender.drawBoxes(matrices, boxes, true, true);
        DrevRender.drawTracers(matrices, lines);
    }

    @Override
    public String getInfo() { return String.valueOf(cache.size()); }
}

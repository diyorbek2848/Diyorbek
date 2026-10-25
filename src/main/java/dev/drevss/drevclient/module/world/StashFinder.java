package dev.drevss.drevclient.module.world;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.module.render.StorageESP;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.ChatUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StashFinder extends Module {
    private final NumberSetting minStorage = add(new NumberSetting("MinStorage", "Containers in one chunk to count as a stash", 8, 2, 50, 1));
    private final BoolSetting saveToFile = add(new BoolSetting("SaveToFile", "Write finds to config/drevclient/stashes.txt", true));

    private final Set<Long> reported = new HashSet<>();
    private int timer;

    public StashFinder() {
        super("StashFinder", "Alerts you about chunks full of storage", Category.WORLD);
    }

    @Override
    public void onEnable() { reported.clear(); }

    @Override
    public void onTick() {
        if (++timer < 40) return;
        timer = 0;

        Map<Long, int[]> counts = new HashMap<>();
        StorageESP.loadedBlockEntities().forEach(be -> {
            if (!isStorage(be)) return;
            ChunkPos cp = new ChunkPos(be.getBlockPos());
            int[] c = counts.computeIfAbsent(cp.toLong(), k -> new int[3]);
            c[0]++;
            if (be instanceof ShulkerBoxBlockEntity) c[1]++;
        });

        for (Map.Entry<Long, int[]> e : counts.entrySet()) {
            int total = e.getValue()[0];
            if (total < minStorage.getValue() || !reported.add(e.getKey())) continue;
            ChunkPos cp = new ChunkPos(e.getKey());
            int x = cp.getMiddleBlockX(), z = cp.getMiddleBlockZ();
            String line = String.format("Stash: %d containers (%d shulkers) at X %d Z %d", total, e.getValue()[1], x, z);
            ChatUtil.success(line);
            if (saveToFile.isOn()) write(line);
        }
    }

    private boolean isStorage(BlockEntity be) {
        return be instanceof ChestBlockEntity || be instanceof ShulkerBoxBlockEntity
            || be instanceof BarrelBlockEntity || be instanceof EnderChestBlockEntity;
    }

    private void write(String line) {
        try {
            Path file = DrevClient.get().config().getDir().resolve("stashes.txt");
            String server = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "singleplayer";
            Files.writeString(file, LocalDateTime.now().withNano(0) + " [" + server + "] " + line + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) { }
    }

    @Override
    public String getInfo() { return String.valueOf(reported.size()); }
}

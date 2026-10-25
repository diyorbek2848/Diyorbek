package dev.drevss.drevclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Scans the blocks around the player on a background thread so big radii
 * (XRay, BlockESP) don't stall the game. Results are read on the render thread.
 */
public class BlockScanner {
    public record Found(BlockPos pos, BlockState state) { }

    private final Minecraft mc = Minecraft.getInstance();
    private volatile List<Found> results = Collections.emptyList();
    private volatile boolean running;
    private long lastScan;

    public List<Found> results() { return results; }

    public void clear() { results = Collections.emptyList(); }

    /** Starts a new scan if the previous one finished and enough time passed. */
    public void update(int radius, int maxResults, long intervalMs, Predicate<BlockState> filter) {
        if (running || mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastScan < intervalMs) return;
        lastScan = now;
        running = true;

        BlockPos center = mc.player.blockPosition();
        ClientLevel level = mc.level;
        Thread t = new Thread(() -> {
            List<Found> found = new ArrayList<>();
            try {
                int minY = Math.max(level.getMinY(), center.getY() - radius);
                int maxY = Math.min(level.getMaxY(), center.getY() + radius);
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                outer:
                for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                    for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                        if (!level.hasChunk(x >> 4, z >> 4)) continue;
                        for (int y = minY; y <= maxY; y++) {
                            pos.set(x, y, z);
                            BlockState state = level.getBlockState(pos);
                            if (!state.isAir() && filter.test(state)) {
                                found.add(new Found(pos.immutable(), state));
                                if (found.size() >= maxResults) break outer;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
                // the level changed under us (dimension switch, disconnect); just skip this scan
            }
            results = found;
            running = false;
        }, "DrevClient-BlockScanner");
        t.setDaemon(true);
        t.start();
    }
}

package dev.drevss.drevclient.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.setting.TextSetting;
import dev.drevss.drevclient.util.BlockScanner;
import dev.drevss.drevclient.util.DrevRender;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Real X-ray: every block that isn't in the list becomes invisible, so ores,
 * spawners and chests can be seen from anywhere. Chunks are rebuilt when
 * it's turned on or off.
 */
public class XRay extends Module {
    public static final String DEFAULT_BLOCKS = "diamond_ore,deepslate_diamond_ore,ancient_debris,emerald_ore,deepslate_emerald_ore,"
        + "gold_ore,deepslate_gold_ore,nether_gold_ore,iron_ore,deepslate_iron_ore,redstone_ore,deepslate_redstone_ore,"
        + "lapis_ore,deepslate_lapis_ore,copper_ore,deepslate_copper_ore,coal_ore,deepslate_coal_ore,nether_quartz_ore,"
        + "spawner,trial_spawner,chest,trapped_chest,ender_chest,barrel,end_portal_frame,budding_amethyst,raw_iron_block,raw_gold_block";

    private final TextSetting blocks = add(new TextSetting("Blocks", "Blocks that stay visible (comma separated ids)", DEFAULT_BLOCKS));
    private final BoolSetting fluids = add(new BoolSetting("ShowFluids", "Keep water and lava visible", false));
    private final BoolSetting highlight = add(new BoolSetting("Highlight", "Also draw boxes around diamonds and debris", true));
    private final NumberSetting radius = add(new NumberSetting("HighlightRadius", "Scan radius for the boxes", 48, 8, 128, 8));
    private final BoolSetting nightVision = add(new BoolSetting("Fullbright", "Light everything up while active", true));

    private static volatile Set<Block> visible = Collections.emptySet();
    private static volatile boolean active;
    private static volatile boolean showFluids;
    private final BlockScanner scanner = new BlockScanner();
    private String lastList = "";
    private boolean lastFluids;

    public XRay() {
        super("XRay", "See ores, spawners and chests through every other block", Category.RENDER);
    }

    // ---- read by the chunk-building mixins (render worker threads) ----
    public static boolean isActive() { return active; }
    public static boolean showsFluids() { return showFluids; }
    public static boolean isVisible(BlockState state) { return visible.contains(state.getBlock()); }

    private void rebuildList() {
        Set<Block> set = new HashSet<>();
        for (String id : blocks.asList()) {
            String full = id.contains(":") ? id : "minecraft:" + id;
            BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.tryParse(full)).ifPresent(set::add);
        }
        visible = set;
        lastList = blocks.get();
    }

    @Override
    public void onEnable() {
        rebuildList();
        showFluids = fluids.isOn();
        lastFluids = showFluids;
        active = true;
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }

    @Override
    public void onDisable() {
        active = false;
        scanner.clear();
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
        if (inGame() && nightVision.isOn()) mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }

    @Override
    public void onTick() {
        if (!blocks.get().equals(lastList) || fluids.isOn() != lastFluids) {
            rebuildList();
            showFluids = fluids.isOn();
            lastFluids = showFluids;
            mc.levelRenderer.allChanged();
        }
        if (highlight.isOn()) scanner.update(radius.getInt(), 4000, 750, s -> highlightColor(s) != 0);
        if (nightVision.isOn()) {
            MobEffectInstance nv = mc.player.getEffect(MobEffects.NIGHT_VISION);
            if (nv == null || nv.getDuration() < 220)
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000, 0, false, false));
        }
    }

    private int highlightColor(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (id.endsWith("diamond_ore")) return 0xFF33E5FF;
        if (id.equals("ancient_debris")) return 0xFFB0703A;
        if (id.endsWith("emerald_ore")) return 0xFF22FF66;
        return 0;
    }

    @Override
    public void onRender3D(PoseStack matrices, float pt) {
        if (!highlight.isOn()) return;
        List<DrevRender.ColoredBox> boxes = new ArrayList<>();
        for (BlockScanner.Found f : scanner.results()) {
            int c = highlightColor(f.state());
            if (c != 0) boxes.add(new DrevRender.ColoredBox(new AABB(f.pos()), DrevRender.withAlpha(c, 40), c));
        }
        DrevRender.drawBoxes(matrices, boxes, true, true);
    }

    /** Number of highlighted ores (used by the HUD and the automated test). */
    @Override
    public String getInfo() { return String.valueOf(scanner.results().size()); }
}

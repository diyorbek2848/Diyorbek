package dev.drevss.drevclient.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.BlockScanner;
import dev.drevss.drevclient.util.DrevRender;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/** Ore X-ray: highlights ores through stone so you can see veins from anywhere. */
public class XRay extends Module {
    private final NumberSetting radius = add(new NumberSetting("Radius", "Scan radius in blocks", 48, 8, 128, 8));
    private final BoolSetting diamond = add(new BoolSetting("Diamond", "Show diamond ore", true));
    private final BoolSetting debris = add(new BoolSetting("AncientDebris", "Show ancient debris", true));
    private final BoolSetting emerald = add(new BoolSetting("Emerald", "Show emerald ore", true));
    private final BoolSetting gold = add(new BoolSetting("Gold", "Show gold ore", true));
    private final BoolSetting iron = add(new BoolSetting("Iron", "Show iron ore", true));
    private final BoolSetting redstone = add(new BoolSetting("Redstone", "Show redstone ore", false));
    private final BoolSetting lapis = add(new BoolSetting("Lapis", "Show lapis ore", false));
    private final BoolSetting copper = add(new BoolSetting("Copper", "Show copper ore", false));
    private final BoolSetting coal = add(new BoolSetting("Coal", "Show coal ore", false));
    private final BoolSetting quartz = add(new BoolSetting("Quartz", "Show nether quartz", false));
    private final BoolSetting fill = add(new BoolSetting("Fill", "Fill the boxes", true));
    private final BoolSetting nightVision = add(new BoolSetting("Fullbright", "Light up caves while active", true));

    private final BlockScanner scanner = new BlockScanner();

    public XRay() {
        super("XRay", "Shows ores through walls", Category.RENDER);
    }

    @Override
    public void onDisable() {
        scanner.clear();
        if (inGame() && nightVision.isOn()) mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }

    @Override
    public void onTick() {
        scanner.update(radius.getInt(), 4000, 750, s -> colorFor(s) != 0);
        if (nightVision.isOn()) {
            MobEffectInstance nv = mc.player.getEffect(MobEffects.NIGHT_VISION);
            if (nv == null || nv.getDuration() < 220)
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000, 0, false, false));
        }
    }

    private int colorFor(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (diamond.isOn() && id.endsWith("diamond_ore")) return 0xFF33E5FF;
        if (debris.isOn() && id.equals("ancient_debris")) return 0xFF8B5A2B;
        if (emerald.isOn() && id.endsWith("emerald_ore")) return 0xFF22FF66;
        if (gold.isOn() && (id.endsWith("gold_ore"))) return 0xFFFFD700;
        if (iron.isOn() && id.endsWith("iron_ore")) return 0xFFE0B090;
        if (redstone.isOn() && id.endsWith("redstone_ore")) return 0xFFFF2020;
        if (lapis.isOn() && id.endsWith("lapis_ore")) return 0xFF2050FF;
        if (copper.isOn() && id.endsWith("copper_ore")) return 0xFFE07040;
        if (coal.isOn() && id.endsWith("coal_ore")) return 0xFF404040;
        if (quartz.isOn() && id.equals("nether_quartz_ore")) return 0xFFF0F0F0;
        return 0;
    }

    @Override
    public void onRender3D(PoseStack matrices, float pt) {
        List<DrevRender.ColoredBox> boxes = new ArrayList<>();
        for (BlockScanner.Found f : scanner.results()) {
            int c = colorFor(f.state());
            if (c == 0) continue;
            boxes.add(new DrevRender.ColoredBox(new AABB(f.pos()), DrevRender.withAlpha(c, 50), c));
        }
        DrevRender.drawBoxes(matrices, boxes, fill.isOn(), true);
    }

    @Override
    public String getInfo() { return String.valueOf(scanner.results().size()); }
}

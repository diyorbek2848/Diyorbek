package dev.drevss.drevclient.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ColorSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.setting.TextSetting;
import dev.drevss.drevclient.util.BlockScanner;
import dev.drevss.drevclient.util.DrevRender;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockESP extends Module {
    private final TextSetting blocks = add(new TextSetting("Blocks", "Comma separated block ids", "spawner,trial_spawner,end_portal_frame,budding_amethyst"));
    private final NumberSetting radius = add(new NumberSetting("Radius", "Scan radius", 64, 8, 128, 8));
    private final ColorSetting color = add(new ColorSetting("Color", "Box color", 0xFFFF66CC));
    private final BoolSetting tracers = add(new BoolSetting("Tracers", "Lines to each block", false));

    private final BlockScanner scanner = new BlockScanner();
    private Set<String> wanted = new HashSet<>();
    private String lastText = "";

    public BlockESP() {
        super("BlockESP", "Highlights chosen blocks through walls", Category.RENDER);
    }

    @Override
    public void onDisable() { scanner.clear(); }

    @Override
    public void onTick() {
        if (!blocks.get().equals(lastText)) {
            lastText = blocks.get();
            Set<String> set = new HashSet<>();
            for (String s : blocks.asList()) set.add(s.contains(":") ? s : "minecraft:" + s);
            wanted = set;
        }
        Set<String> filter = wanted;
        scanner.update(radius.getInt(), 2000, 1000, s -> filter.contains(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString()));
    }

    @Override
    public void onRender3D(PoseStack matrices, float pt) {
        int c = color.getColor();
        List<DrevRender.ColoredBox> boxes = new ArrayList<>();
        List<DrevRender.ColoredLine> lines = new ArrayList<>();
        for (BlockScanner.Found f : scanner.results()) {
            boxes.add(new DrevRender.ColoredBox(new AABB(f.pos()), DrevRender.withAlpha(c, 45), c));
            if (tracers.isOn()) lines.add(new DrevRender.ColoredLine(null, Vec3.atCenterOf(f.pos()), c));
        }
        DrevRender.drawBoxes(matrices, boxes, true, true);
        DrevRender.drawTracers(matrices, lines);
    }

    @Override
    public String getInfo() { return String.valueOf(scanner.results().size()); }
}

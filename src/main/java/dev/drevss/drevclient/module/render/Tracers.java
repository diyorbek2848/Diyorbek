package dev.drevss.drevclient.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.DrevRender;
import dev.drevss.drevclient.util.EntityUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class Tracers extends Module {
    private final BoolSetting players = add(new BoolSetting("Players", "Lines to players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Lines to hostile mobs", false));
    private final BoolSetting animals = add(new BoolSetting("Animals", "Lines to animals", false));
    private final ModeSetting target = add(new ModeSetting("Target", "Where the line ends", "Body", "Body", "Head", "Feet"));
    private final ModeSetting colorMode = add(new ModeSetting("Color", "ESP color or distance-based", "Distance", "Distance", "ESP"));
    private final NumberSetting range = add(new NumberSetting("Range", "Max distance", 1000, 16, 1000, 8));

    public Tracers() {
        super("Tracers", "Draws lines to entities", Category.RENDER);
    }

    @Override
    public void onRender3D(PoseStack matrices, float pt) {
        ESP esp = DrevClient.get().modules().get(ESP.class);
        double max = range.getValue();
        List<DrevRender.ColoredLine> lines = new ArrayList<>();
        for (Entity e : (Iterable<Entity>) EntityUtil.all()::iterator) {
            if (e == mc.player || !e.isAlive()) continue;
            boolean ok = e instanceof Player ? players.isOn()
                : EntityUtil.isHostile(e) ? mobs.isOn()
                : EntityUtil.isPassive(e) && animals.isOn();
            if (!ok) continue;
            double dist = e.distanceTo(mc.player);
            if (dist > max) continue;

            Vec3 pos = EntityUtil.lerpPos(e, pt);
            double yOff = switch (target.get()) {
                case "Head" -> e.getBbHeight();
                case "Feet" -> 0;
                default -> e.getBbHeight() / 2;
            };
            int color;
            if (EntityUtil.isFriend(e)) color = 0xFF55FFFF;
            else if (colorMode.is("ESP")) color = esp.colorOf(e);
            else color = DrevRender.healthColor((float) Math.min(1, dist / 60.0));
            lines.add(new DrevRender.ColoredLine(null, pos.add(0, yOff, 0), color));
        }
        DrevRender.drawTracers(matrices, lines);
    }
}

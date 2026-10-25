package dev.drevss.drevclient.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ColorSetting;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.DrevRender;
import dev.drevss.drevclient.util.EntityUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class ESP extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Box draws 3D boxes, Glow uses the vanilla outline", "Box", "Box", "Glow", "Both"));
    private final BoolSetting fill = add(new BoolSetting("Fill", "Fill the boxes", true));
    private final NumberSetting fillAlpha = add(new NumberSetting("FillAlpha", "Box fill opacity", 40, 0, 255, 5));
    private final NumberSetting range = add(new NumberSetting("Range", "Max distance", 128, 16, 512, 16));
    private final BoolSetting players = add(new BoolSetting("Players", "Show players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Show hostile mobs", true));
    private final BoolSetting animals = add(new BoolSetting("Animals", "Show animals", false));
    private final BoolSetting items = add(new BoolSetting("Items", "Show dropped items", false));
    private final BoolSetting crystals = add(new BoolSetting("Crystals", "Show end crystals", true));
    private final ColorSetting playerColor = add(new ColorSetting("PlayerColor", "Color for players", 0xFFB06BFF));
    private final ColorSetting friendColor = add(new ColorSetting("FriendColor", "Color for friends", 0xFF55FFFF));
    private final ColorSetting mobColor = add(new ColorSetting("MobColor", "Color for hostile mobs", 0xFFFF5555));
    private final ColorSetting animalColor = add(new ColorSetting("AnimalColor", "Color for animals", 0xFF55FF55));
    private final ColorSetting itemColor = add(new ColorSetting("ItemColor", "Color for items and crystals", 0xFFFFD84A));

    public ESP() {
        super("ESP", "Highlights entities through walls", Category.RENDER);
    }

    private boolean wanted(Entity e) {
        if (e == mc.player || !e.isAlive()) return false;
        if (e.distanceToSqr(mc.player) > range.getValue() * range.getValue()) return false;
        if (e instanceof Player) return players.isOn();
        if (e instanceof ItemEntity) return items.isOn();
        if (e instanceof EndCrystal) return crystals.isOn();
        if (EntityUtil.isHostile(e)) return mobs.isOn();
        if (EntityUtil.isPassive(e)) return animals.isOn();
        return false;
    }

    public int colorOf(Entity e) {
        if (e instanceof Player) return EntityUtil.isFriend(e) ? friendColor.getColor() : playerColor.getColor();
        if (e instanceof ItemEntity || e instanceof EndCrystal) return itemColor.getColor();
        if (EntityUtil.isHostile(e)) return mobColor.getColor();
        return animalColor.getColor();
    }

    public boolean shouldGlow(Entity e) {
        return !mode.is("Box") && mc.player != null && wanted(e);
    }

    @Override
    public void onRender3D(PoseStack matrices, float pt) {
        if (mode.is("Glow")) return;
        List<DrevRender.ColoredBox> boxes = new ArrayList<>();
        EntityUtil.all().filter(this::wanted).forEach(e -> {
            int c = colorOf(e);
            boxes.add(new DrevRender.ColoredBox(EntityUtil.lerpBox(e, pt).inflate(0.05),
                DrevRender.withAlpha(c, fillAlpha.getInt()), c));
        });
        DrevRender.drawBoxes(matrices, boxes, fill.isOn(), true);
    }

    @Override
    public String getInfo() { return mode.get(); }
}

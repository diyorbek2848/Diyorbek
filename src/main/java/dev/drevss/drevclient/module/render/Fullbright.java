package dev.drevss.drevclient.module.render;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class Fullbright extends Module {
    public Fullbright() {
        super("Fullbright", "See in the dark", Category.RENDER);
    }

    @Override
    public void onTick() {
        MobEffectInstance current = mc.player.getEffect(MobEffects.NIGHT_VISION);
        if (current == null || current.getDuration() < 220) {
            // client-side only; the server never sees this effect
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000, 0, false, false));
        }
    }

    @Override
    public void onDisable() {
        if (inGame()) mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }
}

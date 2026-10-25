package dev.drevss.drevclient.module.misc;

import dev.drevss.drevclient.event.PacketEvent;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.TextSetting;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;

public class SoundBlocker extends Module {
    private final TextSetting blocked = add(new TextSetting("Blocked", "Comma separated words in sound ids to mute",
        "explode,wither.spawn,thunder,dragon.death,ender_dragon.growl"));

    public SoundBlocker() {
        super("SoundBlocker", "Mutes chosen sounds sent by the server", Category.MISC);
    }

    @Override
    public void onPacketReceive(PacketEvent event) {
        Holder<SoundEvent> sound = null;
        if (event.getPacket() instanceof ClientboundSoundPacket p) sound = p.getSound();
        else if (event.getPacket() instanceof ClientboundSoundEntityPacket p) sound = p.getSound();
        if (sound == null) return;
        String id = sound.getRegisteredName();
        for (String word : blocked.asList()) {
            if (id.contains(word)) {
                event.cancel();
                return;
            }
        }
    }
}

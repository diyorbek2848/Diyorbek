package dev.drevss.drevclient.module.misc;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class BetterChat extends Module {
    private final BoolSetting timestamps = add(new BoolSetting("Timestamps", "Show the time before messages", true));
    private final BoolSetting antiSpam = add(new BoolSetting("AntiSpam", "Hide repeated messages", true));
    private final BoolSetting highlight = add(new BoolSetting("Highlight", "Mark messages that mention you", true));

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private String lastText = "";
    private long lastTime;

    public BetterChat() {
        super("BetterChat", "Timestamps, anti-spam and mention highlights", Category.MISC);
    }

    /** Returns the message to show, or null to hide it. */
    public Component process(Component message) {
        String text = message.getString();
        long now = System.currentTimeMillis();
        if (antiSpam.isOn() && text.equals(lastText) && now - lastTime < 15000) {
            lastTime = now;
            return null;
        }
        lastText = text;
        lastTime = now;

        MutableComponent out = Component.empty();
        if (timestamps.isOn()) {
            out.append(Component.literal("[" + LocalTime.now().format(TIME) + "] ").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (highlight.isOn() && mc.player != null) {
            String me = mc.player.getName().getString();
            if (!me.isEmpty() && text.contains(me) && !text.startsWith("<" + me + ">")) {
                out.append(Component.literal("» ").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        out.append(message);
        return out;
    }
}

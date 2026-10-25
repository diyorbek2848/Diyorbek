package dev.drevss.drevclient.module.misc;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.setting.TextSetting;

import java.util.List;
import java.util.Random;

public class Spam extends Module {
    private final TextSetting messages = add(new TextSetting("Messages", "Messages separated by | (start with / for commands)", "DrevClient on top!|Join my server!"));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Seconds between messages", 10, 1, 300, 1));
    private final BoolSetting random = add(new BoolSetting("Random", "Pick messages in random order", false));
    private final BoolSetting antiFilter = add(new BoolSetting("AntiFilter", "Append a few random characters", false));

    private final Random rng = new Random();
    private int timer;
    private int index;

    public Spam() {
        super("Spam", "Sends chat messages on a timer", Category.MISC);
    }

    @Override
    public void onTick() {
        if (++timer < delay.getValue() * 20) return;
        timer = 0;
        List<String> list = List.of(messages.get().split("\\|"));
        if (list.isEmpty()) return;
        String msg = random.isOn() ? list.get(rng.nextInt(list.size())) : list.get(index++ % list.size());
        msg = msg.trim();
        if (msg.isEmpty()) return;
        if (antiFilter.isOn()) msg += " " + Integer.toHexString(rng.nextInt(0xFFFF));
        if (msg.startsWith("/")) mc.player.connection.sendCommand(msg.substring(1));
        else mc.player.connection.sendChat(msg);
    }
}

package dev.drevss.drevclient.manager;

import dev.drevss.drevclient.DrevClient;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A macro runs one or more lines when its key is pressed. Lines are separated
 * by ";". A line starting with the client prefix runs a DrevClient command,
 * "/" runs a server command, anything else is sent as chat.
 */
public class MacroManager {
    private final Map<Integer, String> macros = new LinkedHashMap<>();

    public void set(int key, String actions) { macros.put(key, actions); }
    public boolean remove(int key) { return macros.remove(key) != null; }
    public Map<Integer, String> all() { return macros; }

    public void onKey(int key) {
        String actions = macros.get(key);
        if (actions == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String prefix = DrevClient.get().commands().getPrefix();
        for (String raw : actions.split(";")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith(prefix)) DrevClient.get().commands().execute(line.substring(prefix.length()));
            else if (line.startsWith("/")) mc.player.connection.sendCommand(line.substring(1));
            else mc.player.connection.sendChat(line);
        }
    }
}

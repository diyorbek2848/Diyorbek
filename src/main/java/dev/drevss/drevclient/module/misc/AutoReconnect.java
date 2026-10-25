package dev.drevss.drevclient.module.misc;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

public class AutoReconnect extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay", "Seconds before reconnecting", 5, 1, 60, 1));

    private ServerData lastServer;
    private int ticksLeft = -1;
    private Button button;

    public AutoReconnect() {
        super("AutoReconnect", "Rejoins the server after you get disconnected", Category.MISC);
    }

    public void rememberServer(ServerData server) {
        if (server != null) lastServer = server;
    }

    public void onScreenInit(Screen screen) {
        if (!(screen instanceof DisconnectedScreen) || lastServer == null) return;
        ticksLeft = isEnabled() ? delay.getInt() * 20 : -1;
        button = Button.builder(Component.literal(label()), b -> {
            if (ticksLeft >= 0) ticksLeft = -1;
            else reconnect();
            b.setMessage(Component.literal(label()));
        }).bounds(screen.width / 2 - 100, screen.height - 30, 200, 20).build();
        Screens.getButtons(screen).add(button);
    }

    private String label() {
        if (ticksLeft >= 0) return "Reconnecting in " + (ticksLeft / 20 + 1) + "s (click to cancel)";
        return "Reconnect to " + (lastServer != null ? lastServer.ip : "server");
    }

    /** Runs every client tick, even on menus. */
    public void tickAlways() {
        if (ticksLeft < 0) return;
        if (!(mc.screen instanceof DisconnectedScreen)) { ticksLeft = -1; return; }
        ticksLeft--;
        if (button != null) button.setMessage(Component.literal(label()));
        if (ticksLeft <= 0) {
            ticksLeft = -1;
            reconnect();
        }
    }

    private void reconnect() {
        if (lastServer == null) return;
        Screen parent = new JoinMultiplayerScreen(new TitleScreen());
        ConnectScreen.startConnecting(parent, mc, ServerAddress.parseString(lastServer.ip), lastServer, false, null);
    }
}

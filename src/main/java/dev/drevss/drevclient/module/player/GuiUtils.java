package dev.drevss.drevclient.module.player;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.util.ChatUtil;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;

/**
 * UI utilities for testing your own server's GUIs and plugins:
 * close a menu without telling the server, desync it, or save and reopen it.
 */
public class GuiUtils extends Module {
    private final BoolSetting showButtons = add(new BoolSetting("Buttons", "Show the buttons on container screens", true));

    private Screen saved;

    public GuiUtils() {
        super("GuiUtils", "UI utils: soft close, desync and save/restore container GUIs", Category.PLAYER);
    }

    public void onScreenInit(Screen screen) {
        if (!isEnabled() || !showButtons.isOn()) return;
        if (!(screen instanceof AbstractContainerScreen<?>) || screen instanceof CreativeModeInventoryScreen) return;
        int x = 4, y = 4, w = 90, h = 16;
        Screens.getButtons(screen).add(Button.builder(Component.literal("Soft close"), b -> softClose()).bounds(x, y, w, h).build());
        Screens.getButtons(screen).add(Button.builder(Component.literal("Desync"), b -> desync()).bounds(x, y + 18, w, h).build());
        Screens.getButtons(screen).add(Button.builder(Component.literal("Save GUI"), b -> save()).bounds(x, y + 36, w, h).build());
        Screens.getButtons(screen).add(Button.builder(Component.literal("Sync ID: " + mc.player.containerMenu.containerId),
            b -> ChatUtil.info("Container id " + mc.player.containerMenu.containerId
                + ", state id " + mc.player.containerMenu.getStateId())).bounds(x, y + 54, w, h).build());
    }

    /** Closes the screen on the client only; the server still thinks it's open. */
    public void softClose() {
        if (mc.screen != null) mc.setScreen(null);
    }

    /** Tells the server the container closed while keeping it open here. */
    public void desync() {
        if (mc.player == null) return;
        mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
        ChatUtil.info("Sent close packet, the GUI stays open on your side");
    }

    public void save() {
        if (!(mc.screen instanceof AbstractContainerScreen<?>)) {
            ChatUtil.error("Open a container first");
            return;
        }
        saved = mc.screen;
        mc.setScreen(null);
        ChatUtil.info("GUI saved. Use .gui restore to reopen it");
    }

    public void restore() {
        if (saved == null) {
            ChatUtil.error("No saved GUI");
            return;
        }
        mc.setScreen(saved);
    }
}

package dev.drevss.drevclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ChatUtil {
    public static final String PREFIX = "§5[§dDrev§5]§r ";

    private ChatUtil() { }

    public static void info(String message) {
        send(PREFIX + "§7" + message);
    }

    public static void success(String message) {
        send(PREFIX + "§a" + message);
    }

    public static void error(String message) {
        send(PREFIX + "§c" + message);
    }

    public static void raw(String message) {
        send(message);
    }

    public static void toggle(String module, boolean on) {
        send(PREFIX + "§f" + module + (on ? " §aenabled" : " §cdisabled"));
    }

    private static void send(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui == null) return;
        // chat is touched only on the render thread
        mc.execute(() -> mc.gui.getChat().addMessage(Component.literal(text)));
    }
}

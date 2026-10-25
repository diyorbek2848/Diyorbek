package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.util.ChatUtil;

public class CoordsCommand extends Command {
    public CoordsCommand() { super("coords", "coords", "Copies your coordinates to the clipboard", "pos"); }

    @Override
    public void execute(String[] args) {
        String s = mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ();
        mc.keyboardHandler.setClipboard(s);
        ChatUtil.success("Copied " + s);
    }
}

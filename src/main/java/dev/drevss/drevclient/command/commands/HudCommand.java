package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.gui.HudEditorScreen;
import dev.drevss.drevclient.hud.HudElement;
import dev.drevss.drevclient.util.ChatUtil;

public class HudCommand extends Command {
    public HudCommand() { super("hud", "hud [edit|list|toggle <element>]", "Opens the HUD editor or toggles HUD elements"); }

    @Override
    public void execute(String[] args) throws CommandException {
        var hud = DrevClient.get().hud();
        if (args.length == 0 || args[0].equalsIgnoreCase("edit")) {
            mc.execute(() -> mc.setScreen(new HudEditorScreen(null)));
            return;
        }
        if (args[0].equalsIgnoreCase("list")) {
            for (HudElement e : hud.elements()) ChatUtil.raw("  " + (e.isVisible() ? "§a" : "§c") + e.getName());
            return;
        }
        if (args[0].equalsIgnoreCase("toggle") && args.length > 1) {
            HudElement e = hud.get(args[1]);
            if (e == null) throw new CommandException("No HUD element '" + args[1] + "'");
            e.setVisible(!e.isVisible());
            ChatUtil.success(e.getName() + (e.isVisible() ? " shown" : " hidden"));
            return;
        }
        throw usage();
    }
}

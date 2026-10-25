package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.module.player.GuiUtils;

public class GuiCommand extends Command {
    public GuiCommand() { super("gui", "gui <restore|close|desync>", "UI utils: reopen a saved GUI, soft close or desync"); }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1) throw usage();
        GuiUtils gu = DrevClient.get().modules().get(GuiUtils.class);
        switch (args[0].toLowerCase()) {
            case "restore" -> mc.execute(gu::restore);
            case "close" -> mc.execute(gu::softClose);
            case "desync" -> gu.desync();
            default -> throw usage();
        }
    }
}

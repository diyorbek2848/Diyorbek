package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.util.ChatUtil;

public class DrawnCommand extends Command {
    public DrawnCommand() { super("drawn", "drawn <module>", "Shows or hides a module in the HUD list"); }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1) throw usage();
        Module m = DrevClient.get().modules().get(args[0]);
        if (m == null) throw new CommandException("Module not found");
        m.setDrawn(!m.isDrawn());
        ChatUtil.success(m.getName() + (m.isDrawn() ? " is shown" : " is hidden") + " in the module list");
    }
}

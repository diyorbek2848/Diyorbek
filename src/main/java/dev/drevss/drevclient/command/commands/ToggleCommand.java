package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.module.Module;

public class ToggleCommand extends Command {
    public ToggleCommand() { super("toggle", "toggle <module> [on|off]", "Turns a module on or off", "t"); }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1) throw usage();
        Module m = DrevClient.get().modules().get(args[0]);
        if (m == null) throw new CommandException("Module '" + args[0] + "' not found");
        if (args.length > 1) m.setEnabled(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true"));
        else m.toggle();
    }
}

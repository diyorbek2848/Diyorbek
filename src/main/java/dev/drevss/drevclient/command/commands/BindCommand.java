package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.util.ChatUtil;
import dev.drevss.drevclient.util.KeyUtil;

public class BindCommand extends Command {
    public BindCommand() { super("bind", "bind <module> <key|none> [hold]", "Binds a module to a key (add 'hold' for hold-to-use)", "b"); }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 2) throw usage();
        Module m = DrevClient.get().modules().get(args[0]);
        if (m == null) throw new CommandException("Module '" + args[0] + "' not found");
        int key = KeyUtil.parse(args[1]);
        if (key == -2) throw new CommandException("Unknown key '" + args[1] + "'. Examples: R, F6, RSHIFT, NUMPAD1");
        m.setKey(key);
        m.setHoldMode(args.length > 2 && args[2].equalsIgnoreCase("hold"));
        if (key < 0) ChatUtil.success("Unbound " + m.getName());
        else ChatUtil.success("Bound " + m.getName() + " to " + KeyUtil.name(key) + (m.isHoldMode() ? " (hold)" : ""));
    }
}

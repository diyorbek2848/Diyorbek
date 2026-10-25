package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.util.ChatUtil;

public class HelpCommand extends Command {
    public HelpCommand() { super("help", "help [command]", "Lists commands or explains one", "h", "?"); }

    @Override
    public void execute(String[] args) {
        var cm = DrevClient.get().commands();
        String p = cm.getPrefix();
        if (args.length > 0) {
            Command c = cm.get(args[0]);
            if (c == null) { ChatUtil.error("No such command"); return; }
            ChatUtil.info("§d" + p + c.getSyntax() + "§7 - " + c.getDescription());
            return;
        }
        ChatUtil.info("§dDrevClient commands §7(" + cm.all().size() + "):");
        for (Command c : cm.all()) ChatUtil.raw("  §d" + p + c.getSyntax() + " §8- §7" + c.getDescription());
        ChatUtil.raw("  §7Tip: §d" + p + "<module>§7 toggles a module, Right Shift opens the GUI");
    }
}

package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.util.ChatUtil;
import dev.drevss.drevclient.util.KeyUtil;

import java.util.Map;

public class MacroCommand extends Command {
    public MacroCommand() {
        super("macro", "macro <add|remove|list> [key] [actions; separated; by semicolons]",
            "Runs chat lines, /commands or .commands when a key is pressed", "m");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        var macros = DrevClient.get().macros();
        if (args.length < 1) throw usage();
        switch (args[0].toLowerCase()) {
            case "add", "set" -> {
                if (args.length < 3) throw usage();
                int key = KeyUtil.parse(args[1]);
                if (key < 0) throw new CommandException("Unknown key '" + args[1] + "'");
                String actions = join(args, 2);
                macros.set(key, actions);
                ChatUtil.success("Macro on " + KeyUtil.name(key) + ": " + actions);
            }
            case "remove", "del" -> {
                if (args.length < 2) throw usage();
                int key = KeyUtil.parse(args[1]);
                if (macros.remove(key)) ChatUtil.success("Removed macro on " + KeyUtil.name(key));
                else ChatUtil.error("No macro on that key");
            }
            case "list" -> {
                ChatUtil.info("Macros:");
                for (Map.Entry<Integer, String> e : macros.all().entrySet())
                    ChatUtil.raw("  §d" + KeyUtil.name(e.getKey()) + " §8- §f" + e.getValue());
            }
            default -> throw usage();
        }
    }
}

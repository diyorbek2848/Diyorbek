package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.util.ChatUtil;

public class ConfigCommand extends Command {
    public ConfigCommand() { super("config", "config <save|load> [name]", "Saves or loads a named config", "cfg"); }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1) throw usage();
        String name = args.length > 1 ? args[1].replaceAll("[^a-zA-Z0-9_-]", "") : "default";
        var cfg = DrevClient.get().config();
        switch (args[0].toLowerCase()) {
            case "save" -> { if (cfg.save(name)) ChatUtil.success("Saved config '" + name + "'"); else ChatUtil.error("Save failed"); }
            case "load" -> { if (cfg.load(name)) ChatUtil.success("Loaded config '" + name + "'"); else ChatUtil.error("No config named '" + name + "'"); }
            default -> throw usage();
        }
    }
}

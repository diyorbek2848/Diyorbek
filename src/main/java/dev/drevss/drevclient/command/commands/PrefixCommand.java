package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.util.ChatUtil;

public class PrefixCommand extends Command {
    public PrefixCommand() { super("prefix", "prefix <symbol>", "Changes the command prefix"); }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1 || args[0].startsWith("/")) throw usage();
        DrevClient.get().commands().setPrefix(args[0]);
        ChatUtil.success("Prefix is now " + DrevClient.get().commands().getPrefix());
    }
}

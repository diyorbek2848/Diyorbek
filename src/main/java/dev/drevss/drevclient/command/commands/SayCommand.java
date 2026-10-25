package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.command.Command;

public class SayCommand extends Command {
    public SayCommand() { super("say", "say <message>", "Sends a chat message that starts with the prefix"); }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1) throw usage();
        mc.player.connection.sendChat(join(args, 0));
    }
}

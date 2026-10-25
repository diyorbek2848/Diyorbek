package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.util.ChatUtil;

public class FriendCommand extends Command {
    public FriendCommand() { super("friend", "friend <add|remove|list|clear> [name]", "Manages friends (combat modules ignore them)", "f"); }

    @Override
    public void execute(String[] args) throws CommandException {
        var friends = DrevClient.get().friends();
        if (args.length < 1) throw usage();
        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) throw usage();
                if (friends.add(args[1])) ChatUtil.success("Added " + args[1] + " as a friend");
                else ChatUtil.info(args[1] + " is already a friend");
            }
            case "remove", "del" -> {
                if (args.length < 2) throw usage();
                if (friends.remove(args[1])) ChatUtil.success("Removed " + args[1]);
                else ChatUtil.error(args[1] + " isn't a friend");
            }
            case "list" -> ChatUtil.info("Friends (" + friends.all().size() + "): " + String.join(", ", friends.all()));
            case "clear" -> { friends.clear(); ChatUtil.success("Friend list cleared"); }
            default -> throw usage();
        }
    }
}

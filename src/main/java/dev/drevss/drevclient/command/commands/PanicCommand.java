package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.util.ChatUtil;

public class PanicCommand extends Command {
    public PanicCommand() { super("panic", "panic", "Turns every module off"); }

    @Override
    public void execute(String[] args) {
        DrevClient.get().modules().disableAll();
        ChatUtil.info("All modules disabled");
    }
}

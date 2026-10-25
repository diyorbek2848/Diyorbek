package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.util.ChatUtil;
import dev.drevss.drevclient.util.KeyUtil;

public class BindsCommand extends Command {
    public BindsCommand() { super("binds", "binds", "Lists all key binds"); }

    @Override
    public void execute(String[] args) {
        ChatUtil.info("Binds:");
        for (Module m : DrevClient.get().modules().all()) {
            if (m.getKey() >= 0)
                ChatUtil.raw("  §f" + m.getName() + " §8- §d" + KeyUtil.name(m.getKey()) + (m.isHoldMode() ? " §7(hold)" : ""));
        }
    }
}

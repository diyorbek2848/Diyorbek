package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.util.ChatUtil;

import java.util.ArrayList;
import java.util.List;

public class ModulesCommand extends Command {
    public ModulesCommand() { super("modules", "modules", "Lists every module", "list"); }

    @Override
    public void execute(String[] args) {
        for (Category c : Category.values()) {
            List<String> names = new ArrayList<>();
            for (Module m : DrevClient.get().modules().byCategory(c)) names.add((m.isEnabled() ? "§a" : "§7") + m.getName());
            ChatUtil.raw("§d" + c.displayName + ": " + String.join("§8, ", names));
        }
    }
}

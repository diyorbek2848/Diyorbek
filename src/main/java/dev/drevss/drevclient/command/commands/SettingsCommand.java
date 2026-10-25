package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.Command;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.setting.Setting;
import dev.drevss.drevclient.util.ChatUtil;

public class SettingsCommand extends Command {
    public SettingsCommand() {
        super("settings", "settings <module> [setting] [value|reset]", "Shows or changes module settings", "set", "s");
    }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1) throw usage();
        Module m = DrevClient.get().modules().get(args[0]);
        if (m == null) throw new CommandException("Module '" + args[0] + "' not found");

        if (args.length == 1) {
            ChatUtil.info("§d" + m.getName() + "§7: " + m.getDescription());
            for (Setting<?> s : m.getSettings()) {
                String extra = "";
                if (s instanceof NumberSetting n) extra = " §8[" + n.getMin() + " - " + n.getMax() + "]";
                if (s instanceof ModeSetting ms) extra = " §8" + ms.getModes();
                String shown = s.getName().equals("ApiKey") && !s.getDisplayValue().isEmpty() ? "********" : s.getDisplayValue();
                ChatUtil.raw("  §f" + s.getName() + " §8= §d" + shown + extra + " §7" + s.getDescription());
            }
            return;
        }

        Setting<?> s = m.getSetting(args[1]);
        if (s == null) throw new CommandException("Setting '" + args[1] + "' not found in " + m.getName());
        if (args.length == 2) {
            ChatUtil.info(m.getName() + " " + s.getName() + " = " + s.getDisplayValue());
            return;
        }
        String value = join(args, 2);
        if (value.equalsIgnoreCase("reset")) {
            s.reset();
            ChatUtil.success(s.getName() + " reset to " + s.getDisplayValue());
            return;
        }
        if (!s.parse(value)) throw new CommandException("Invalid value '" + value + "' for " + s.getName());
        ChatUtil.success(m.getName() + " " + s.getName() + " set to " + s.getDisplayValue());
    }
}

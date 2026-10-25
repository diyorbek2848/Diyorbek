package dev.drevss.drevclient.command;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.command.commands.*;
import dev.drevss.drevclient.util.ChatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandManager {
    private final List<Command> commands = new ArrayList<>();
    private String prefix = ".";

    public CommandManager() {
        commands.add(new HelpCommand());
        commands.add(new ToggleCommand());
        commands.add(new BindCommand());
        commands.add(new BindsCommand());
        commands.add(new SettingsCommand());
        commands.add(new FriendCommand());
        commands.add(new MacroCommand());
        commands.add(new GiveCommand());
        commands.add(new EnchantCommand());
        commands.add(new AiCommand());
        commands.add(new ConfigCommand());
        commands.add(new PrefixCommand());
        commands.add(new DrawnCommand());
        commands.add(new HudCommand());
        commands.add(new VClipCommand());
        commands.add(new HClipCommand());
        commands.add(new GuiCommand());
        commands.add(new CoordsCommand());
        commands.add(new PanicCommand());
        commands.add(new ModulesCommand());
        commands.add(new SayCommand());
    }

    public List<Command> all() { return Collections.unmodifiableList(commands); }
    public String getPrefix() { return prefix; }

    public void setPrefix(String prefix) {
        if (prefix != null && !prefix.isBlank() && !prefix.startsWith("/")) this.prefix = prefix.trim();
    }

    public Command get(String name) {
        for (Command c : commands) if (c.matches(name)) return c;
        return null;
    }

    /** Runs "toggle KillAura" style input (no prefix). */
    public void execute(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            ChatUtil.info("Type " + prefix + "help for a list of commands");
            return;
        }
        String[] parts = trimmed.split("\\s+");
        Command cmd = get(parts[0]);
        if (cmd == null) {
            // shortcut: ".killaura" toggles the module
            var module = DrevClient.get().modules().get(parts[0]);
            if (module != null && parts.length == 1) {
                module.toggle();
                DrevClient.get().config().markDirty();
                return;
            }
            ChatUtil.error("Unknown command '" + parts[0] + "'. Try " + prefix + "help");
            return;
        }
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        try {
            cmd.execute(args);
            DrevClient.get().config().markDirty();
        } catch (Command.CommandException e) {
            ChatUtil.error(e.getMessage());
        } catch (Exception e) {
            ChatUtil.error("Command failed: " + e);
            DrevClient.LOGGER.error("Command " + parts[0] + " failed", e);
        }
    }
}

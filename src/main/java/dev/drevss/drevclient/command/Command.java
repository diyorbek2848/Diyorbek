package dev.drevss.drevclient.command;

import net.minecraft.client.Minecraft;

public abstract class Command {
    protected static final Minecraft mc = Minecraft.getInstance();

    private final String name;
    private final String syntax;
    private final String description;
    private final String[] aliases;

    protected Command(String name, String syntax, String description, String... aliases) {
        this.name = name;
        this.syntax = syntax;
        this.description = description;
        this.aliases = aliases;
    }

    /** args excludes the command name. Throw CommandException for usage errors. */
    public abstract void execute(String[] args) throws CommandException;

    public String getName() { return name; }
    public String getSyntax() { return syntax; }
    public String getDescription() { return description; }
    public String[] getAliases() { return aliases; }

    public boolean matches(String input) {
        if (name.equalsIgnoreCase(input)) return true;
        for (String a : aliases) if (a.equalsIgnoreCase(input)) return true;
        return false;
    }

    public static class CommandException extends Exception {
        public CommandException(String message) { super(message); }
    }

    protected CommandException usage() {
        return new CommandException("Usage: " + dev.drevss.drevclient.DrevClient.get().commands().getPrefix() + syntax);
    }

    protected static String join(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }
}

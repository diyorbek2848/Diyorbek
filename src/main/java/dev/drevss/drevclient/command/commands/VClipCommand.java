package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.command.Command;

public class VClipCommand extends Command {
    public VClipCommand() { super("vclip", "vclip <blocks>", "Teleports you up or down"); }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1) throw usage();
        double d;
        try { d = Double.parseDouble(args[0]); } catch (NumberFormatException e) { throw usage(); }
        var p = mc.player;
        if (p.isPassenger() && p.getVehicle() != null) p.getVehicle().setPos(p.getVehicle().getX(), p.getVehicle().getY() + d, p.getVehicle().getZ());
        p.setPos(p.getX(), p.getY() + d, p.getZ());
    }
}

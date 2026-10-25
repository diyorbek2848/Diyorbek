package dev.drevss.drevclient.command.commands;

import dev.drevss.drevclient.command.Command;
import net.minecraft.world.phys.Vec3;

public class HClipCommand extends Command {
    public HClipCommand() { super("hclip", "hclip <blocks>", "Teleports you forward or backward"); }

    @Override
    public void execute(String[] args) throws CommandException {
        if (args.length < 1) throw usage();
        double d;
        try { d = Double.parseDouble(args[0]); } catch (NumberFormatException e) { throw usage(); }
        var p = mc.player;
        Vec3 dir = Vec3.directionFromRotation(0, p.getYRot()).scale(d);
        p.setPos(p.getX() + dir.x, p.getY(), p.getZ() + dir.z);
    }
}

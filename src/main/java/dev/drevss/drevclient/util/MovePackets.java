package dev.drevss.drevclient.util;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/** Rebuilds movement packets with a different on-ground flag. */
public final class MovePackets {
    private MovePackets() { }

    public static ServerboundMovePlayerPacket withOnGround(ServerboundMovePlayerPacket p, boolean onGround) {
        boolean hc = p.horizontalCollision();
        if (p.hasPosition() && p.hasRotation())
            return new ServerboundMovePlayerPacket.PosRot(p.getX(0), p.getY(0), p.getZ(0), p.getYRot(0), p.getXRot(0), onGround, hc);
        if (p.hasPosition())
            return new ServerboundMovePlayerPacket.Pos(p.getX(0), p.getY(0), p.getZ(0), onGround, hc);
        if (p.hasRotation())
            return new ServerboundMovePlayerPacket.Rot(p.getYRot(0), p.getXRot(0), onGround, hc);
        return new ServerboundMovePlayerPacket.StatusOnly(onGround, hc);
    }
}

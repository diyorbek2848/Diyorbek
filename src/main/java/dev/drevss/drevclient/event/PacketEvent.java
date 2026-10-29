package dev.drevss.drevclient.event;

import net.minecraft.network.protocol.Packet;

public class PacketEvent {
    private Packet<?> packet;
    private boolean cancelled;

    public PacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public Packet<?> getPacket() { return packet; }

    /** Replaces an outgoing packet (only has an effect for sent packets). */
    public void setPacket(Packet<?> packet) { this.packet = packet; }

    public boolean isCancelled() { return cancelled; }
    public void cancel() { cancelled = true; }
}

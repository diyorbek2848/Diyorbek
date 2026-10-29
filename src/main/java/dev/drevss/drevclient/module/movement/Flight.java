package dev.drevss.drevclient.module.movement;

import dev.drevss.drevclient.event.PacketEvent;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.MovePackets;
import dev.drevss.drevclient.util.PlayerUtil;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class Flight extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Motion: you control speed directly. Creative: creative-style flying", "Motion", "Motion", "Creative"));
    private final NumberSetting speed = add(new NumberSetting("Speed", "Horizontal speed (blocks per tick)", 1.0, 0.1, 9, 0.1));
    private final NumberSetting vSpeed = add(new NumberSetting("VerticalSpeed", "Up/down speed", 0.6, 0.1, 5, 0.1));
    private final BoolSetting spoofGround = add(new BoolSetting("SpoofGround", "Tell the server you're on the ground: no fly kick, no fall damage", true));
    private final BoolSetting antiKick = add(new BoolSetting("AntiKick", "Dip down a little every 2 seconds (for servers that check)", true));

    private int ticks;

    public Flight() {
        super("Flight", "Lets you fly", Category.MOVEMENT);
    }

    @Override
    public void onEnable() { ticks = 0; }

    @Override
    public void onDisable() {
        if (!inGame()) return;
        if (!mc.player.isCreative() && !mc.player.isSpectator()) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().mayfly = false;
        }
        mc.player.getAbilities().setFlyingSpeed(0.05f);
        Vec3 v = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(v.x * 0.3, Math.min(v.y, 0), v.z * 0.3);
    }

    @Override
    public void onTick() {
        ticks++;
        mc.player.fallDistance = 0;
        if (mode.is("Creative")) {
            mc.player.getAbilities().mayfly = true;
            mc.player.getAbilities().flying = true;
            mc.player.getAbilities().setFlyingSpeed((float) (speed.getValue() * 0.05));
            return;
        }

        mc.player.getAbilities().flying = false;
        double y = 0;
        if (mc.options.keyJump.isDown()) y += vSpeed.getValue();
        if (mc.options.keyShift.isDown()) y -= vSpeed.getValue();
        // vanilla kicks after 80 ticks without moving down; dip every 40 ticks
        if (antiKick.isOn() && ticks % 40 == 0) y = -0.04;

        Vec3 horizontal = PlayerUtil.inputVelocity(speed.getValue());
        mc.player.setDeltaMovement(horizontal.x, y, horizontal.z);
    }

    @Override
    public void onPacketSend(PacketEvent event) {
        if (!spoofGround.isOn() || !(event.getPacket() instanceof ServerboundMovePlayerPacket p) || p.isOnGround()) return;
        event.setPacket(MovePackets.withOnGround(p, true));
    }

    @Override
    public String getInfo() { return mode.get(); }
}

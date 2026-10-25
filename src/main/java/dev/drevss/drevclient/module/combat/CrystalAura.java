package dev.drevss.drevclient.module.combat;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.util.EntityUtil;
import dev.drevss.drevclient.util.InvUtil;
import dev.drevss.drevclient.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class CrystalAura extends Module {
    private final NumberSetting targetRange = add(new NumberSetting("TargetRange", "How far to look for targets", 10, 4, 16, 0.5));
    private final NumberSetting placeRange = add(new NumberSetting("PlaceRange", "Max distance to place crystals", 4.5, 2, 6, 0.1));
    private final NumberSetting breakRange = add(new NumberSetting("BreakRange", "Max distance to break crystals", 4.5, 2, 6, 0.1));
    private final NumberSetting minDamage = add(new NumberSetting("MinDamage", "Minimum damage dealt to the target", 6, 0, 36, 0.5));
    private final NumberSetting maxSelf = add(new NumberSetting("MaxSelfDamage", "Never take more than this", 8, 0, 36, 0.5));
    private final NumberSetting placeDelay = add(new NumberSetting("PlaceDelay", "Ticks between placements", 2, 0, 20, 1));
    private final NumberSetting breakDelay = add(new NumberSetting("BreakDelay", "Ticks between breaks", 2, 0, 20, 1));
    private final BoolSetting autoSwitch = add(new BoolSetting("AutoSwitch", "Switch to crystals in the hotbar", true));
    private final BoolSetting antiSuicide = add(new BoolSetting("AntiSuicide", "Skip anything that could kill you", true));
    private final BoolSetting players = add(new BoolSetting("Players", "Target players", true));
    private final BoolSetting mobs = add(new BoolSetting("Mobs", "Target hostile mobs", false));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", "Send rotations to the server", true));

    private int placeTimer, breakTimer;
    private LivingEntity target;

    public CrystalAura() {
        super("CrystalAura", "Places and detonates end crystals near enemies", Category.COMBAT);
    }

    @Override
    public void onTick() {
        if (placeTimer > 0) placeTimer--;
        if (breakTimer > 0) breakTimer--;

        double tr = targetRange.getValue();
        target = (LivingEntity) EntityUtil.all()
            .filter(e -> EntityUtil.isValidTarget(e, players.isOn(), mobs.isOn(), false, true))
            .filter(e -> e.distanceToSqr(mc.player) <= tr * tr)
            .min(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player)))
            .orElse(null);
        if (target == null) return;

        if (breakTimer == 0) doBreak();
        if (placeTimer == 0) doPlace();
    }

    private void doBreak() {
        double br = breakRange.getValue();
        EndCrystal best = null;
        double bestDmg = 0;
        for (Entity e : (Iterable<Entity>) EntityUtil.all()::iterator) {
            if (!(e instanceof EndCrystal crystal) || !crystal.isAlive()) continue;
            if (mc.player.getEyePosition().distanceTo(crystal.position()) > br) continue;
            double self = damage(crystal.position(), mc.player);
            if (!selfSafe(self)) continue;
            double dmg = damage(crystal.position(), target);
            if (dmg > bestDmg) { bestDmg = dmg; best = crystal; }
        }
        if (best == null || bestDmg < minDamage.getValue() * 0.5) return;
        if (rotate.isOn()) PlayerUtil.lookAt(best.position(), true);
        mc.gameMode.attack(mc.player, best);
        mc.player.swing(InteractionHand.MAIN_HAND);
        breakTimer = breakDelay.getInt();
    }

    private void doPlace() {
        InteractionHand hand;
        int prevSlot = -1;
        if (mc.player.getOffhandItem().is(Items.END_CRYSTAL)) {
            hand = InteractionHand.OFF_HAND;
        } else if (mc.player.getMainHandItem().is(Items.END_CRYSTAL)) {
            hand = InteractionHand.MAIN_HAND;
        } else if (autoSwitch.isOn()) {
            int slot = InvUtil.findHotbar(s -> s.is(Items.END_CRYSTAL));
            if (slot == -1) return;
            prevSlot = InvUtil.selected();
            InvUtil.select(slot);
            hand = InteractionHand.MAIN_HAND;
        } else {
            return;
        }

        BlockPos best = null;
        double bestDmg = minDamage.getValue();
        int r = (int) Math.ceil(placeRange.getValue());
        BlockPos center = mc.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (!canPlaceCrystal(pos)) continue;
            Vec3 crystalPos = Vec3.atBottomCenterOf(pos.above());
            if (mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(pos)) > placeRange.getValue()) continue;
            if (crystalPos.distanceTo(target.position()) > 12) continue;
            double self = damage(crystalPos, mc.player);
            if (!selfSafe(self)) continue;
            double dmg = damage(crystalPos, target);
            if (dmg > bestDmg) { bestDmg = dmg; best = pos.immutable(); }
        }

        if (best != null) {
            Vec3 hitVec = Vec3.atCenterOf(best).add(0, 0.5, 0);
            if (rotate.isOn()) PlayerUtil.lookAt(hitVec, true);
            mc.gameMode.useItemOn(mc.player, hand, new BlockHitResult(hitVec, Direction.UP, best, false));
            mc.player.swing(hand);
            placeTimer = placeDelay.getInt();
        }
        if (prevSlot != -1 && best == null) InvUtil.select(prevSlot);
    }

    private boolean selfSafe(double selfDamage) {
        if (selfDamage > maxSelf.getValue()) return false;
        return !antiSuicide.isOn() || selfDamage < EntityUtil.totalHealth(mc.player) - 1;
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        BlockState base = mc.level.getBlockState(pos);
        if (!base.is(Blocks.OBSIDIAN) && !base.is(Blocks.BEDROCK)) return false;
        BlockPos up = pos.above();
        if (!mc.level.getBlockState(up).isAir() || !mc.level.getBlockState(up.above()).isAir()) return false;
        AABB space = new AABB(up.getX(), up.getY(), up.getZ(), up.getX() + 1, up.getY() + 2, up.getZ() + 1);
        List<Entity> blocking = mc.level.getEntities((Entity) null, space, e -> !e.isSpectator() && e.isAlive());
        return blocking.isEmpty();
    }

    /**
     * Rough explosion damage (power 6) using the vanilla falloff formula with a
     * simple line-of-sight exposure check. Armor is ignored on purpose so the
     * self-damage estimate stays on the safe side.
     */
    private double damage(Vec3 explosion, LivingEntity entity) {
        double dist = entity.position().distanceTo(explosion) / 12.0;
        if (dist > 1) return 0;
        double exposure = exposure(explosion, entity);
        double impact = (1 - dist) * exposure;
        return (impact * impact + impact) / 2.0 * 7.0 * 12.0 + 1.0;
    }

    private double exposure(Vec3 from, Entity entity) {
        AABB box = entity.getBoundingBox();
        int hits = 0, total = 0;
        for (double x = 0; x <= 1; x += 0.5) {
            for (double y = 0; y <= 1; y += 0.5) {
                for (double z = 0; z <= 1; z += 0.5) {
                    Vec3 point = new Vec3(box.minX + (box.maxX - box.minX) * x, box.minY + (box.maxY - box.minY) * y,
                        box.minZ + (box.maxZ - box.minZ) * z);
                    HitResult hit = mc.level.clip(new ClipContext(point, from, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
                    if (hit.getType() == HitResult.Type.MISS) hits++;
                    total++;
                }
            }
        }
        return (double) hits / total;
    }

    @Override
    public String getInfo() { return target != null ? target.getName().getString() : null; }
}

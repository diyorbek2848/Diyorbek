package dev.drevss.drevclient.test;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.Module;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Module tests against a real dedicated server (offline mode, survival, no
 * spawn protection), so server-side checks like fall damage, fly kicks,
 * "moved wrongly" and reach are all real.
 */
public class ServerTests {
    private static final Logger LOG = LoggerFactory.getLogger("DrevClient ServerTest");
    private static final String P = "DrevSS";
    private static final int Y = 200; // platform top surface

    private final ClientGameTestContext ctx;
    private final TestDedicatedServerContext server;
    private final List<String> report;
    private final List<String> errors;

    public ServerTests(ClientGameTestContext ctx, TestDedicatedServerContext server, List<String> report, List<String> errors) {
        this.ctx = ctx;
        this.server = server;
        this.report = report;
        this.errors = errors;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private void cmd(String c) {
        try { server.runCommand(c); } catch (Throwable t) { report.add("Server setup command failed: /" + c + " -> " + t.getMessage()); }
    }

    private <T> T onServer(Function<MinecraftServer, T> f) { return server.computeOnServer(f::apply); }

    private ServerPlayer sp(MinecraftServer s) { return s.getPlayerList().getPlayerByName(P); }

    private Vec3 serverPos() { return onServer(s -> sp(s).position()); }
    private Vec3 clientPos() { return ctx.computeOnClient(mc -> mc.player.position()); }
    private float serverHealth() { return onServer(s -> sp(s).getHealth()); }

    private boolean connected() {
        return ctx.computeOnClient(mc -> mc.player != null && mc.getConnection() != null && mc.level != null);
    }

    private void module(String name, boolean on) {
        ctx.runOnClient(mc -> DrevClient.get().modules().get(name).setEnabled(on));
    }

    private void set(String module, String setting, String value) {
        ctx.runOnClient(mc -> DrevClient.get().modules().get(module).getSetting(setting).parse(value));
    }

    private void check(boolean ok, String what) {
        report.add("Server " + what + ": " + (ok ? "OK" : "FAILED"));
        LOG.info("{} {}", what, ok ? "OK" : "FAILED");
        if (!ok) errors.add("server check failed: " + what);
    }

    private static UUID id(int n) { return new UUID(0L, n); }
    private static String uuidNbt(int n) { return "UUID:[I;0,0,0," + n + "]"; }

    private float entityHealth(int n) {
        return onServer(s -> {
            Entity e = s.overworld().getEntity(id(n));
            return e instanceof LivingEntity le && le.isAlive() ? le.getHealth() : 0f;
        });
    }

    private String fmt(Vec3 v) { return String.format("%.2f %.2f %.2f", v.x, v.y, v.z); }

    /** Turns everything off and puts the player back on the platform, healthy. */
    private void reset(double x, double z, float yaw) {
        ctx.runOnClient(mc -> {
            for (Module m : DrevClient.get().modules().all()) {
                if (m.isEnabled() && m.getCategory() != dev.drevss.drevclient.module.Category.CLIENT
                    && !m.getName().equals("MiddleClickFriend")) m.setEnabledSilently(false);
            }
            mc.options.keyUp.setDown(false);
            mc.options.keyUse.setDown(false);
            mc.options.keyJump.setDown(false);
        });
        cmd("kill @e[type=!player]");
        cmd("clear " + P);
        cmd("effect clear " + P);
        cmd("tp " + P + " " + x + " " + Y + " " + z + " " + yaw + " 0");
        server.runOnServer(s -> {
            ServerPlayer p = sp(s);
            p.setHealth(p.getMaxHealth());
            p.getFoodData().setFoodLevel(20);
        });
        ctx.waitTicks(10);
    }

    private void platform() {
        cmd("fill -25 " + (Y - 1) + " -25 25 " + (Y - 1) + " 25 minecraft:stone");
        cmd("fill -25 " + Y + " -25 25 " + (Y + 30) + " 25 minecraft:air");
    }

    // ------------------------------------------------------------------
    // tests
    // ------------------------------------------------------------------

    public void run() {
        cmd("gamerule doMobSpawning false");
        cmd("gamerule doDaylightCycle false");
        cmd("difficulty normal");
        cmd("gamemode survival " + P);
        cmd("time set noon");
        platform();
        ctx.waitTicks(20);

        test("NoFall", this::noFall);
        test("Flight", this::flight);
        test("Speed", this::speed);
        test("Step", this::step);
        test("Spider", this::spider);
        test("Jesus", this::jesus);
        test("NoSlow", this::noSlow);
        test("Velocity", this::velocity);
        test("KillAura", this::killAura);
        test("Criticals", this::criticals);
        test("Surround", this::surround);
        test("CrystalAura", this::crystalAura);
        test("AutoTotem", this::autoTotem);
        test("BowAimbot", this::bowAimbot);
        test("Freecam", this::freecam);
        test("ElytraFly", this::elytraFly);
        reset(0.5, 0.5, 0);
    }

    private void test(String name, Runnable r) {
        LOG.info("=== server test {}", name);
        try {
            platform();
            r.run();
        } catch (Throwable t) {
            check(false, name + " threw " + t);
        }
        if (!connected()) check(false, name + " got the player disconnected/kicked");
    }

    private void noFall() {
        reset(0.5, 0.5, 0);
        ctx.runOnClient(mc -> mc.player.setDeltaMovement(Vec3.ZERO));
        module("NoFall", true);
        cmd("tp " + P + " 0.5 " + (Y + 30) + " 0.5");
        ctx.waitTicks(5);
        ctx.waitFor(mc -> mc.player.onGround(), 200);
        ctx.waitTicks(10);
        float hp = serverHealth();
        check(hp >= 19.9f, "NoFall: 30 block fall, health " + hp);
    }

    private void flight() {
        reset(0.5, 0.5, -90);
        module("Flight", true);
        ctx.getInput().holdKeyFor(o -> o.keyJump, 20);
        ctx.waitTicks(130); // longer than the 80 tick fly-kick limit
        Vec3 c = clientPos(), s = serverPos();
        check(connected() && c.y > Y + 5 && Math.abs(c.y - s.y) < 1.0,
            "Flight: hovered 6.5 s without kick (client y " + String.format("%.1f", c.y) + ", server y " + String.format("%.1f", s.y) + ")");
        ctx.getInput().holdKeyFor(o -> o.keyUp, 15);
        ctx.waitTicks(10);
        Vec3 c2 = clientPos(), s2 = serverPos();
        check(c2.x - c.x > 8 && c2.distanceTo(s2) < 1.0, "Flight: flew forward " + String.format("%.1f", c2.x - c.x) + " blocks, server agrees");
        // land safely with the flight's ground spoofing
        module("Flight", false);
        module("NoFall", true);
        ctx.waitFor(mc -> mc.player.onGround(), 200);
        ctx.waitTicks(10);
        check(serverHealth() >= 19.9f, "Flight: landing after flight without damage");
    }

    private void speed() {
        reset(-20.5, 0.5, -90);
        module("Speed", true);
        Vec3 a = clientPos();
        ctx.getInput().holdKeyFor(o -> o.keyUp, 30);
        ctx.waitTicks(15);
        Vec3 b = clientPos(), s = serverPos();
        double moved = b.x - a.x;
        check(moved > 10 && b.distanceTo(s) < 1.0, "Speed: moved " + String.format("%.1f", moved) + " blocks in 1.5 s, server " + fmt(s));
    }

    private void step() {
        reset(0.5, 0.5, -90);
        cmd("fill 3 " + Y + " -3 3 " + Y + " 3 minecraft:stone");
        module("Step", true);
        ctx.getInput().holdKeyFor(o -> o.keyUp, 25);
        ctx.waitTicks(10);
        Vec3 c = clientPos(), s = serverPos();
        check(c.y >= Y + 0.99 && s.y >= Y + 0.99 && s.x > 3, "Step: walked up a full block (server " + fmt(s) + ")");
    }

    private void spider() {
        reset(0.5, 0.5, -90);
        cmd("fill 2 " + Y + " -3 2 " + (Y + 20) + " 3 minecraft:stone");
        module("Spider", true);
        ctx.getInput().holdKeyFor(o -> o.keyUp, 50);
        Vec3 c = clientPos(), s = serverPos();
        check(c.y > Y + 4 && Math.abs(c.y - s.y) < 1.0, "Spider: climbed to y " + String.format("%.1f", s.y));
        module("Spider", false);
        module("NoFall", true);
        ctx.waitFor(mc -> mc.player.onGround(), 200);
    }

    private void jesus() {
        reset(4.5, 0.5, -90);
        cmd("fill 6 " + (Y - 1) + " -4 20 " + (Y - 1) + " 4 minecraft:water");
        cmd("fill 6 " + (Y - 3) + " -4 20 " + (Y - 2) + " 4 minecraft:water");
        ctx.waitTicks(5);
        module("Jesus", true);
        ctx.getInput().holdKeyFor(o -> o.keyUp, 30);
        ctx.waitTicks(10);
        Vec3 c = clientPos(), s = serverPos();
        boolean inWater = ctx.computeOnClient(mc -> mc.player.isInWater());
        check(c.x > 9 && c.y > Y - 0.2 && !inWater && Math.abs(c.y - s.y) < 0.6, "Jesus: walking on water at " + fmt(c));
        cmd("fill 6 " + (Y - 3) + " -4 20 " + (Y - 1) + " 4 minecraft:stone");
    }

    private void noSlow() {
        reset(-20.5, 0.5, -90);
        cmd("item replace entity " + P + " weapon.offhand with minecraft:shield");
        ctx.waitTicks(5);
        module("NoSlow", true);
        Vec3 a = clientPos();
        ctx.runOnClient(mc -> mc.options.keyUse.setDown(true));
        ctx.getInput().holdKeyFor(o -> o.keyUp, 40);
        boolean using = ctx.computeOnClient(mc -> mc.player.isUsingItem());
        ctx.runOnClient(mc -> mc.options.keyUse.setDown(false));
        Vec3 b = clientPos();
        double moved = b.x - a.x;
        check(using && moved > 6, "NoSlow: walked " + String.format("%.1f", moved) + " blocks while blocking with a shield");
    }

    private void velocity() {
        reset(0.5, 0.5, 0);
        cmd("summon minecraft:zombie 0.5 " + Y + " 3.5 {NoAI:1b,Silent:1b," + uuidNbt(11) + "}");
        module("Velocity", true);
        ctx.waitTicks(10);
        Vec3 a = clientPos();
        cmd("damage " + P + " 1 minecraft:mob_attack by 00000000-0000-0000-0000-00000000000b");
        ctx.waitTicks(15);
        Vec3 b = clientPos();
        double moved = Math.sqrt((b.x - a.x) * (b.x - a.x) + (b.z - a.z) * (b.z - a.z));
        check(moved < 0.15, "Velocity: knockback moved the player " + String.format("%.2f", moved) + " blocks");
    }

    private void killAura() {
        reset(0.5, 0.5, 0);
        cmd("item replace entity " + P + " weapon.mainhand with minecraft:diamond_sword");
        cmd("summon minecraft:zombie 0.5 " + Y + " 3 {NoAI:1b,Silent:1b,PersistenceRequired:1b," + uuidNbt(21) + "}");
        ctx.waitTicks(10);
        module("KillAura", true);
        ctx.waitTicks(80);
        float hp = entityHealth(21);
        check(hp < 1f, "KillAura: zombie health on the server " + hp);
    }

    private void criticals() {
        reset(0.5, 0.5, 0);
        cmd("item replace entity " + P + " weapon.mainhand with minecraft:diamond_sword");
        cmd("summon minecraft:zombie 0.5 " + Y + " 2.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b," + uuidNbt(31) + "}");
        ctx.waitTicks(30);
        module("Criticals", true);
        ctx.runOnClient(mc -> {
            Entity target = null;
            for (Entity e : mc.level.entitiesForRendering()) if (e.getUUID().equals(id(31))) target = e;
            if (target != null) mc.gameMode.attack(mc.player, target);
        });
        ctx.waitTicks(10);
        float hp = entityHealth(31);
        // diamond sword = 7 damage, a crit does 10.5
        check(hp > 0 && hp <= 10f, "Criticals: one hit left the zombie at " + hp + " (crit leaves 9.5, normal 13)");
    }

    private void surround() {
        reset(0.5, 0.5, 0);
        cmd("give " + P + " minecraft:obsidian 16");
        ctx.waitTicks(5);
        module("Surround", true);
        ctx.waitTicks(30);
        boolean ok = onServer(s -> {
            var lvl = s.overworld();
            return lvl.getBlockState(new BlockPos(1, Y, 0)).is(Blocks.OBSIDIAN) && lvl.getBlockState(new BlockPos(-1, Y, 0)).is(Blocks.OBSIDIAN)
                && lvl.getBlockState(new BlockPos(0, Y, 1)).is(Blocks.OBSIDIAN) && lvl.getBlockState(new BlockPos(0, Y, -1)).is(Blocks.OBSIDIAN);
        });
        check(ok, "Surround: obsidian placed on all 4 sides (server)");
        module("Surround", false);
        cmd("fill -1 " + Y + " -1 1 " + Y + " 1 minecraft:air");
    }

    private void crystalAura() {
        reset(0.5, 0.5, 0);
        cmd("effect give " + P + " minecraft:resistance 60 4 true");
        cmd("give " + P + " minecraft:end_crystal 16");
        cmd("fill -1 " + (Y - 1) + " 3 1 " + (Y - 1) + " 5 minecraft:obsidian");
        cmd("summon minecraft:zombie 0.5 " + Y + " 5.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b," + uuidNbt(41) + "}");
        ctx.waitTicks(15);
        set("CrystalAura", "Mobs", "on");
        module("CrystalAura", true);
        ctx.waitTicks(80);
        float hp = entityHealth(41);
        float me = serverHealth();
        check(hp < 1f && me > 0, "CrystalAura: zombie health " + hp + ", your health " + me);
        set("CrystalAura", "Mobs", "off");
        module("CrystalAura", false);
        cmd("fill -1 " + (Y - 1) + " 3 1 " + (Y - 1) + " 5 minecraft:stone");
    }

    private void autoTotem() {
        reset(0.5, 0.5, 0);
        cmd("item replace entity " + P + " hotbar.5 with minecraft:totem_of_undying");
        ctx.waitTicks(5);
        module("AutoTotem", true);
        ctx.waitTicks(20);
        boolean ok = onServer(s -> sp(s).getOffhandItem().is(Items.TOTEM_OF_UNDYING));
        check(ok, "AutoTotem: totem in offhand on the server");
    }

    private void bowAimbot() {
        reset(0.5, 0.5, 180); // facing away from the target
        cmd("item replace entity " + P + " weapon.mainhand with minecraft:bow");
        cmd("give " + P + " minecraft:arrow 16");
        cmd("summon minecraft:zombie 0.5 " + Y + " 14.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b," + uuidNbt(51) + "}");
        ctx.waitTicks(15);
        module("BowAimbot", true);
        ctx.runOnClient(mc -> mc.options.keyUse.setDown(true));
        ctx.waitTicks(25);
        ctx.runOnClient(mc -> mc.options.keyUse.setDown(false));
        ctx.waitTicks(40);
        float hp = entityHealth(51);
        check(hp < 20f, "BowAimbot: arrow hit a zombie 14 blocks behind you (health " + hp + ")");
    }

    private void freecam() {
        reset(0.5, 0.5, -90);
        Vec3 before = serverPos();
        module("Freecam", true);
        ctx.waitTicks(5);
        ctx.getInput().holdKeyFor(o -> o.keyUp, 20);
        ctx.waitTicks(5);
        Vec3 cam = ctx.computeOnClient(mc -> mc.gameRenderer.getMainCamera().position());
        Vec3 c = clientPos(), s = serverPos();
        ctx.takeScreenshot("server_freecam");
        check(cam.x - c.x > 3 && c.distanceTo(before) < 0.2 && s.distanceTo(before) < 0.2,
            "Freecam: camera flew " + String.format("%.1f", cam.x - c.x) + " blocks, body stayed put");
        module("Freecam", false);
    }

    private void elytraFly() {
        reset(0.5, 0.5, -90);
        cmd("item replace entity " + P + " armor.chest with minecraft:elytra");
        cmd("tp " + P + " 0.5 " + (Y + 25) + " 0.5 -90 0");
        ctx.waitTicks(5);
        module("ElytraFly", true);
        module("NoFall", true);
        ctx.waitTicks(4);
        ctx.getInput().holdKeyFor(o -> o.keyJump, 3);
        ctx.waitTicks(3);
        ctx.getInput().holdKeyFor(o -> o.keyJump, 3);
        boolean flying = ctx.computeOnClient(mc -> mc.player.isFallFlying());
        Vec3 a = clientPos();
        ctx.getInput().holdKeyFor(o -> o.keyUp, 15);
        Vec3 b = clientPos(), s = serverPos();
        check(flying && b.x - a.x > 10 && b.distanceTo(s) < 2, "ElytraFly: flew " + String.format("%.1f", b.x - a.x) + " blocks (gliding " + flying + ")");
        module("ElytraFly", false);
        ctx.waitFor(mc -> mc.player.onGround(), 200);
    }
}

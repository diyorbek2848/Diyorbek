package dev.drevss.drevclient.test;

import com.mojang.brigadier.ParseResults;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.gui.ClickGuiScreen;
import dev.drevss.drevclient.gui.DrevAIScreen;
import dev.drevss.drevclient.gui.HudEditorScreen;
import dev.drevss.drevclient.hud.HudElement;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.module.ModuleManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * End-to-end test that runs in a real Minecraft client on GitHub Actions:
 * creates a world, turns every module on and off, opens every screen,
 * runs every command and takes screenshots. Any exception fails the build.
 */
public class DrevClientTest implements FabricClientGameTest {
    private static final Logger LOG = LoggerFactory.getLogger("DrevClient Test");
    private static final Set<String> SKIP = Set.of("ClickGUI", "DrevAI", "Panic", "Client");
    private final List<String> report = new ArrayList<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(mc -> mc.options.hideSplashTexts().set(true));
        context.waitFor(mc -> mc.screen instanceof TitleScreen t && !t.fading);
        LOG.info("Title screen reached");

        TestWorldBuilder builder = context.worldBuilder();
        builder.adjustSettings(c -> {
            c.setName("DrevClient Test");
            c.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        });

        try (TestSingleplayerContext sp = builder.create()) {
            TestServerContext server = sp.getServer();
            sp.getClientWorld().waitForChunksRender();

            LOG.info("Auditing mixins");
            MixinEnvironment.getCurrentEnvironment().audit();
            report.add("Mixin audit: OK");

            setupScene(context, server);
            sp.getClientWorld().waitForChunksRender();
            context.takeScreenshot("01_in_game_hud");

            testModules(context);
            testCommands(context);
            testRenderShowcase(context, sp);
            testScreens(context);
        }

        writeReport();
        if (!ModuleManager.ERRORS.isEmpty()) {
            throw new AssertionError("DrevClient errors:\n" + String.join("\n", ModuleManager.ERRORS));
        }
        LOG.info("All DrevClient tests passed");
    }

    private void setupScene(ClientGameTestContext context, TestServerContext server) {
        run(server, "time set noon");
        run(server, "weather clear");
        run(server, "gamerule doMobSpawning false");
        run(server, "tp @p 0 -60 0 0 20");
        run(server, "fill -6 -61 3 6 -57 12 minecraft:air");
        run(server, "setblock 2 -60 6 minecraft:chest");
        run(server, "setblock 3 -60 6 minecraft:ender_chest");
        run(server, "setblock -2 -60 6 minecraft:shulker_box");
        run(server, "setblock -3 -60 6 minecraft:barrel");
        run(server, "setblock 0 -62 4 minecraft:diamond_ore");
        run(server, "setblock 1 -62 4 minecraft:gold_ore");
        run(server, "setblock -1 -62 4 minecraft:iron_ore");
        run(server, "setblock 0 -60 10 minecraft:spawner");
        run(server, "summon minecraft:zombie 2 -60 8 {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
        run(server, "summon minecraft:pig -2 -60 8 {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
        run(server, "summon minecraft:skeleton 0 -60 11 {NoAI:1b,PersistenceRequired:1b,Silent:1b}");
        context.waitTicks(20);
    }

    private void testModules(ClientGameTestContext context) {
        List<String> names = context.computeOnClient(mc -> {
            List<String> list = new ArrayList<>();
            for (Module m : DrevClient.get().modules().all()) list.add(m.getName());
            return list;
        });
        for (String name : names) {
            if (SKIP.contains(name)) continue;
            int before = ModuleManager.ERRORS.size();
            try {
                context.runOnClient(mc -> DrevClient.get().modules().get(name).setEnabled(true));
                context.waitTicks(25);
                context.runOnClient(mc -> DrevClient.get().modules().get(name).setEnabled(false));
                context.waitTicks(2);
            } catch (Throwable t) {
                ModuleManager.ERRORS.add(name + " (enable/disable): " + t);
            }
            boolean ok = ModuleManager.ERRORS.size() == before;
            report.add("Module " + name + ": " + (ok ? "OK" : "FAILED"));
            LOG.info("Module {} {}", name, ok ? "OK" : "FAILED");
        }
        // put the player back where the scene is
        context.runOnClient(mc -> DrevClient.get().commands().execute("vclip 0"));
    }

    private void testCommands(ClientGameTestContext context) {
        String[] commands = {
            "help", "help bind", "modules", "binds",
            "toggle Sprint on", "toggle Sprint off", "sprint", "sprint",
            "bind Flight F", "bind Flight none", "bind Zoom C hold",
            "settings KillAura", "settings KillAura Range 5", "settings KillAura Range reset",
            "settings ESP Mode Both", "settings ESP PlayerColor #FF00FF", "settings ESP PlayerColor rainbow",
            "friend add Notch", "friend list", "friend remove Notch",
            "macro add K say hi; .help", "macro list", "macro remove K",
            "give diamond_sword", "give minecraft:golden_apple 16", "enchant sharpness 5", "enchant all",
            "config save citest", "config load citest", "prefix .",
            "drawn ESP", "drawn ESP", "hud list", "hud toggle Compass", "hud toggle Compass",
            "vclip 0", "hclip 0", "coords", "gui desync", "ai reset", "ai model claude-haiku-5-5",
            "ai model claude-sonnet-5-5"
        };
        for (String c : commands) {
            int before = ModuleManager.ERRORS.size();
            context.runOnClient(mc -> DrevClient.get().commands().execute(c));
            context.waitTick();
            report.add("Command ." + c + ": " + (ModuleManager.ERRORS.size() == before ? "OK" : "FAILED"));
        }
    }

    private void testRenderShowcase(ClientGameTestContext context, TestSingleplayerContext sp) {
        String[] showcase = { "ESP", "Tracers", "Nametags", "StorageESP", "BlockESP", "XRay" };
        run(sp.getServer(), "tp @p 0 -60 0 0 20");
        context.runOnClient(mc -> {
            for (HudElement e : DrevClient.get().hud().elements()) e.setVisible(true);
            for (String s : showcase) DrevClient.get().modules().get(s).setEnabled(true);
            DrevClient.get().modules().get("ESP").getSetting("Mode").parse("Box");
        });
        context.waitTicks(40);
        sp.getClientWorld().waitForChunksRender();
        context.takeScreenshot("02_render_modules");

        context.runOnClient(mc -> DrevClient.get().modules().get("ESP").getSetting("Mode").parse("Glow"));
        context.waitTicks(5);
        context.takeScreenshot("03_esp_glow");

        context.runOnClient(mc -> DrevClient.get().modules().get("Freecam").setEnabled(true));
        context.waitTicks(5);
        context.takeScreenshot("04_freecam");
        context.runOnClient(mc -> {
            DrevClient.get().modules().get("Freecam").setEnabled(false);
            for (String s : showcase) DrevClient.get().modules().get(s).setEnabled(false);
        });
        context.waitTicks(5);
    }

    private void testScreens(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            ClickGuiScreen.expand("KillAura");
            ClickGuiScreen.expand("ESP");
            mc.setScreen(new ClickGuiScreen());
        });
        context.waitTicks(5);
        context.takeScreenshot("05_clickgui");

        context.runOnClient(mc -> mc.setScreen(new HudEditorScreen(null)));
        context.waitTicks(5);
        context.takeScreenshot("06_hud_editor");

        context.runOnClient(mc -> {
            DrevClient.get().ai().ask("What does KillAura do?", a -> { });
            mc.setScreen(new DrevAIScreen());
        });
        context.waitTicks(5);
        context.takeScreenshot("07_drevai");

        context.runOnClient(mc -> mc.setScreen(null));
        context.waitTicks(2);
        report.add("Screens: OK");
    }

    private void run(TestServerContext server, String command) {
        server.runOnServer(s -> {
            ParseResults<CommandSourceStack> results = s.getCommands().getDispatcher().parse(command, s.createCommandSourceStack());
            if (!results.getExceptions().isEmpty()) {
                report.add("Setup command skipped: /" + command);
                return;
            }
            s.getCommands().performCommand(results, command);
        });
    }

    private void writeReport() {
        try {
            List<String> lines = new ArrayList<>(report);
            lines.add("");
            lines.add("Errors: " + ModuleManager.ERRORS.size());
            lines.addAll(ModuleManager.ERRORS);
            Files.write(FabricLoader.getInstance().getGameDir().resolve("drevclient-test-report.txt"), lines);
        } catch (IOException e) {
            LOG.error("Could not write report", e);
        }
    }
}

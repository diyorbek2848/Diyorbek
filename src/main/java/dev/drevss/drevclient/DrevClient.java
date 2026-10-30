package dev.drevss.drevclient;

import dev.drevss.drevclient.ai.DrevAI;
import dev.drevss.drevclient.command.CommandManager;
import dev.drevss.drevclient.hud.Hud;
import dev.drevss.drevclient.manager.ConfigManager;
import dev.drevss.drevclient.manager.FriendManager;
import dev.drevss.drevclient.manager.MacroManager;
import dev.drevss.drevclient.module.ModuleManager;
import dev.drevss.drevclient.module.misc.AutoReconnect;
import dev.drevss.drevclient.module.player.GuiUtils;
import dev.drevss.drevclient.module.player.InventoryTweaks;
import dev.drevss.drevclient.util.DrevRender;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DrevClient implements ClientModInitializer {
    public static final String NAME = "DrevClient";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static DrevClient instance;

    private ModuleManager modules;
    private CommandManager commands;
    private FriendManager friends;
    private MacroManager macros;
    private ConfigManager config;
    private Hud hud;
    private DrevAI ai;
    private dev.drevss.drevclient.skin.SkinManager skins;

    public static DrevClient get() { return instance; }

    @Override
    public void onInitializeClient() {
        instance = this;
        DrevRender.init();

        friends = new FriendManager();
        macros = new MacroManager();
        config = new ConfigManager();
        commands = new CommandManager();
        modules = new ModuleManager();
        hud = new Hud();
        ai = new DrevAI();
        skins = new dev.drevss.drevclient.skin.SkinManager();
        config.load("default");

        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            config.tick();
            modules.get(AutoReconnect.class).tickAlways();
            if (mc.player != null && mc.level != null) modules.onTick();
        });

        HudElementRegistry.addLast(Identifier.parse("drevclient:hud"), (graphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) return;
            float pt = tickCounter.getGameTimeDeltaPartialTick(true);
            modules.onRender2D(graphics, pt);
            hud.render(graphics, pt);
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (message.startsWith(commands.getPrefix())) {
                if (!message.toLowerCase().contains(" key ")) Minecraft.getInstance().gui.getChat().addRecentChat(message);
                commands.execute(message.substring(commands.getPrefix().length()));
                return false;
            }
            return true;
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) ->
            modules.get(AutoReconnect.class).rememberServer(mc.getCurrentServer()));

        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            modules.get(GuiUtils.class).onScreenInit(screen);
            modules.get(InventoryTweaks.class).onScreenInit(screen);
            modules.get(AutoReconnect.class).onScreenInit(screen);
        });

        ClientLifecycleEvents.CLIENT_STARTED.register(mc -> skins.load());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) ->
            mc.execute(() -> modules.get(dev.drevss.drevclient.module.render.Freecam.class).setEnabledSilently(false)));

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> config.save("default"));

        LOGGER.info("{} {} loaded with {} modules", NAME, VERSION, modules.all().size());
    }

    public ModuleManager modules() { return modules; }
    public CommandManager commands() { return commands; }
    public FriendManager friends() { return friends; }
    public MacroManager macros() { return macros; }
    public ConfigManager config() { return config; }
    public Hud hud() { return hud; }
    public DrevAI ai() { return ai; }
    public dev.drevss.drevclient.skin.SkinManager skins() { return skins; }
}

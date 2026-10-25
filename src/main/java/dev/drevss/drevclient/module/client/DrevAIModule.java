package dev.drevss.drevclient.module.client;

import dev.drevss.drevclient.gui.DrevAIScreen;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.setting.TextSetting;
import org.lwjgl.glfw.GLFW;

public class DrevAIModule extends Module {
    public final TextSetting apiKey = add(new TextSetting("ApiKey", "Anthropic API key (console.anthropic.com). Set with .ai key <key>", ""));
    public final ModeSetting model = add(new ModeSetting("Model", "Claude model to use", "claude-sonnet-5-5",
        "claude-sonnet-5-5", "claude-opus-5-5", "claude-haiku-5-5"));
    public final BoolSetting webSearch = add(new BoolSetting("WebSearch", "Let DrevAI search the internet", true));
    public final BoolSetting gameContext = add(new BoolSetting("GameContext", "Share your coords, dimension and enabled modules", true));

    public DrevAIModule() {
        super("DrevAI", "AI assistant that knows DrevClient and can search the web (key: J)", Category.CLIENT);
        setKey(GLFW.GLFW_KEY_J);
        setDrawn(false);
    }

    @Override
    public void onEnable() {
        // open on the next frame so the key press that opened it doesn't also reach the new screen
        if (mc.player != null) mc.execute(() -> mc.setScreen(new DrevAIScreen()));
        setEnabledSilently(false);
    }

    @Override
    protected boolean showToggleMessage() { return false; }
}

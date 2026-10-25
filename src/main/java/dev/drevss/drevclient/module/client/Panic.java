package dev.drevss.drevclient.module.client;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.util.ChatUtil;

public class Panic extends Module {
    public Panic() {
        super("Panic", "Turns every module off at once", Category.CLIENT);
        setDrawn(false);
    }

    @Override
    public void onEnable() {
        DrevClient.get().modules().disableAll();
        setEnabledSilently(false);
        ChatUtil.info("All modules disabled");
    }

    @Override
    protected boolean showToggleMessage() { return false; }
}

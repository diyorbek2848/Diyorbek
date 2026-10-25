package dev.drevss.drevclient.module.client;

import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.BoolSetting;

/** Global options. It can't be toggled; its settings always apply. */
public class ClientSettings extends Module {
    public final BoolSetting toggleMessages = add(new BoolSetting("ToggleMessages", "Chat message when a module turns on/off", true));
    public final BoolSetting customMenu = add(new BoolSetting("CustomMenu", "Use the DrevClient main menu instead of the vanilla one", true));
    public final BoolSetting autoSave = add(new BoolSetting("AutoSave", "Save the config automatically", true));

    public ClientSettings() {
        super("Client", "Global DrevClient options", Category.CLIENT);
        setDrawn(false);
    }

    @Override
    public void setEnabled(boolean value) { /* always on */ }

    @Override
    public boolean isEnabled() { return true; }

    @Override
    protected boolean showToggleMessage() { return false; }
}

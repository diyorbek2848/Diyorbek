package dev.drevss.drevclient.module;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.event.PacketEvent;
import dev.drevss.drevclient.setting.Setting;
import dev.drevss.drevclient.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final Minecraft mc = Minecraft.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;
    private int key = -1;           // GLFW key code, -1 = unbound
    private boolean holdMode;       // enabled only while the key is held
    private boolean drawn = true;   // shown in the HUD module list

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (enabled) {
            onEnable();
            if (showToggleMessage()) ChatUtil.toggle(name, true);
        } else {
            onDisable();
            if (showToggleMessage()) ChatUtil.toggle(name, false);
        }
    }

    /** Silent toggle used when loading the config. */
    public void setEnabledSilently(boolean value) {
        if (value == enabled) return;
        enabled = value;
        if (enabled) onEnable(); else onDisable();
    }

    protected boolean showToggleMessage() {
        return dev.drevss.drevclient.DrevClient.get().modules().clientSettings().toggleMessages.isOn();
    }

    protected boolean inGame() { return mc.player != null && mc.level != null; }

    // --- hooks, all optional ---
    public void onEnable() { }
    public void onDisable() { }
    public void onTick() { }
    public void onRender3D(PoseStack matrices, float partialTicks) { }
    public void onRender2D(GuiGraphics graphics, float partialTicks) { }
    public void onPacketSend(PacketEvent event) { }
    public void onPacketReceive(PacketEvent event) { }
    /** Called right before the player attacks an entity (KillAura or a normal click). */
    public void onAttack(Entity target) { }

    /** Extra info shown next to the name in the HUD module list. */
    public String getInfo() { return null; }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public List<Setting<?>> getSettings() { return settings; }
    public boolean isEnabled() { return enabled; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }
    public boolean isHoldMode() { return holdMode; }
    public void setHoldMode(boolean holdMode) { this.holdMode = holdMode; }
    public boolean isDrawn() { return drawn; }
    public void setDrawn(boolean drawn) { this.drawn = drawn; }

    public Setting<?> getSetting(String settingName) {
        String target = settingName.replace(" ", "").replace("_", "");
        for (Setting<?> s : settings) {
            if (s.getName().replace(" ", "").equalsIgnoreCase(target)) return s;
        }
        return null;
    }
}

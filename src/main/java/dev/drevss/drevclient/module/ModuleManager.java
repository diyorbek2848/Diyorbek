package dev.drevss.drevclient.module;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.event.PacketEvent;
import dev.drevss.drevclient.module.client.ClickGuiModule;
import dev.drevss.drevclient.module.client.ClientSettings;
import dev.drevss.drevclient.module.client.DrevAIModule;
import dev.drevss.drevclient.module.client.HudModule;
import dev.drevss.drevclient.module.client.Panic;
import dev.drevss.drevclient.module.combat.*;
import dev.drevss.drevclient.module.misc.*;
import dev.drevss.drevclient.module.movement.*;
import dev.drevss.drevclient.module.player.*;
import dev.drevss.drevclient.module.render.*;
import dev.drevss.drevclient.module.world.*;
import dev.drevss.drevclient.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final java.util.Map<Class<?>, Module> byClass = new java.util.HashMap<>();

    public ModuleManager() {
        // Combat
        add(new KillAura());
        add(new CrystalAura());
        add(new AutoTotem());
        add(new AutoArmor());
        add(new Criticals());
        add(new Surround());
        add(new BowAimbot());
        add(new AutoWeapon());
        add(new Velocity());
        // Player
        add(new AutoEat());
        add(new AutoTool());
        add(new AutoReplenish());
        add(new FastUse());
        add(new NoFall());
        add(new AutoRespawn());
        add(new InventoryTweaks());
        add(new Reach());
        add(new GuiUtils());
        // Movement
        add(new Flight());
        add(new Speed());
        add(new Sprint());
        add(new NoSlow());
        add(new Step());
        add(new Jesus());
        add(new ElytraFly());
        add(new Spider());
        add(new AutoWalk());
        // Render
        add(new ESP());
        add(new Tracers());
        add(new Fullbright());
        add(new Freecam());
        add(new XRay());
        add(new Nametags());
        add(new StorageESP());
        add(new BlockESP());
        add(new Zoom());
        add(new Trajectories());
        add(new NoRender());
        // World
        add(new Nuker());
        add(new Scaffold());
        add(new AutoFarm());
        add(new LiquidFiller());
        add(new Timer());
        add(new StashFinder());
        add(new InfinityMiner());
        // Misc
        add(new AutoReconnect());
        add(new Notifier());
        add(new Spam());
        add(new DiscordPresence());
        add(new BetterChat());
        add(new SoundBlocker());
        add(new MiddleClickFriend());
        // Client
        add(new ClickGuiModule());
        add(new HudModule());
        add(new ClientSettings());
        add(new DrevAIModule());
        add(new Panic());
    }

    private void add(Module m) {
        modules.add(m);
        byClass.put(m.getClass(), m);
    }

    public List<Module> all() { return Collections.unmodifiableList(modules); }

    public List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.getCategory() == c) out.add(m);
        return out;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> type) {
        Module cached = byClass.get(type);
        if (cached != null) return (T) cached;
        for (Module m : modules) if (type.isInstance(m)) return (T) m;
        throw new IllegalArgumentException("Unknown module " + type.getSimpleName());
    }

    public Module get(String name) {
        String n = name.replace(" ", "");
        for (Module m : modules) if (m.getName().replace(" ", "").equalsIgnoreCase(n)) return m;
        return null;
    }

    public ClientSettings clientSettings() { return get(ClientSettings.class); }

    // ------------------------------------------------------------------
    // Dispatch. Every call is guarded so one broken module can't crash the game.
    // ------------------------------------------------------------------

    private void forEnabled(Consumer<Module> action) {
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            try {
                action.accept(m);
            } catch (Throwable t) {
                DrevClient.LOGGER.error("Module " + m.getName() + " crashed", t);
                m.setEnabledSilently(false);
                ChatUtil.error(m.getName() + " hit an error and was turned off: " + t);
            }
        }
    }

    public void onTick() { forEnabled(Module::onTick); }
    public void onRender3D(PoseStack matrices, float pt) { forEnabled(m -> m.onRender3D(matrices, pt)); }
    public void onRender2D(GuiGraphics graphics, float pt) { forEnabled(m -> m.onRender2D(graphics, pt)); }
    public void onPacketSend(PacketEvent e) { forEnabled(m -> m.onPacketSend(e)); }
    public void onPacketReceive(PacketEvent e) { forEnabled(m -> m.onPacketReceive(e)); }
    public void onAttack(Entity target) { forEnabled(m -> m.onAttack(target)); }

    public void onKey(int key, int action) {
        Minecraft mc = Minecraft.getInstance();
        if (key == GLFW.GLFW_KEY_UNKNOWN) return;
        if (action == GLFW.GLFW_PRESS) {
            if (mc.screen != null) return;
            for (Module m : modules) {
                if (m.getKey() != key) continue;
                if (m.isHoldMode()) m.setEnabled(true);
                else m.toggle();
                DrevClient.get().config().markDirty();
            }
            DrevClient.get().macros().onKey(key);
        } else if (action == GLFW.GLFW_RELEASE) {
            for (Module m : modules) {
                if (m.getKey() == key && m.isHoldMode() && m.isEnabled()) m.setEnabled(false);
            }
        }
    }

    public void disableAll() {
        for (Module m : modules) {
            if (m instanceof HudModule) continue;
            if (m.isEnabled()) m.setEnabled(false);
        }
    }
}

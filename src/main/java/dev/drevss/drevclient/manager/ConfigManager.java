package dev.drevss.drevclient.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.gui.ClickGuiScreen;
import dev.drevss.drevclient.hud.HudElement;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Saves everything to .minecraft/config/drevclient/<name>.json */
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path dir = FabricLoader.getInstance().getConfigDir().resolve("drevclient");
    private boolean dirty;
    private int saveTimer;

    public Path getDir() {
        try { Files.createDirectories(dir); } catch (IOException ignored) { }
        return dir;
    }

    public void markDirty() { dirty = true; }

    /** Called every client tick; writes at most every 10 s when something changed. */
    public void tick() {
        if (++saveTimer >= 200) {
            saveTimer = 0;
            if (dirty) save("default");
        }
    }

    public boolean save(String name) {
        DrevClient c = DrevClient.get();
        JsonObject root = new JsonObject();
        root.addProperty("prefix", c.commands().getPrefix());

        JsonObject modules = new JsonObject();
        for (Module m : c.modules().all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            o.addProperty("key", m.getKey());
            o.addProperty("hold", m.isHoldMode());
            o.addProperty("drawn", m.isDrawn());
            JsonObject settings = new JsonObject();
            for (Setting<?> s : m.getSettings()) settings.add(s.getName(), s.toJson());
            o.add("settings", settings);
            modules.add(m.getName(), o);
        }
        root.add("modules", modules);

        JsonArray friends = new JsonArray();
        c.friends().all().forEach(friends::add);
        root.add("friends", friends);

        JsonObject macros = new JsonObject();
        for (Map.Entry<Integer, String> e : c.macros().all().entrySet()) macros.addProperty(String.valueOf(e.getKey()), e.getValue());
        root.add("macros", macros);

        JsonObject hud = new JsonObject();
        for (HudElement e : c.hud().elements()) {
            JsonObject o = new JsonObject();
            o.addProperty("x", e.getX());
            o.addProperty("y", e.getY());
            o.addProperty("visible", e.isVisible());
            hud.add(e.getName(), o);
        }
        root.add("hud", hud);
        root.add("clickgui", ClickGuiScreen.savePanels());

        try {
            Files.writeString(getDir().resolve(name + ".json"), GSON.toJson(root), StandardCharsets.UTF_8);
            if (name.equals("default")) dirty = false;
            return true;
        } catch (IOException e) {
            DrevClient.LOGGER.error("Could not save config", e);
            return false;
        }
    }

    public boolean load(String name) {
        Path file = getDir().resolve(name + ".json");
        if (!Files.exists(file)) return false;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            DrevClient c = DrevClient.get();
            if (root.has("prefix")) c.commands().setPrefix(root.get("prefix").getAsString());

            if (root.has("modules")) {
                JsonObject modules = root.getAsJsonObject("modules");
                for (Module m : c.modules().all()) {
                    if (!modules.has(m.getName())) continue;
                    JsonObject o = modules.getAsJsonObject(m.getName());
                    if (o.has("settings")) {
                        JsonObject settings = o.getAsJsonObject("settings");
                        for (Setting<?> s : m.getSettings()) {
                            if (settings.has(s.getName())) s.fromJson(settings.get(s.getName()));
                        }
                    }
                    if (o.has("key")) m.setKey(o.get("key").getAsInt());
                    if (o.has("hold")) m.setHoldMode(o.get("hold").getAsBoolean());
                    if (o.has("drawn")) m.setDrawn(o.get("drawn").getAsBoolean());
                    if (o.has("enabled") && !(m instanceof dev.drevss.drevclient.module.client.ClickGuiModule)) {
                        m.setEnabledSilently(o.get("enabled").getAsBoolean());
                    }
                }
            }
            if (root.has("friends")) {
                c.friends().clear();
                for (JsonElement e : root.getAsJsonArray("friends")) c.friends().add(e.getAsString());
            }
            if (root.has("macros")) {
                c.macros().all().clear();
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("macros").entrySet()) {
                    try { c.macros().set(Integer.parseInt(e.getKey()), e.getValue().getAsString()); }
                    catch (NumberFormatException ignored) { }
                }
            }
            if (root.has("hud")) {
                JsonObject hud = root.getAsJsonObject("hud");
                for (HudElement e : c.hud().elements()) {
                    if (!hud.has(e.getName())) continue;
                    JsonObject o = hud.getAsJsonObject(e.getName());
                    e.setPosition(o.get("x").getAsInt(), o.get("y").getAsInt());
                    if (o.has("visible")) e.setVisible(o.get("visible").getAsBoolean());
                }
            }
            if (root.has("clickgui")) ClickGuiScreen.loadPanels(root.getAsJsonObject("clickgui"));
            return true;
        } catch (Exception e) {
            DrevClient.LOGGER.error("Could not load config " + name, e);
            return false;
        }
    }
}

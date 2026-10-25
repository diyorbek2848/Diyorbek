package dev.drevss.drevclient.gui;

import com.google.gson.JsonObject;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.module.client.ClickGuiModule;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ColorSetting;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.setting.Setting;
import dev.drevss.drevclient.setting.TextSetting;
import dev.drevss.drevclient.util.KeyUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ClickGuiScreen extends Screen {
    private static final int PANEL_W = 112, HEADER_H = 18, ROW_H = 15, SET_H = 13;

    // panel state survives closing and reopening the GUI
    private static final List<Panel> PANELS = new ArrayList<>();
    private static final Set<String> EXPANDED = new HashSet<>();

    private static class Panel {
        final Category category;
        int x, y;
        boolean open = true;
        int scroll;
        int maxBody; // 0 = use the space down to the bottom of the screen

        Panel(Category c, int x, int y) { category = c; this.x = x; this.y = y; }
    }

    static {
        int x = 6;
        for (Category c : Category.values()) {
            PANELS.add(new Panel(c, x, 22));
            x += PANEL_W + 6;
        }
    }

    private static boolean customLayout;

    private Panel dragging;
    private int dragOffX, dragOffY;
    private NumberSetting slider;
    private int sliderX;
    private ColorSetting hueSlider;
    private int hueX;
    private Module binding;
    private TextSetting editing;
    private String editBuffer = "";
    private String search = "";
    private String tooltip;

    public ClickGuiScreen() {
        super(Component.literal("DrevClient"));
    }

    private ClickGuiModule cfg() { return DrevClient.get().modules().get(ClickGuiModule.class); }
    private int accent() { return cfg().accent.getColor(); }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        boolean offScreen = false;
        for (Panel p : PANELS) {
            if (p.x < 0 || p.x + PANEL_W > width || p.y < 0 || p.y + HEADER_H > height - 20) offScreen = true;
        }
        if (!customLayout || offScreen) autoLayout();
    }

    /** Fits all panels on screen: wraps into rows and limits each panel's height (scroll for the rest). */
    private void autoLayout() {
        int cols = Math.max(1, Math.min(PANELS.size(), (width - 6) / (PANEL_W + 6)));
        int rows = (PANELS.size() + cols - 1) / cols;
        int rowH = (height - 46) / rows;
        int totalW = cols * (PANEL_W + 6) - 6;
        int startX = Math.max(4, (width - totalW) / 2);
        List<Panel> ordered = new ArrayList<>(PANELS);
        ordered.sort((a, b) -> a.category.ordinal() - b.category.ordinal());
        for (int i = 0; i < ordered.size(); i++) {
            Panel p = ordered.get(i);
            p.x = startX + (i % cols) * (PANEL_W + 6);
            p.y = 20 + (i / cols) * rowH;
            p.maxBody = rows > 1 ? Math.max(ROW_H * 2, rowH - HEADER_H - 6) : 0;
            p.scroll = 0;
        }
        customLayout = false;
    }

    private int maxBodyHeight(Panel p) {
        int toBottom = height - p.y - HEADER_H - 24;
        return p.maxBody > 0 ? Math.min(p.maxBody, toBottom) : toBottom;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        if (cfg().blur.isOn()) g.fill(0, 0, width, height, 0x88000000);
    }

    // ------------------------------------------------------------------
    // rendering
    // ------------------------------------------------------------------

    private List<Module> visibleModules(Panel p) {
        List<Module> list = new ArrayList<>();
        for (Module m : DrevClient.get().modules().byCategory(p.category)) {
            if (search.isEmpty() || m.getName().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))) list.add(m);
        }
        return list;
    }

    private List<Setting<?>> visibleSettings(Module m) {
        List<Setting<?>> list = new ArrayList<>();
        for (Setting<?> s : m.getSettings()) if (s.isVisible()) list.add(s);
        return list;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        tooltip = null;
        Font f = font;

        if (dragging != null) {
            dragging.x = mouseX - dragOffX;
            dragging.y = mouseY - dragOffY;
        }
        if (slider != null) slider.setPercent((mouseX - sliderX) / (double) (PANEL_W - 8));
        if (hueSlider != null) hueSlider.setHue((float) Math.max(0, Math.min(0.999, (mouseX - hueX) / (double) (PANEL_W - 8))));

        // title + search
        String title = "§lDrev§fClient §7" + DrevClient.VERSION;
        g.drawString(f, title, 6, 6, accent(), true);
        String searchText = search.isEmpty() ? "§8Type to search..." : "§fSearch: " + search + "_";
        g.drawString(f, searchText, width / 2 - f.width(searchText) / 2, 6, 0xFFFFFFFF, true);

        for (Panel p : PANELS) renderPanel(g, p, mouseX, mouseY);

        // bottom buttons
        drawButton(g, "HUD Editor", width - 170, height - 20, 80, mouseX, mouseY);
        drawButton(g, "DrevAI", width - 86, height - 20, 80, mouseX, mouseY);

        if (binding != null) {
            String s = "Press a key for " + binding.getName() + " (Esc = none)";
            g.fill(width / 2 - f.width(s) / 2 - 6, height / 2 - 10, width / 2 + f.width(s) / 2 + 6, height / 2 + 8, 0xE0101018);
            g.drawString(f, s, width / 2 - f.width(s) / 2, height / 2 - 4, 0xFFFFFFFF, true);
        }

        if (tooltip != null && cfg().descriptions.isOn()) {
            int tw = f.width(tooltip);
            int tx = Math.min(mouseX + 10, width - tw - 6);
            int ty = mouseY + 12;
            g.fill(tx - 3, ty - 3, tx + tw + 3, ty + 11, 0xF0101018);
            g.fill(tx - 3, ty - 3, tx - 2, ty + 11, accent());
            g.drawString(f, tooltip, tx, ty, 0xFFDDDDDD, false);
        }
    }

    private void drawButton(GuiGraphics g, String text, int x, int y, int w, int mx, int my) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + 14;
        g.fill(x, y, x + w, y + 14, hover ? withAlpha(accent(), 200) : 0xD0181820);
        g.fill(x, y + 13, x + w, y + 14, accent());
        g.drawString(font, text, x + w / 2 - font.width(text) / 2, y + 3, 0xFFFFFFFF, true);
    }

    private void renderPanel(GuiGraphics g, Panel p, int mx, int my) {
        Font f = font;
        int bg = (cfg().opacity.getInt() << 24) | 0x101018;
        List<Module> modules = visibleModules(p);
        if (!search.isEmpty() && modules.isEmpty()) return;

        // header
        g.fill(p.x, p.y, p.x + PANEL_W, p.y + HEADER_H, withAlpha(accent(), 235));
        g.drawString(f, p.category.displayName, p.x + 5, p.y + 5, 0xFFFFFFFF, true);
        String count = p.open ? "-" : "+";
        g.drawString(f, count, p.x + PANEL_W - 9, p.y + 5, 0xFFFFFFFF, true);
        if (!p.open) return;

        int contentH = 0;
        for (Module m : modules) {
            contentH += ROW_H;
            if (EXPANDED.contains(m.getName())) contentH += visibleSettings(m).size() * SET_H + SET_H;
        }
        int maxH = maxBodyHeight(p);
        p.scroll = Math.max(0, Math.min(p.scroll, Math.max(0, contentH - maxH)));
        int visibleH = Math.min(contentH, maxH);
        int top = p.y + HEADER_H;

        g.fill(p.x, top, p.x + PANEL_W, top + visibleH + 2, bg);
        g.enableScissor(p.x, top, p.x + PANEL_W, top + visibleH + 2);
        int y = top - p.scroll;
        for (Module m : modules) {
            boolean hover = inside(mx, my, p.x, y, PANEL_W, ROW_H) && my >= top && my <= top + visibleH;
            if (m.isEnabled()) g.fill(p.x + 1, y, p.x + PANEL_W - 1, y + ROW_H, withAlpha(accent(), hover ? 200 : 150));
            else if (hover) g.fill(p.x + 1, y, p.x + PANEL_W - 1, y + ROW_H, 0x30FFFFFF);
            g.drawString(f, m.getName(), p.x + 5, y + 4, m.isEnabled() ? 0xFFFFFFFF : 0xFFB8B8C8, true);
            String marker = EXPANDED.contains(m.getName()) ? "v" : "...";
            g.drawString(f, marker, p.x + PANEL_W - 5 - f.width(marker), y + 4, 0xFF808090, false);
            if (hover) tooltip = m.getDescription();
            y += ROW_H;

            if (EXPANDED.contains(m.getName())) {
                for (Setting<?> s : visibleSettings(m)) {
                    renderSetting(g, s, p.x, y, mx, my, top, visibleH);
                    y += SET_H;
                }
                // bind row
                String bind = binding == m ? "Bind: ..." : "Bind: " + KeyUtil.name(m.getKey()) + (m.isHoldMode() ? " (hold)" : "");
                g.fill(p.x + 1, y, p.x + PANEL_W - 1, y + SET_H, 0x40000000);
                g.drawString(f, bind, p.x + 8, y + 3, 0xFF9090A0, false);
                if (inside(mx, my, p.x, y, PANEL_W, SET_H)) tooltip = "Click to bind, right click toggles hold mode";
                y += SET_H;
            }
        }
        g.disableScissor();
        g.fill(p.x, top + visibleH + 1, p.x + PANEL_W, top + visibleH + 2, accent());
    }

    private void renderSetting(GuiGraphics g, Setting<?> s, int x, int y, int mx, int my, int top, int visibleH) {
        Font f = font;
        g.fill(x + 1, y, x + PANEL_W - 1, y + SET_H, 0x40000000);
        g.fill(x + 3, y, x + 4, y + SET_H, withAlpha(accent(), 120));
        boolean hover = inside(mx, my, x, y, PANEL_W, SET_H) && my >= top && my <= top + visibleH;
        if (hover) tooltip = s.getDescription();

        if (s instanceof BoolSetting b) {
            g.drawString(f, s.getName(), x + 8, y + 3, 0xFFD0D0E0, false);
            int bx = x + PANEL_W - 14;
            g.fill(bx, y + 3, bx + 8, y + 10, 0xFF303040);
            if (b.isOn()) g.fill(bx + 1, y + 4, bx + 7, y + 9, accent());
        } else if (s instanceof NumberSetting n) {
            int w = PANEL_W - 8;
            g.fill(x + 4, y + 1, x + 4 + (int) (w * n.getPercent()), y + SET_H - 1, withAlpha(accent(), 110));
            g.drawString(f, s.getName(), x + 8, y + 3, 0xFFD0D0E0, false);
            String v = n.getDisplayValue();
            g.drawString(f, v, x + PANEL_W - 5 - f.width(v), y + 3, 0xFFFFFFFF, false);
        } else if (s instanceof ModeSetting) {
            g.drawString(f, s.getName(), x + 8, y + 3, 0xFFD0D0E0, false);
            String v = s.getDisplayValue();
            g.drawString(f, v, x + PANEL_W - 5 - f.width(v), y + 3, accent(), false);
        } else if (s instanceof ColorSetting c) {
            int w = PANEL_W - 8;
            for (int i = 0; i < w; i += 2) {
                int rgb = java.awt.Color.HSBtoRGB(i / (float) w, 0.75f, 1f);
                g.fill(x + 4 + i, y + 9, x + 6 + i, y + SET_H - 1, 0xFF000000 | rgb);
            }
            g.drawString(f, s.getName(), x + 8, y + 1, 0xFFD0D0E0, false);
            g.fill(x + PANEL_W - 14, y + 1, x + PANEL_W - 5, y + 8, c.getColor());
            if (c.isRainbow()) g.drawString(f, "R", x + PANEL_W - 22, y + 1, 0xFFFFFFFF, false);
        } else if (s instanceof TextSetting t) {
            boolean isEditing = editing == t;
            String v = isEditing ? editBuffer + "_" : t.getName().equals("ApiKey") && !t.get().isEmpty() ? "****" : t.getDisplayValue();
            g.drawString(f, s.getName(), x + 8, y + 3, 0xFFD0D0E0, false);
            int vw = f.width(v);
            int maxW = PANEL_W - 14 - f.width(s.getName());
            if (vw > maxW) v = f.plainSubstrByWidth(v, maxW);
            g.drawString(f, v, x + PANEL_W - 5 - f.width(v), y + 3, isEditing ? 0xFFFFFF55 : 0xFF9AA0FF, false);
            if (hover && !isEditing) tooltip = s.getDescription() + " (click to edit, Ctrl+V pastes)";
        }
    }

    // ------------------------------------------------------------------
    // input
    // ------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();

        if (editing != null) commitEdit();
        if (binding != null) return true;

        if (inside(mx, my, width - 170, height - 20, 80, 14)) {
            minecraft.setScreen(new HudEditorScreen(this));
            return true;
        }
        if (inside(mx, my, width - 86, height - 20, 80, 14)) {
            minecraft.setScreen(new DrevAIScreen());
            return true;
        }

        for (int i = PANELS.size() - 1; i >= 0; i--) {
            Panel p = PANELS.get(i);
            if (handlePanelClick(p, mx, my, button)) {
                PANELS.remove(i);
                PANELS.add(p); // bring to front
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean handlePanelClick(Panel p, double mx, double my, int button) {
        List<Module> modules = visibleModules(p);
        if (!search.isEmpty() && modules.isEmpty()) return false;
        if (inside(mx, my, p.x, p.y, PANEL_W, HEADER_H)) {
            if (button == 0) {
                dragging = p;
                customLayout = true;
                p.maxBody = 0;
                dragOffX = (int) mx - p.x;
                dragOffY = (int) my - p.y;
            } else if (button == 1) {
                p.open = !p.open;
            }
            return true;
        }
        if (!p.open) return false;

        int top = p.y + HEADER_H;
        int maxH = maxBodyHeight(p);
        if (my < top || my > top + maxH || mx < p.x || mx > p.x + PANEL_W) return false;

        int y = top - p.scroll;
        for (Module m : modules) {
            if (inside(mx, my, p.x, y, PANEL_W, ROW_H)) {
                if (button == 0) m.toggle();
                else if (button == 1) {
                    if (!EXPANDED.remove(m.getName())) EXPANDED.add(m.getName());
                } else if (button == 2) binding = m;
                DrevClient.get().config().markDirty();
                return true;
            }
            y += ROW_H;
            if (!EXPANDED.contains(m.getName())) continue;

            for (Setting<?> s : visibleSettings(m)) {
                if (inside(mx, my, p.x, y, PANEL_W, SET_H)) {
                    clickSetting(s, p.x, mx, button);
                    DrevClient.get().config().markDirty();
                    return true;
                }
                y += SET_H;
            }
            if (inside(mx, my, p.x, y, PANEL_W, SET_H)) {
                if (button == 1) m.setHoldMode(!m.isHoldMode());
                else binding = m;
                DrevClient.get().config().markDirty();
                return true;
            }
            y += SET_H;
        }
        return inside(mx, my, p.x, top, PANEL_W, maxH);
    }

    private void clickSetting(Setting<?> s, int panelX, double mx, int button) {
        if (s instanceof BoolSetting b) b.toggle();
        else if (s instanceof NumberSetting n) {
            if (button == 0) { slider = n; sliderX = panelX + 4; }
            else n.reset();
        } else if (s instanceof ModeSetting m) m.cycle(button == 0);
        else if (s instanceof ColorSetting c) {
            if (button == 0) { hueSlider = c; hueX = panelX + 4; c.setRainbow(false); }
            else c.setRainbow(!c.isRainbow());
        } else if (s instanceof TextSetting t) {
            editing = t;
            editBuffer = t.getName().equals("ApiKey") ? "" : t.get();
        }
    }

    private void commitEdit() {
        if (editing != null && !(editing.getName().equals("ApiKey") && editBuffer.isBlank())) {
            editing.set(editBuffer.trim());
            DrevClient.get().config().markDirty();
        }
        editing = null;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        slider = null;
        hueSlider = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        for (int i = PANELS.size() - 1; i >= 0; i--) {
            Panel p = PANELS.get(i);
            if (p.open && mx >= p.x && mx <= p.x + PANEL_W && my >= p.y) {
                p.scroll -= (int) (vertical * 20);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        boolean ctrl = (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;

        if (binding != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) binding.setKey(-1);
            else binding.setKey(key);
            binding = null;
            DrevClient.get().config().markDirty();
            return true;
        }

        if (editing != null) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) commitEdit();
            else if (key == GLFW.GLFW_KEY_ESCAPE) editing = null;
            else if (key == GLFW.GLFW_KEY_BACKSPACE) { if (!editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.length() - 1); }
            else if (ctrl && key == GLFW.GLFW_KEY_V) editBuffer += minecraft.keyboardHandler.getClipboard().replace("\n", "").trim();
            else {
                char c = typed(key, shift);
                if (c != 0) editBuffer += c;
            }
            return true;
        }

        if (key == cfg().getKey() && key != -1) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            return true;
        }
        if (key != GLFW.GLFW_KEY_ESCAPE) {
            char c = KeyUtil.toChar(key, shift);
            if (c != 0 && c != ' ') {
                search += c;
                for (Panel p : PANELS) p.scroll = 0;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    /** Characters allowed in text settings. */
    private static char typed(int key, boolean shift) {
        char c = KeyUtil.toChar(key, shift);
        if (c != 0) return c;
        return switch (key) {
            case GLFW.GLFW_KEY_COMMA -> ',';
            case GLFW.GLFW_KEY_SLASH -> '/';
            case GLFW.GLFW_KEY_SEMICOLON -> shift ? ':' : ';';
            case GLFW.GLFW_KEY_BACKSLASH -> '|';
            case GLFW.GLFW_KEY_EQUAL -> shift ? '+' : '=';
            case GLFW.GLFW_KEY_1 -> '!';
            default -> 0;
        };
    }

    @Override
    public void onClose() {
        DrevClient.get().config().save("default");
        super.onClose();
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my < y + h;
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    // ------------------------------------------------------------------
    // persistence
    // ------------------------------------------------------------------

    /** Opens a module's settings in the GUI (used by the automated test). */
    public static void expand(String moduleName) { EXPANDED.add(moduleName); }

    public static JsonObject savePanels() {
        JsonObject o = new JsonObject();
        for (Panel p : PANELS) {
            JsonObject po = new JsonObject();
            po.addProperty("x", p.x);
            po.addProperty("y", p.y);
            po.addProperty("open", p.open);
            po.addProperty("maxBody", p.maxBody);
            o.add(p.category.name(), po);
        }
        return o;
    }

    public static void loadPanels(JsonObject o) {
        for (Panel p : PANELS) {
            if (!o.has(p.category.name())) continue;
            JsonObject po = o.getAsJsonObject(p.category.name());
            p.x = po.get("x").getAsInt();
            p.y = po.get("y").getAsInt();
            p.open = po.get("open").getAsBoolean();
            if (po.has("maxBody")) p.maxBody = po.get("maxBody").getAsInt();
            customLayout = true;
        }
    }
}

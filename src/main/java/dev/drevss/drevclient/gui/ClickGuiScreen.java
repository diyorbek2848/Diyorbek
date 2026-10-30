package dev.drevss.drevclient.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.gui.ui.Ui;
import dev.drevss.drevclient.gui.ui.Ui.F;
import dev.drevss.drevclient.hud.HudElement;
import dev.drevss.drevclient.module.Category;
import dev.drevss.drevclient.module.Module;
import dev.drevss.drevclient.module.client.ClickGuiModule;
import dev.drevss.drevclient.module.client.ClientSettings;
import dev.drevss.drevclient.module.client.DrevAIModule;
import dev.drevss.drevclient.module.client.HudModule;
import dev.drevss.drevclient.setting.BoolSetting;
import dev.drevss.drevclient.setting.ColorSetting;
import dev.drevss.drevclient.setting.ModeSetting;
import dev.drevss.drevclient.setting.NumberSetting;
import dev.drevss.drevclient.setting.Setting;
import dev.drevss.drevclient.setting.TextSetting;
import dev.drevss.drevclient.util.KeyUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** The DrevClient menu (Right Shift). */
public class ClickGuiScreen extends Screen {
    // ---- palette ----
    private static final int BORDER = 0xFF1C2033;
    private static final int SIDEBAR = 0xFF0D1020;
    private static final int CARD = 0xFF121626;
    private static final int CARD_HOVER = 0xFF171B2D;
    private static final int CARD_BORDER = 0xFF1E2337;
    private static final int INPUT = 0xFF0E111D;
    private static final int TEXT = 0xFFE8EAF4;
    private static final int TEXT2 = 0xFF8B91AA;
    private static final int TEXT3 = 0xFF5C6280;
    private static final int TRACK_OFF = 0xFF262B40;
    private static final int RED = 0xFFFF5C6C;
    private static final int[] PRESETS = { 0xFF8C7BFF, 0xFF5B8CFF, 0xFF3FD0FF, 0xFF2EE6A6, 0xFFFFC14D, 0xFFFF7A45, 0xFFFF4D8D, 0xFFE0E4F0 };

    private enum Page { CATEGORY, SETTINGS, THEME, CONFIGS, SOCIALS, KEYBINDS, SKINS }

    // ---- state that survives closing the menu ----
    private static Page page = Page.CATEGORY;
    private static Category category = Category.COMBAT;
    private static boolean grid;
    private static String filter = "All";
    private static final Set<String> EXPANDED = new HashSet<>();
    private static final Map<Page, Float> SCROLL = new HashMap<>();

    // ---- per-open state ----
    private final long openedAt = System.currentTimeMillis();
    private String search = "";
    private String focused;                       // key of the focused text field
    private final Map<String, String> fields = new HashMap<>();
    private Module binding;
    private boolean bindingMacro;
    private int macroKey = -1;
    private NumberSetting slider;
    private float sliderX, sliderW;
    private ColorSetting hueDrag;
    private float hueX, hueW;
    private String tooltip;
    private String toast;
    private long toastAt;
    private String confirmDelete;
    private float contentHeight, viewHeight;
    private float viewX, viewY, viewW;
    private String scrollTo;

    public ClickGuiScreen() {
        super(Component.literal("DrevClient"));
    }

    private ClickGuiModule cfg() { return DrevClient.get().modules().get(ClickGuiModule.class); }
    private int accent() { return cfg().accent.getColor() | 0xFF000000; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        if (minecraft.level != null && cfg().blur.isOn()) renderBlurredBackground(g);
        g.fill(0, 0, width, height, minecraft.level != null ? 0x66050611 : 0xFF07080F);
    }

    // ==================================================================
    // frame
    // ==================================================================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float pt) {
        Ui.begin(graphics, mouseX, mouseY);
        tooltip = null;
        try {
            if (slider != null) slider.setPercent((Ui.mx - sliderX) / sliderW);
            if (hueDrag != null) hueDrag.setHue(Math.max(0f, Math.min(0.999f, (Ui.mx - hueX) / hueW)));

            float open = Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f);
            float pw = Math.min(1120, Ui.vw - 72), ph = Math.min(720, Ui.vh - 56);
            float px = (Ui.vw - pw) / 2, py = (Ui.vh - ph) / 2 + (1 - open) * 12;

            int panelBg = Ui.withA(0x0A0C16, cfg().opacity.getInt());
            Ui.shadow(px, py, pw, ph, 18, 90);
            Ui.roundBordered(px, py, pw, ph, 18, panelBg, BORDER);

            float sbw = 226;
            drawSidebar(px + 10, py + 10, sbw, ph - 20);
            float cx = px + sbw + 40, cw = pw - sbw - 40 - 30;
            drawContent(cx, py + 26, cw, ph - 26 - 18);

            if (toast != null && System.currentTimeMillis() - toastAt < 2600) drawToast(px + pw / 2, py + ph - 26);
            if (tooltip != null && cfg().descriptions.isOn()) drawTooltip(tooltip);
        } finally {
            Ui.end();
        }
    }

    private void toast(String text) {
        toast = text;
        toastAt = System.currentTimeMillis();
    }

    private void drawToast(float cx, float cy) {
        float w = Ui.width(toast, F.SEMI, 13) + 32;
        Ui.roundBordered(cx - w / 2, cy - 16, w, 32, 16, 0xF0141828, Ui.alpha(accent(), 0.6f));
        Ui.textCenter(toast, F.SEMI, 13, cx, cy, TEXT);
    }

    private void drawTooltip(String text) {
        List<String> lines = Ui.wrap(text, F.SANS, 12, 300, 4);
        float w = 0;
        for (String l : lines) w = Math.max(w, Ui.width(l, F.SANS, 12));
        w += 22;
        float h = lines.size() * 17 + 14;
        float x = Math.min(Ui.mx + 14, Ui.vw - w - 8), y = Math.min(Ui.my + 16, Ui.vh - h - 8);
        Ui.roundBordered(x, y, w, h, 8, 0xF7101320, CARD_BORDER);
        for (int i = 0; i < lines.size(); i++) Ui.text(lines.get(i), F.SANS, 12, x + 11, y + 15 + i * 17, 0xFFC8CCDD);
    }

    // ==================================================================
    // sidebar
    // ==================================================================

    private void drawSidebar(float x, float y, float w, float h) {
        Ui.roundBordered(x, y, w, h, 14, SIDEBAR, BORDER);

        // brand
        int logoPx = Ui.p(26);
        int file = logoPx <= 24 ? 24 : logoPx <= 36 ? 36 : 48;
        Ui.texture(Identifier.parse("drevclient:textures/gui/logo_" + file + ".png"), x + 18, y + 18, 26, 26, file, file, 0xFFFFFFFF);
        Ui.textBase("Drev", F.SEMI, 17, x + 52, y + 33, TEXT);
        Ui.textBase("Client", F.SERIF, 20, x + 52 + Ui.width("Drev ", F.SEMI, 17), y + 33, accent());
        Ui.text("BETA RELEASE " + DrevClient.VERSION, F.SANS, 10, x + 52, y + 46, TEXT3);

        // the list scales down a little on short windows
        int items = (Category.values().length - 1) + 8;
        boolean showProfile = h > 540;
        float fixed = 78 + 14 + 14 + 14 + (showProfile ? 60 : 8);
        float itemH = Math.max(18, Math.min(36, (h - fixed) / items - 2));

        float iy = y + 78;
        Ui.text("MODULES", F.SEMI, 11, x + 18, iy, TEXT3);
        iy += 14;
        for (Category c : Category.values()) {
            if (c == Category.CLIENT) continue;
            int count = DrevClient.get().modules().byCategory(c).size();
            boolean sel = page == Page.CATEGORY && category == c && search.isEmpty();
            Category target = c;
            sidebarItem(x + 8, iy, w - 16, itemH, iconFor(c), c.displayName, String.valueOf(count), sel, () -> {
                page = Page.CATEGORY;
                category = target;
                search = "";
                focused = null;
            });
            iy += itemH + 2;
        }

        iy += 14;
        Ui.text("GENERAL", F.SEMI, 11, x + 18, iy, TEXT3);
        iy += 14;
        iy = generalItem(x, iy, w, itemH, "settings", "Settings", Page.SETTINGS);
        iy = generalItem(x, iy, w, itemH, "theme", "Theme", Page.THEME);
        iy = generalItem(x, iy, w, itemH, "configs", "Configs", Page.CONFIGS);
        iy = generalItem(x, iy, w, itemH, "socials", "Socials", Page.SOCIALS);
        iy = generalItem(x, iy, w, itemH, "keybinds", "Keybinds", Page.KEYBINDS);
        iy = generalItem(x, iy, w, itemH, "skins", "Skins", Page.SKINS);
        sidebarItem(x + 8, iy, w - 16, itemH, "hud", "HUD Editor", "", false, () -> minecraft.setScreen(new HudEditorScreen(this)));
        iy += itemH + 2;
        sidebarItem(x + 8, iy, w - 16, itemH, "ai", "DrevAI", "", false, () -> minecraft.setScreen(new DrevAIScreen()));

        // profile at the bottom
        if (!showProfile) return;
        float by = y + h - 52;
        Ui.rect(x + 14, by, w - 28, 1, BORDER);
        face(minecraft.getUser().getName(), x + 18, by + 12, 28);
        Ui.text(Ui.ellipsize(minecraft.getUser().getName(), F.SEMI, 13, w - 80), F.SEMI, 13, x + 56, by + 20, TEXT);
        long on = DrevClient.get().modules().all().stream().filter(m -> m.isEnabled() && m.getCategory() != Category.CLIENT).count();
        Ui.text(on + " modules on", F.SANS, 11, x + 56, by + 35, TEXT2);
    }

    private float generalItem(float x, float iy, float w, float itemH, String icon, String label, Page target) {
        sidebarItem(x + 8, iy, w - 16, itemH, icon, label, "", page == target, () -> {
            page = target;
            focused = null;
        });
        return iy + itemH + 2;
    }

    private void sidebarItem(float x, float y, float w, float h, String icon, String label, String count, boolean selected, Runnable action) {
        boolean hov = Ui.hover(x, y, w, h);
        float t = Ui.anim("sb:" + label, selected ? 1 : hov ? 0.45f : 0, 14);
        if (t > 0.01f) {
            Ui.round(x, y, w, h, 9, Ui.alpha(Ui.withA(accent(), 0x30), t));
            if (selected) {
                Ui.fadeH(x, y, w * 0.8f, h, Ui.withA(accent(), 0x38));
                Ui.round(x, y + h * 0.25f, 3, h * 0.5f, 1.5f, accent());
            }
        }
        int fg = selected ? 0xFFFFFFFF : Ui.mix(0xFFAEB3C8, 0xFFFFFFFF, t);
        Ui.icon(icon, x + 12, y + h / 2 - 8, 16, selected ? Ui.mix(accent(), 0xFFFFFFFF, 0.25f) : fg);
        Ui.text(label, selected ? F.SEMI : F.SANS, h < 26 ? 13 : 14, x + 38, y + h / 2, fg);
        if (!count.isEmpty()) Ui.textRight(count, F.SANS, 11, x + w - 12, y + h / 2, TEXT3);
        Ui.onClick(x, y, w, h, b -> action.run());
    }

    private static String iconFor(Category c) {
        return switch (c) {
            case COMBAT -> "combat";
            case PLAYER -> "player";
            case MOVEMENT -> "movement";
            case RENDER -> "render";
            case WORLD -> "world";
            case MISC -> "misc";
            default -> "settings";
        };
    }

    // ==================================================================
    // content
    // ==================================================================

    private void drawContent(float x, float y, float w, float h) {
        viewX = x;
        viewW = w;
        switch (page) {
            case CATEGORY -> categoryPage(x, y, w, h);
            case SETTINGS -> settingsPage(x, y, w, h);
            case THEME -> themePage(x, y, w, h);
            case CONFIGS -> configsPage(x, y, w, h);
            case SOCIALS -> socialsPage(x, y, w, h);
            case KEYBINDS -> keybindsPage(x, y, w, h);
            case SKINS -> skinsPage(x, y, w, h);
        }
    }

    private float headerRight;

    private void bigHeader(float x, float y, String accentWord, String rest, String subtitle) {
        Ui.textBase(accentWord, F.SERIF, 30, x, y + 26, accent());
        float rx = x + Ui.width(accentWord, F.SERIF, 30) + 8;
        Ui.textBase(rest, F.BOLD, 26, rx, y + 26, TEXT);
        headerRight = rx + Ui.width(rest, F.BOLD, 26);
        Ui.text(Ui.ellipsize(subtitle, F.SANS, 13, viewW), F.SANS, 13, x, y + 47, TEXT2);
    }

    /** Search box at the top right, shrunk so it never covers the title. */
    private void headerSearch(float x, float y, float w) {
        float sw = Math.min(290, x + w - headerRight - 24);
        if (sw < 150) sw = 150;
        searchBox(x + w - sw, y, sw, 40);
    }

    /** Begins a scrollable area; returns the y to start drawing at. */
    private float beginScroll(float x, float y, float w, float h) {
        viewY = y;
        viewHeight = h;
        float max = Math.max(0, contentHeight - h + 8);
        float target = Math.max(0, Math.min(SCROLL.getOrDefault(page, 0f), max));
        SCROLL.put(page, target);
        float cur = Ui.anim("scroll:" + page, target, 18);
        Ui.clip(x - 6, y, w + 12, h);
        return y - cur;
    }

    private void endScroll(float x, float y, float w, float h, float startY, float endY) {
        contentHeight = endY - startY;
        Ui.unclip();
        // scrollbar
        if (contentHeight > h) {
            float cur = Ui.anim("scroll:" + page, SCROLL.getOrDefault(page, 0f), 18);
            float barH = Math.max(30, h * h / contentHeight);
            float barY = y + (h - barH) * (cur / Math.max(1, contentHeight - h + 8));
            Ui.round(x + w + 10, barY, 3, barH, 1.5f, 0x40FFFFFF);
        }
    }

    // ------------------------------------------------------------------
    // modules page
    // ------------------------------------------------------------------

    private List<Module> visibleModules() {
        List<Module> list = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT).trim();
        for (Module m : DrevClient.get().modules().all()) {
            if (m.getCategory() == Category.CLIENT) continue;
            if (q.isEmpty()) {
                if (m.getCategory() != category) continue;
            } else if (!m.getName().toLowerCase(Locale.ROOT).contains(q) && !m.getDescription().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            if (filter.equals("Enabled") && !m.isEnabled()) continue;
            if (filter.equals("Disabled") && m.isEnabled()) continue;
            list.add(m);
        }
        return list;
    }

    private void categoryPage(float x, float y, float w, float h) {
        List<Module> mods = visibleModules();
        long on = mods.stream().filter(Module::isEnabled).count();
        if (search.isEmpty()) {
            bigHeader(x, y, category.displayName, "Modules", mods.size() + " modules  ·  " + on + " enabled");
        } else {
            bigHeader(x, y, "Search", "results", mods.size() + " modules match “" + search + "”");
        }
        headerSearch(x, y, w);

        // filter chips + view toggle
        float cy = y + 72;
        float chipX = x;
        for (String f : new String[] { "All", "Enabled", "Disabled" }) {
            float cw = Ui.width(f, F.SEMI, 12) + 26;
            boolean sel = filter.equals(f);
            boolean hov = Ui.hover(chipX, cy, cw, 28);
            Ui.roundBordered(chipX, cy, cw, 28, 8, sel ? Ui.withA(accent(), 0x2A) : hov ? CARD_HOVER : CARD, sel ? Ui.withA(accent(), 0x90) : CARD_BORDER);
            Ui.textCenter(f, F.SEMI, 12, chipX + cw / 2, cy + 14, sel ? TEXT : TEXT2);
            String target = f;
            Ui.onClick(chipX, cy, cw, 28, b -> filter = target);
            chipX += cw + 8;
        }
        viewToggle(x + w - 66, cy, false, "list");
        viewToggle(x + w - 32, cy, true, "grid");

        float top = cy + 42, vh = h - (top - y);
        float sy = beginScroll(x, top, w, vh);
        float yy = sy;
        if (mods.isEmpty()) {
            Ui.textCenter("No modules here", F.SEMI, 15, x + w / 2, top + 60, TEXT2);
            yy += 120;
        } else if (grid) {
            int cols = w >= 780 ? 3 : 2;
            float gap = 12, cw = (w - gap * (cols - 1)) / cols, ch = 118;
            for (int i = 0; i < mods.size(); i++) {
                float gx = x + (i % cols) * (cw + gap), gy = yy + (i / cols) * (ch + gap);
                if (gy + ch >= top && gy <= top + vh) gridCard(mods.get(i), gx, gy, cw, ch);
            }
            yy += ((mods.size() + cols - 1) / cols) * (ch + gap);
        } else {
            for (Module m : mods) {
                float ch = cardHeight(m, false);
                if (m.getName().equals(scrollTo)) {
                    SCROLL.put(page, yy - sy);
                    scrollTo = null;
                }
                if (yy + ch >= top && yy <= top + vh) moduleCard(m, x, yy, w, false, true);
                yy += ch + 10;
            }
        }
        endScroll(x, top, w, vh, sy, yy);
    }

    private void viewToggle(float x, float y, boolean gridMode, String icon) {
        boolean sel = grid == gridMode;
        boolean hov = Ui.hover(x, y, 28, 28);
        if (sel || hov) Ui.round(x, y, 28, 28, 7, sel ? 0x1FFFFFFF : 0x10FFFFFF);
        Ui.icon(icon, x + 6, y + 6, 16, sel ? TEXT : TEXT3);
        Ui.onClick(x, y, 28, 28, b -> grid = gridMode);
    }

    private void searchBox(float x, float y, float w, float h) {
        boolean foc = "search".equals(focused);
        Ui.roundBordered(x, y, w, h, 10, INPUT, foc ? Ui.withA(accent(), 0xB0) : CARD_BORDER);
        Ui.icon("search", x + 14, y + h / 2 - 8, 16, TEXT3);
        if (search.isEmpty() && !foc) Ui.text("Search modules", F.SANS, 13, x + 40, y + h / 2, TEXT3);
        else Ui.text(Ui.ellipsize(search, F.SANS, 13, w - 70) + caret(foc), F.SANS, 13, x + 40, y + h / 2, TEXT);
        if (!search.isEmpty()) {
            Ui.icon("close", x + w - 28, y + h / 2 - 7, 14, TEXT3);
            Ui.onClick(x + w - 34, y, 34, h, b -> search = "");
        }
        Ui.onClick(x, y, w - (search.isEmpty() ? 0 : 34), h, b -> focused = "search");
    }

    private String caret(boolean focused) {
        return focused && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "";
    }

    // ------------------------------------------------------------------
    // module cards
    // ------------------------------------------------------------------

    private static final float HEAD_H = 66, ROW_H = 42;

    private boolean expanded(Module m, boolean forced) { return forced || EXPANDED.contains(m.getName()); }

    private List<Setting<?>> visibleSettings(Module m) {
        List<Setting<?>> out = new ArrayList<>();
        for (Setting<?> s : m.getSettings()) if (s.isVisible()) out.add(s);
        return out;
    }

    private boolean hasModuleExtras(Module m) {
        return !(m instanceof ClientSettings) && !(m instanceof ClickGuiModule) && !(m instanceof DrevAIModule);
    }

    private float cardHeight(Module m, boolean forced) {
        if (!expanded(m, forced)) return HEAD_H;
        int rows = visibleSettings(m).size() + (hasModuleExtras(m) ? 2 : 0);
        return HEAD_H + 10 + rows * ROW_H + 8;
    }

    private void moduleCard(Module m, float x, float y, float w, boolean forced, boolean showSwitch) {
        float h = cardHeight(m, forced);
        boolean hovHead = Ui.hover(x, y, w, HEAD_H);
        float hv = Ui.anim("card:" + m.getName(), hovHead ? 1 : 0, 14);
        Ui.roundBordered(x, y, w, h, 12, Ui.mix(CARD, CARD_HOVER, hv), m.isEnabled() ? Ui.mix(CARD_BORDER, Ui.withA(accent(), 0x70), 0.6f) : CARD_BORDER);

        String title = m instanceof ClickGuiModule ? "Menu" : m instanceof ClientSettings ? "General" : m.getName();
        String desc = m instanceof ClickGuiModule ? "How this menu looks  \u00b7  opens with the key on the right"
            : m instanceof ClientSettings ? "Applies to the whole client" : m.getDescription();
        Ui.text(title, F.SEMI, 15, x + 22, y + 25, TEXT);
        if (binding == m) Ui.text("Press a key\u2026", F.SEMI, 12, x + 30 + Ui.width(title, F.SEMI, 15), y + 25, accent());
        else if (m.getKey() >= 0) {
            String k = KeyUtil.name(m.getKey());
            float kx = x + 32 + Ui.width(title, F.SEMI, 15);
            float kw = Ui.width(k, F.SEMI, 10) + 12;
            Ui.roundBordered(kx, y + 16, kw, 18, 5, 0xFF1A1E30, CARD_BORDER);
            Ui.textCenter(k, F.SEMI, 10, kx + kw / 2, y + 25, TEXT2);
        }
        if (!search.isEmpty() && page == Page.CATEGORY) {
            String cat = m.getCategory().displayName;
            float tx = x + w - 150 - Ui.width(cat, F.SANS, 11);
            Ui.text(cat, F.SANS, 11, tx, y + 25, TEXT3);
        }
        Ui.text(Ui.ellipsize(desc, F.SANS, 12, w - 190), F.SANS, 12, x + 22, y + 45, TEXT2);

        float sx = x + w - 22 - 40;
        if (showSwitch) {
            toggleSwitch("mod:" + m.getName(), sx, y + HEAD_H / 2 - 10, 40, 20, m.isEnabled(), () -> {
                m.toggle();
                DrevClient.get().config().markDirty();
            });
        }
        if (!forced) {
            boolean exp = expanded(m, false);
            float chx = (showSwitch ? sx : x + w - 22) - 34;
            boolean ch = Ui.hover(chx - 6, y + HEAD_H / 2 - 14, 28, 28);
            if (ch) Ui.round(chx - 6, y + HEAD_H / 2 - 14, 28, 28, 7, 0x14FFFFFF);
            Ui.icon(exp ? "chevron_down" : "chevron", chx, y + HEAD_H / 2 - 8, 16, ch ? TEXT : TEXT2);
            // header: left click opens settings, right click toggles, middle click binds
            Ui.onClick(x, y, (showSwitch ? sx - 8 : x + w) - x, HEAD_H, b -> {
                if (b == 1 && showSwitch) m.toggle();
                else if (b == 2) binding = m;
                else if (!EXPANDED.remove(m.getName())) EXPANDED.add(m.getName());
                DrevClient.get().config().markDirty();
            });
        }

        if (!expanded(m, forced)) return;
        float ry = y + HEAD_H;
        Ui.rect(x + 20, ry, w - 40, 1, CARD_BORDER);
        ry += 10;
        float right = x + w - 22;
        for (Setting<?> s : visibleSettings(m)) {
            settingRow(m, s, x + 22, ry, right);
            ry += ROW_H;
        }
        if (hasModuleExtras(m)) {
            keybindRow(m, x + 22, ry, right);
            ry += ROW_H;
            rowLabel("Show in HUD module list", "Hide modules you don't want in the HUD list", x + 22, ry, right);
            toggleSwitch("drawn:" + m.getName(), right - 36, ry + ROW_H / 2 - 9, 36, 18, m.isDrawn(), () -> {
                m.setDrawn(!m.isDrawn());
                DrevClient.get().config().markDirty();
            });
        }
    }

    private void gridCard(Module m, float x, float y, float w, float h) {
        boolean hov = Ui.hover(x, y, w, h);
        float hv = Ui.anim("gcard:" + m.getName(), hov ? 1 : 0, 14);
        Ui.roundBordered(x, y, w, h, 12, Ui.mix(CARD, CARD_HOVER, hv), m.isEnabled() ? Ui.withA(accent(), 0x80) : CARD_BORDER);
        Ui.onClick(x, y, w, h, b -> {
            if (b == 2) binding = m;
            else m.toggle();
            DrevClient.get().config().markDirty();
        });
        Ui.text(Ui.ellipsize(m.getName(), F.SEMI, 15, w - 40), F.SEMI, 15, x + 18, y + 24, TEXT);
        List<String> desc = Ui.wrap(m.getDescription(), F.SANS, 12, w - 36, 2);
        for (int i = 0; i < desc.size(); i++) Ui.text(desc.get(i), F.SANS, 12, x + 18, y + 46 + i * 16, TEXT2);

        float by = y + h - 32;
        boolean sh = Ui.hover(x + 12, by - 4, 90, 26);
        Ui.icon("settings", x + 18, by + 1, 14, sh ? TEXT : TEXT3);
        Ui.text("Settings", F.SANS, 12, x + 38, by + 8, sh ? TEXT : TEXT3);
        Ui.onClick(x + 12, by - 4, 90, 26, b -> {
            grid = false;
            EXPANDED.add(m.getName());
            scrollTo = m.getName();
        });
        toggleSwitch("gmod:" + m.getName(), x + w - 18 - 40, by - 2, 40, 20, m.isEnabled(), () -> {
            m.toggle();
            DrevClient.get().config().markDirty();
        });
    }

    // ------------------------------------------------------------------
    // controls
    // ------------------------------------------------------------------

    /** Window-pixel centers of module switches from the last frame (used by the automated test). */
    public static final Map<String, int[]> SWITCH_POS = new HashMap<>();

    private void toggleSwitch(String key, float x, float y, float w, float h, boolean on, Runnable onToggle) {
        if (key.startsWith("mod:")) SWITCH_POS.put(key.substring(4), new int[] { Ui.p(x + w / 2), Ui.p(y + h / 2) });
        float t = Ui.anim("sw:" + key, on ? 1 : 0, 16);
        Ui.round(x, y, w, h, h / 2, Ui.mix(TRACK_OFF, accent(), t));
        float r = h / 2 - 3;
        float kx = x + h / 2 + (w - h) * t;
        Ui.circleFill(kx, y + h / 2, r, Ui.mix(0xFFC9CDDD, 0xFFFFFFFF, t));
        Ui.onClick(x - 4, y - 6, w + 8, h + 12, b -> onToggle.run());
    }

    private void rowLabel(String label, String description, float x, float y, float right) {
        boolean hov = Ui.hover(x - 8, y, right - x + 16, ROW_H);
        if (hov) {
            Ui.round(x - 10, y + 2, right - x + 20, ROW_H - 4, 8, 0x08FFFFFF);
            tooltip = description;
        }
        Ui.text(label, F.SANS, 13, x, y + ROW_H / 2, 0xFFC5C9DA);
    }

    private static String pretty(String name) {
        // "MaxSelfDamage" -> "Max self damage"
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(name.charAt(i - 1))) sb.append(' ').append(Character.toLowerCase(c));
            else sb.append(c);
        }
        return sb.toString();
    }

    private void settingRow(Module m, Setting<?> s, float x, float y, float right) {
        rowLabel(pretty(s.getName()), s.getDescription(), x, y, right);
        float cy = y + ROW_H / 2;
        String key = m.getName() + ":" + s.getName();

        if (s instanceof BoolSetting b) {
            toggleSwitch(key, right - 36, cy - 9, 36, 18, b.isOn(), () -> {
                b.toggle();
                changed(m, s);
            });
        } else if (s instanceof NumberSetting n) {
            float vw = 56, tw = Math.min(240, (right - x) * 0.45f);
            float tx = right - vw - 14 - tw;
            float pct = (float) n.getPercent();
            Ui.round(tx, cy - 2.5f, tw, 5, 2.5f, TRACK_OFF);
            Ui.round(tx, cy - 2.5f, Math.max(5, tw * pct), 5, 2.5f, accent());
            boolean drag = slider == n;
            float kr = Ui.anim("knob:" + key, drag || Ui.hover(tx - 8, cy - 12, tw + 16, 24) ? 8 : 6.5f, 18);
            Ui.circleFill(tx + tw * pct, cy, kr, 0xFFFFFFFF);
            Ui.roundBordered(right - vw, cy - 13, vw, 26, 7, INPUT, CARD_BORDER);
            Ui.textCenter(n.getDisplayValue(), F.SEMI, 12, right - vw / 2, cy, TEXT);
            Ui.onClick(tx - 8, cy - 13, tw + 16, 26, b -> {
                if (b == 1) {
                    n.reset();
                    changed(m, s);
                } else {
                    slider = n;
                    sliderX = tx;
                    sliderW = tw;
                }
            });
        } else if (s instanceof ModeSetting ms) {
            modeControl(m, ms, key, right, cy, (right - x) * 0.62f);
        } else if (s instanceof ColorSetting c) {
            float sw = 22;
            Ui.roundBordered(right - sw, cy - sw / 2, sw, sw, 6, c.getColor() | 0xFF000000, 0x40FFFFFF);
            Ui.text("Rainbow", F.SANS, 12, right - sw - 12 - 32 - 8 - Ui.width("Rainbow", F.SANS, 12), cy, TEXT2);
            toggleSwitch(key + ":rb", right - sw - 12 - 32, cy - 8, 32, 16, c.isRainbow(), () -> {
                c.setRainbow(!c.isRainbow());
                changed(m, s);
            });
            float hw = Math.min(170, (right - x) * 0.3f);
            float hx = right - sw - 12 - 32 - 16 - Ui.width("Rainbow", F.SANS, 12) - 14 - hw;
            Ui.round(hx - 1, cy - 5, hw + 2, 10, 5, 0x30FFFFFF);
            Ui.clip(hx, cy - 4, hw, 8);
            Ui.hueBar(hx, cy - 4, hw, 8);
            Ui.unclip();
            Ui.circleFill(hx + hw * c.getHue(), cy, 6, 0xFFFFFFFF);
            Ui.circleFill(hx + hw * c.getHue(), cy, 4, c.getColor() | 0xFF000000);
            Ui.onClick(hx - 6, cy - 12, hw + 12, 24, b -> {
                hueDrag = c;
                hueX = hx;
                hueW = hw;
                c.setRainbow(false);
            });
        } else if (s instanceof TextSetting t) {
            boolean secret = t.getName().equals("ApiKey");
            String fk = "set:" + key;
            float fw = Math.min(320, (right - x) * 0.55f);
            String shown = secret && !t.get().isEmpty() ? "•".repeat(Math.min(24, t.get().length())) : t.get();
            textField(fk, right - fw, cy - 15, fw, 30, shown, secret ? "Paste your key" : "", v -> {
                if (secret) {
                    String key = dev.drevss.drevclient.ai.DrevAI.cleanKey(v);
                    if (key.isEmpty()) return;
                    String problem = dev.drevss.drevclient.ai.DrevAI.keyProblem(key);
                    if (problem != null) {
                        toast(problem);
                        return;
                    }
                    toast("Checking key\u2026");
                    DrevClient.get().ai().verifyKey(key, error -> {
                        if (error == null) {
                            t.set(key);
                            changed(m, s);
                            toast("Key works and is saved");
                        } else toast(error.length() > 90 ? error.substring(0, 88) + "\u2026" : error);
                    });
                    return;
                }
                if (true) {
                    t.set(v.trim());
                    changed(m, s);
                }
            }, secret ? "" : t.get());
        }
    }

    private void changed(Module m, Setting<?> s) {
        DrevClient.get().config().markDirty();
    }

    private void modeControl(Module m, ModeSetting ms, String key, float right, float cy, float maxW) {
        List<String> modes = ms.getModes();
        float total = 8;
        for (String o : modes) total += Ui.width(o, F.SEMI, 12) + 22;
        if (modes.size() <= 4 && total <= maxW) {
            float bx = right - total;
            Ui.roundBordered(bx, cy - 15, total, 30, 8, INPUT, CARD_BORDER);
            float ox = bx + 4;
            for (String o : modes) {
                float ow = Ui.width(o, F.SEMI, 12) + 22;
                boolean sel = ms.is(o);
                float t = Ui.anim("mode:" + key + ":" + o, sel ? 1 : 0, 16);
                if (t > 0.01f) Ui.round(ox, cy - 11, ow, 22, 6, Ui.alpha(Ui.withA(accent(), 0x55), t));
                Ui.textCenter(o, F.SEMI, 12, ox + ow / 2, cy, sel ? TEXT : TEXT2);
                Ui.onClick(ox, cy - 13, ow, 26, b -> {
                    ms.parse(o);
                    changed(m, ms);
                });
                ox += ow;
            }
        } else {
            float bw = 0;
            for (String o : modes) bw = Math.max(bw, Ui.width(o, F.SEMI, 12));
            bw += 70;
            float bx = right - bw;
            Ui.roundBordered(bx, cy - 15, bw, 30, 8, INPUT, CARD_BORDER);
            Ui.textCenter(ms.get(), F.SEMI, 12, bx + bw / 2, cy, TEXT);
            boolean lh = Ui.hover(bx, cy - 15, 30, 30), rh = Ui.hover(bx + bw - 30, cy - 15, 30, 30);
            Ui.text("‹", F.SEMI, 15, bx + 11, cy, lh ? TEXT : TEXT3);
            Ui.text("›", F.SEMI, 15, bx + bw - 18, cy, rh ? TEXT : TEXT3);
            Ui.onClick(bx, cy - 15, bw / 2, 30, b -> {
                ms.cycle(false);
                changed(m, ms);
            });
            Ui.onClick(bx + bw / 2, cy - 15, bw / 2, 30, b -> {
                ms.cycle(true);
                changed(m, ms);
            });
        }
    }

    private void keybindRow(Module m, float x, float y, float right) {
        rowLabel("Keybind", "Click, then press a key. Right click or Esc clears it.", x, y, right);
        float cy = y + ROW_H / 2;
        float chipW = keyChip(m.getKey(), binding == m, right, cy, b -> {
            if (b == 1) {
                m.setKey(-1);
                DrevClient.get().config().markDirty();
            } else binding = m;
        });
        float hx = right - chipW - 18 - 32;
        toggleSwitch("hold:" + m.getName(), hx, cy - 8, 32, 16, m.isHoldMode(), () -> {
            m.setHoldMode(!m.isHoldMode());
            DrevClient.get().config().markDirty();
        });
        Ui.text("Hold", F.SANS, 12, hx - 10 - Ui.width("Hold", F.SANS, 12), cy, TEXT2);
    }

    /** Draws a key chip right-aligned at right; returns its width. */
    private float keyChip(int key, boolean waiting, float right, float cy, Ui.ClickHandler onClick) {
        String label = waiting ? "Press a key…" : key < 0 ? "None" : KeyUtil.name(key);
        float w = Math.max(64, Ui.width(label, F.SEMI, 12) + 26);
        boolean hov = Ui.hover(right - w, cy - 14, w, 28);
        int border = waiting ? Ui.alpha(accent(), 0.5f + 0.5f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 250.0))) : hov ? 0xFF2C3250 : CARD_BORDER;
        Ui.roundBordered(right - w, cy - 14, w, 28, 7, INPUT, border);
        Ui.textCenter(label, F.SEMI, 12, right - w / 2, cy, waiting ? accent() : key < 0 ? TEXT3 : TEXT);
        Ui.onClick(right - w, cy - 14, w, 28, onClick);
        return w;
    }

    public interface Commit { void commit(String value); }
    private final Map<String, Commit> commits = new HashMap<>();

    /**
     * A single-line text field. shown = text when not focused; initial = text
     * placed in the field when it gets focus.
     */
    private void textField(String key, float x, float y, float w, float h, String shown, String placeholder, Commit commit, String initial) {
        boolean foc = key.equals(focused);
        commits.put(key, commit);
        Ui.roundBordered(x, y, w, h, 8, INPUT, foc ? Ui.withA(accent(), 0xB0) : Ui.hover(x, y, w, h) ? 0xFF2C3250 : CARD_BORDER);
        String value = foc ? fields.getOrDefault(key, "") : shown;
        if (value.isEmpty() && !foc) Ui.text(placeholder, F.SANS, 12, x + 12, y + h / 2, TEXT3);
        else {
            String vis = value;
            while (Ui.width(vis, F.SANS, 12) > w - 24 && vis.length() > 1) vis = foc ? vis.substring(1) : vis.substring(0, vis.length() - 1);
            Ui.text(vis + caret(foc), F.SANS, 12, x + 12, y + h / 2, TEXT);
        }
        Ui.onClick(x, y, w, h, b -> {
            focused = key;
            fields.put(key, initial);
        });
    }

    private void commitFocused() {
        if (focused == null || focused.equals("search")) {
            focused = null;
            return;
        }
        Commit c = commits.get(focused);
        String v = fields.getOrDefault(focused, "");
        focused = null;
        if (c != null) c.commit(v);
    }

    private void button(String label, String icon, float x, float y, float w, float h, boolean primary, Runnable action) {
        boolean hov = Ui.hover(x, y, w, h);
        if (primary) Ui.round(x, y, w, h, 9, hov ? Ui.mix(accent(), 0xFFFFFFFF, 0.12f) : accent());
        else Ui.roundBordered(x, y, w, h, 9, hov ? CARD_HOVER : 0xFF151929, hov ? 0xFF2C3250 : CARD_BORDER);
        float tw = Ui.width(label, F.SEMI, 13) + (icon != null ? 22 : 0);
        float tx = x + (w - tw) / 2;
        if (icon != null) {
            Ui.icon(icon, tx, y + h / 2 - 8, 16, primary ? 0xFFFFFFFF : TEXT);
            tx += 22;
        }
        Ui.text(label, F.SEMI, 13, tx, y + h / 2, primary ? 0xFFFFFFFF : TEXT);
        Ui.onClick(x, y, w, h, b -> action.run());
    }

    private void card(float x, float y, float w, float h) {
        cardInner = w - 44;
        Ui.roundBordered(x, y, w, h, 12, CARD, CARD_BORDER);
    }

    private float cardInner = 400;

    private void cardTitle(String title, String sub, float x, float y) {
        Ui.text(Ui.ellipsize(title, F.SEMI, 15, cardInner), F.SEMI, 15, x, y + 12, TEXT);
        if (sub != null) Ui.text(Ui.ellipsize(sub, F.SANS, 12, cardInner), F.SANS, 12, x, y + 32, TEXT2);
    }

    private void face(String name, float x, float y, float size) {
        Identifier tex = null;
        if (minecraft.getConnection() != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(name);
            if (info != null) tex = info.getSkin().body().texturePath();
        }
        if (tex == null) {
            if (DrevClient.get().skins().isLocalName(name) && DrevClient.get().skins().selected() != null) {
                tex = Identifier.parse("drevclient:dynamic/custom_skin");
            } else {
                tex = DefaultPlayerSkin.get(UUIDUtil.createOfflinePlayerUUID(name)).body().texturePath();
            }
        }
        Ui.round(x - 1, y - 1, size + 2, size + 2, 6, 0xFF20253A);
        Ui.textureRegion(tex, x, y, size, size, 8, 8, 8, 8, 64, 64, 0xFFFFFFFF);
        Ui.textureRegion(tex, x, y, size, size, 40, 8, 8, 8, 64, 64, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------
    // general pages
    // ------------------------------------------------------------------

    private void settingsPage(float x, float y, float w, float h) {
        bigHeader(x, y, "Client", "Settings", "General options, DrevAI and quick actions");
        float top = y + 66, vh = h - 66;
        float sy = beginScroll(x, top, w, vh), yy = sy;

        card(x, yy, w, 120);
        cardTitle("Quick actions", "Things you'll want now and then", x + 22, yy + 14);
        float bw = (w - 44 - 24) / 3;
        button("HUD editor", "hud", x + 22, yy + 64, bw, 38, false, () -> minecraft.setScreen(new HudEditorScreen(this)));
        button("Turn all modules off", "power", x + 22 + bw + 12, yy + 64, bw, 38, false, () -> {
            DrevClient.get().modules().disableAll();
            toast("Every module is off");
        });
        button("Save config now", "check", x + 22 + (bw + 12) * 2, yy + 64, bw, 38, true, () -> {
            DrevClient.get().config().save("default");
            toast("Saved");
        });
        yy += 132;

        card(x, yy, w, 70);
        float cy = yy + 35;
        Ui.text("Command prefix", F.SANS, 13, x + 22, cy, 0xFFC5C9DA);
        textField("prefix", x + w - 22 - 90, cy - 15, 90, 30, DrevClient.get().commands().getPrefix(), ".", v -> {
            DrevClient.get().commands().setPrefix(v);
            toast("Prefix is now " + DrevClient.get().commands().getPrefix());
        }, DrevClient.get().commands().getPrefix());
        yy += 82;

        for (Module m : new Module[] { DrevClient.get().modules().get(ClientSettings.class), DrevClient.get().modules().get(DrevAIModule.class) }) {
            moduleCard(m, x, yy, w, true, false);
            yy += cardHeight(m, true) + 12;
        }
        endScroll(x, top, w, vh, sy, yy);
    }

    private void themePage(float x, float y, float w, float h) {
        bigHeader(x, y, "Theme", "& Look", "Colors for the menu and HUD");
        float top = y + 66, vh = h - 66;
        float sy = beginScroll(x, top, w, vh), yy = sy;

        card(x, yy, w, 112);
        cardTitle("Accent color", "Used for highlights, switches and the HUD", x + 22, yy + 14);
        ColorSetting accent = cfg().accent;
        float px0 = x + 22;
        for (int c : PRESETS) {
            boolean sel = !accent.isRainbow() && (accent.get() & 0xFFFFFF) == (c & 0xFFFFFF);
            boolean hov = Ui.hover(px0, yy + 60, 34, 34);
            if (sel || hov) Ui.circleFill(px0 + 17, yy + 77, 17, sel ? 0xFFFFFFFF : 0x40FFFFFF);
            Ui.circleFill(px0 + 17, yy + 77, 14, c);
            Ui.onClick(px0, yy + 60, 34, 34, b -> {
                accent.set(c);
                accent.setRainbow(false);
                DrevClient.get().config().markDirty();
            });
            px0 += 44;
        }
        yy += 124;

        for (Module m : new Module[] { DrevClient.get().modules().get(ClickGuiModule.class), DrevClient.get().modules().get(HudModule.class) }) {
            moduleCard(m, x, yy, w, true, m instanceof HudModule);
            yy += cardHeight(m, true) + 12;
        }
        endScroll(x, top, w, vh, sy, yy);
    }

    private List<Path> configFiles() {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(DrevClient.get().config().getDir())) {
            s.filter(p -> p.getFileName().toString().endsWith(".json") && !p.getFileName().toString().equals("skin.json"))
                .sorted().forEach(out::add);
        } catch (Exception ignored) { }
        return out;
    }

    private void configsPage(float x, float y, float w, float h) {
        List<Path> files = configFiles();
        bigHeader(x, y, "Configs", "", files.size() + " saved  ·  .minecraft/config/drevclient");
        float top = y + 66, vh = h - 66;
        float sy = beginScroll(x, top, w, vh), yy = sy;

        card(x, yy, w, 112);
        cardTitle("Save current setup", "Modules, settings, binds, friends, HUD layout", x + 22, yy + 14);
        float fw = w - 44 - 130;
        textField("cfgname", x + 22, yy + 60, fw, 36, "", "Config name, e.g. pvp", v -> saveConfig(v), "");
        button("Save", "download", x + 22 + fw + 12, yy + 60, 118, 36, true, () -> {
            if ("cfgname".equals(focused)) commitFocused();
            else toast("Type a name first");
        });
        yy += 124;

        SimpleDateFormat fmt = new SimpleDateFormat("d MMM yyyy, HH:mm");
        for (Path p : files) {
            String name = p.getFileName().toString().replace(".json", "");
            card(x, yy, w, 62);
            Ui.text(name, F.SEMI, 15, x + 22, yy + 22, TEXT);
            String when;
            try { when = "Saved " + fmt.format(new Date(Files.getLastModifiedTime(p).toMillis())); } catch (Exception e) { when = ""; }
            Ui.text(name.equals("default") ? "Auto-saved setup  ·  " + when : when, F.SANS, 12, x + 22, yy + 42, TEXT2);
            float bx = x + w - 22;
            boolean confirm = name.equals(confirmDelete);
            float dw = confirm ? 96 : 38;
            bx -= dw;
            boolean dh = Ui.hover(bx, yy + 13, dw, 36);
            Ui.roundBordered(bx, yy + 13, dw, 36, 9, confirm ? 0x30FF5C6C : dh ? CARD_HOVER : 0xFF151929, confirm || dh ? 0x80FF5C6C : CARD_BORDER);
            if (confirm) Ui.textCenter("Delete?", F.SEMI, 12, bx + dw / 2, yy + 31, RED);
            else Ui.icon("trash", bx + 11, yy + 23, 16, dh ? RED : TEXT2);
            Ui.onClick(bx, yy + 13, dw, 36, b -> {
                if (confirm) {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) { }
                    confirmDelete = null;
                    toast("Deleted " + name);
                } else confirmDelete = name;
            });
            bx -= 98;
            button("Load", null, bx, yy + 13, 88, 36, false, () -> {
                if (DrevClient.get().config().load(name)) toast("Loaded " + name);
                else toast("Couldn't load " + name);
            });
            yy += 72;
        }
        endScroll(x, top, w, vh, sy, yy);
    }

    private void saveConfig(String raw) {
        String name = raw.replaceAll("[^a-zA-Z0-9_-]", "");
        if (name.isEmpty()) {
            toast("Use letters, numbers, - or _");
            return;
        }
        if (DrevClient.get().config().save(name)) toast("Saved " + name);
        else toast("Couldn't save");
    }

    private void socialsPage(float x, float y, float w, float h) {
        var friends = DrevClient.get().friends();
        bigHeader(x, y, "Socials", "", friends.all().size() + " friends  ·  combat modules never target friends");
        float top = y + 66, vh = h - 66;
        float sy = beginScroll(x, top, w, vh), yy = sy;

        card(x, yy, w, 112);
        cardTitle("Add a friend", "Or middle-click a player in game", x + 22, yy + 14);
        float fw = w - 44 - 150;
        Commit add = v -> {
            String n = v.trim();
            if (n.matches("[A-Za-z0-9_]{1,16}")) {
                friends.add(n);
                DrevClient.get().config().markDirty();
                toast("Added " + n);
            } else toast("That isn't a valid name");
        };
        textField("friend", x + 22, yy + 60, fw, 36, "", "Player name", add, "");
        button("Add friend", "plus", x + 22 + fw + 12, yy + 60, 138, 36, true, () -> {
            if ("friend".equals(focused)) commitFocused();
            else toast("Type a name first");
        });
        yy += 124;

        if (!friends.all().isEmpty()) {
            Ui.text("FRIENDS", F.SEMI, 11, x + 4, yy + 8, TEXT3);
            yy += 20;
            for (String f : new ArrayList<>(friends.all())) {
                card(x, yy, w, 54);
                face(f, x + 16, yy + 13, 28);
                Ui.text(f, F.SEMI, 14, x + 56, yy + 27, TEXT);
                boolean online = minecraft.getConnection() != null && minecraft.getConnection().getPlayerInfo(f) != null;
                if (online) {
                    float ox = x + 64 + Ui.width(f, F.SEMI, 14);
                    Ui.circleFill(ox, yy + 27, 3.5f, 0xFF35D07F);
                    Ui.text("online", F.SANS, 11, ox + 8, yy + 27, 0xFF35D07F);
                }
                float bx = x + w - 22 - 38;
                boolean dh = Ui.hover(bx, yy + 9, 38, 36);
                Ui.roundBordered(bx, yy + 9, 38, 36, 9, dh ? 0x30FF5C6C : 0xFF151929, dh ? 0x80FF5C6C : CARD_BORDER);
                Ui.icon("trash", bx + 11, yy + 19, 16, dh ? RED : TEXT2);
                Ui.onClick(bx, yy + 9, 38, 36, b -> {
                    friends.remove(f);
                    DrevClient.get().config().markDirty();
                });
                yy += 62;
            }
            yy += 8;
        }

        if (minecraft.getConnection() != null) {
            List<String> online = new ArrayList<>();
            for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
                String n = info.getProfile().name();
                if (!n.equalsIgnoreCase(minecraft.getUser().getName())) online.add(n);
            }
            online.sort(String.CASE_INSENSITIVE_ORDER);
            Ui.text("ON THIS SERVER  ·  " + online.size(), F.SEMI, 11, x + 4, yy + 8, TEXT3);
            yy += 20;
            for (String n : online) {
                card(x, yy, w, 54);
                face(n, x + 16, yy + 13, 28);
                Ui.text(n, F.SEMI, 14, x + 56, yy + 27, TEXT);
                float bx = x + w - 22 - 110;
                if (friends.isFriend(n)) {
                    Ui.text("Friend", F.SEMI, 12, bx + 54, yy + 27, accent());
                    Ui.icon("check", bx + 32, yy + 19, 16, accent());
                } else {
                    button("Add", "plus", bx, yy + 9, 110, 36, false, () -> {
                        friends.add(n);
                        DrevClient.get().config().markDirty();
                        toast("Added " + n);
                    });
                }
                yy += 62;
            }
        }
        endScroll(x, top, w, vh, sy, yy);
    }

    private void keybindsPage(float x, float y, float w, float h) {
        bigHeader(x, y, "Keybinds", "", "Click a key to change it  ·  right click clears it  ·  Hold = only on while held");
        headerSearch(x, y, w);
        float top = y + 66, vh = h - 66;
        float sy = beginScroll(x, top, w, vh), yy = sy;
        String q = search.toLowerCase(Locale.ROOT).trim();

        for (Module m : DrevClient.get().modules().all()) {
            if (!q.isEmpty() && !m.getName().toLowerCase(Locale.ROOT).contains(q)) continue;
            if (m instanceof ClientSettings) continue;
            boolean hov = Ui.hover(x, yy, w, 50);
            Ui.roundBordered(x, yy, w, 50, 10, hov ? CARD_HOVER : CARD, CARD_BORDER);
            Ui.text(m.getName(), F.SEMI, 14, x + 20, yy + 25, TEXT);
            Ui.text(m.getCategory().displayName, F.SANS, 11, x + 30 + Ui.width(m.getName(), F.SEMI, 14), yy + 25, TEXT3);
            float right = x + w - 18;
            float chipW = keyChip(m.getKey(), binding == m, right, yy + 25, b -> {
                if (b == 1) {
                    m.setKey(-1);
                    DrevClient.get().config().markDirty();
                } else binding = m;
            });
            float hx = right - chipW - 18 - 32;
            toggleSwitch("khold:" + m.getName(), hx, yy + 17, 32, 16, m.isHoldMode(), () -> {
                m.setHoldMode(!m.isHoldMode());
                DrevClient.get().config().markDirty();
            });
            Ui.text("Hold", F.SANS, 12, hx - 10 - Ui.width("Hold", F.SANS, 12), yy + 25, TEXT2);
            yy += 58;
        }

        // macros
        yy += 10;
        var macros = DrevClient.get().macros();
        card(x, yy, w, 112);
        cardTitle("Macros", "A key that runs chat lines, /commands or " + DrevClient.get().commands().getPrefix() + "commands (separate with ;)", x + 22, yy + 14);
        float cw = keyChip(macroKey, bindingMacro, x + 22 + 110, yy + 78, b -> bindingMacro = true);
        float fx = x + 22 + 122, fw = w - 44 - 122 - 106;
        textField("macro", fx, yy + 63, fw, 30, "", "e.g. /home; .toggle Flight", v -> {
            if (macroKey < 0) {
                toast("Pick a key first");
                return;
            }
            if (v.isBlank()) return;
            macros.set(macroKey, v.trim());
            DrevClient.get().config().markDirty();
            toast("Macro on " + KeyUtil.name(macroKey));
            macroKey = -1;
        }, "");
        button("Add", "plus", x + w - 22 - 94, yy + 61, 94, 34, true, () -> {
            if ("macro".equals(focused)) commitFocused();
            else toast("Type the macro text first");
        });
        yy += 122;
        for (var e : new ArrayList<>(macros.all().entrySet())) {
            card(x, yy, w, 50);
            String k = KeyUtil.name(e.getKey());
            float kw = Ui.width(k, F.SEMI, 12) + 24;
            Ui.roundBordered(x + 18, yy + 11, kw, 28, 7, INPUT, CARD_BORDER);
            Ui.textCenter(k, F.SEMI, 12, x + 18 + kw / 2, yy + 25, TEXT);
            Ui.text(Ui.ellipsize(e.getValue(), F.SANS, 13, w - kw - 120), F.SANS, 13, x + 32 + kw, yy + 25, 0xFFC5C9DA);
            float bx = x + w - 18 - 36;
            boolean dh = Ui.hover(bx, yy + 7, 36, 36);
            Ui.roundBordered(bx, yy + 7, 36, 36, 9, dh ? 0x30FF5C6C : 0xFF151929, dh ? 0x80FF5C6C : CARD_BORDER);
            Ui.icon("trash", bx + 10, yy + 17, 16, dh ? RED : TEXT2);
            int key = e.getKey();
            Ui.onClick(bx, yy + 7, 36, 36, b -> {
                macros.remove(key);
                DrevClient.get().config().markDirty();
            });
            yy += 58;
        }
        endScroll(x, top, w, vh, sy, yy);
    }

    // ------------------------------------------------------------------
    // skins
    // ------------------------------------------------------------------

    private void skinsPage(float x, float y, float w, float h) {
        var skins = DrevClient.get().skins();
        bigHeader(x, y, "Skins", "", "Works with cracked accounts  ·  drag a .png onto this window to add it");
        float top = y + 66, vh = h - 66;

        // preview column (not scrolled)
        float pw = Math.min(250, w * 0.38f);
        card(x, top, pw, vh);
        float prevH = Math.min(300, vh - 150);
        Ui.round(x + 14, top + 14, pw - 28, prevH, 10, 0xFF0C0F1A);
        if (minecraft.player != null) {
            float gx0 = x + 14, gy0 = top + 14;
            int gx = (int) Ui.toGui(gx0), gy = (int) Ui.toGui(gy0);
            int gw = (int) Ui.toGui(pw - 28), gh = (int) Ui.toGui(prevH);
            Ui.g.pose().pushMatrix();
            Ui.g.pose().scale(Ui.gs, Ui.gs);
            InventoryScreen.renderEntityInInventoryFollowsMouse(Ui.g, gx, gy, gx + gw, gy + gh, (int) (gh * 0.36f), 0.0625f,
                (float) Ui.toGui(Ui.mx), (float) Ui.toGui(Ui.my), minecraft.player);
            Ui.g.pose().popMatrix();
        } else {
            float fs = 96;
            face(minecraft.getUser().getName(), x + pw / 2 - fs / 2, top + 14 + prevH / 2 - fs / 2, fs);
        }
        String cur = skins.selected() == null ? "Your normal skin" : skins.selected().replace(".png", "");
        Ui.textCenter(Ui.ellipsize(cur, F.SEMI, 14, pw - 30), F.SEMI, 14, x + pw / 2, top + prevH + 34, TEXT);

        // model selector
        float my = top + prevH + 54;
        float half = (pw - 28 - 4) / 2;
        Ui.roundBordered(x + 14, my, pw - 28, 34, 9, INPUT, CARD_BORDER);
        for (int i = 0; i < 2; i++) {
            boolean slimOpt = i == 1;
            boolean sel = skins.isSlim() == slimOpt;
            float ox = x + 16 + i * (half + 2);
            if (sel) Ui.round(ox, my + 3, half, 28, 7, Ui.withA(accent(), 0x55));
            String lbl = half > 100 ? (slimOpt ? "Slim (Alex)" : "Classic (Steve)") : (slimOpt ? "Slim" : "Classic");
            Ui.textCenter(lbl, F.SEMI, 12, ox + half / 2, my + 17, sel ? TEXT : TEXT2);
            Ui.onClick(ox, my, half, 34, b -> {
                skins.setSlim(slimOpt);
                if (skins.selected() != null) skins.apply(skins.selected(), slimOpt);
            });
        }
        button("Use my normal skin", "refresh", x + 14, my + 44, pw - 28, 34, false, skins::reset);

        // right column (scrolls)
        float rx = x + pw + 16, rw = w - pw - 16;
        float sy = beginScroll(rx, top, rw, vh), yy = sy;

        boolean narrow = rw - 44 - 122 < 170;
        float addH = narrow ? 232 : 186;
        card(rx, yy, rw, addH);
        cardTitle("Add a skin", "From a file, by dragging it in, or from any premium player", rx + 22, yy + 14);
        button(skins.isBusy() ? "Working\u2026" : "Choose PNG file", "upload", rx + 22, yy + 60, rw - 44, 40, true, skins::openFilePicker);
        float fw = narrow ? rw - 44 : rw - 44 - 122;
        textField("skinname", rx + 22, yy + 112, fw, 36, "", "Player name, e.g. Dream", v -> skins.fetchByName(v), "");
        Runnable getSkin = () -> {
            if ("skinname".equals(focused)) commitFocused();
            else toast("Type a player name first");
        };
        if (narrow) button("Get skin", "download", rx + 22, yy + 156, rw - 44, 36, false, getSkin);
        else button("Get skin", "download", rx + 22 + fw + 10, yy + 112, 112, 36, false, getSkin);
        float statusY = yy + addH - 18;
        String status = skins.status();
        if (!status.isEmpty()) {
            boolean bad = status.startsWith("Couldn") || status.contains("isn't") || status.startsWith("No ") || status.contains("must be")
                || status.contains("error") || status.contains("failed") || status.contains("Enter");
            Ui.text(Ui.ellipsize(status, F.SANS, 12, rw - 44), F.SANS, 12, rx + 22, statusY, bad ? RED : accent());
        }
        yy += addH + 12;

        List<String> lib = skins.library();
        float libH = 64 + Math.max(1, (lib.size() + 3) / 4) * 120;
        card(rx, yy, rw, libH);
        cardTitle("Your skins  ·  " + lib.size(), "Click to wear  ·  saved in config/drevclient/skins", rx + 22, yy + 14);
        int cols = Math.max(2, (int) ((rw - 44 + 12) / 112));
        float tw = (rw - 44 - (cols - 1) * 12) / cols;
        if (lib.isEmpty()) Ui.text("No skins yet. Add one above.", F.SANS, 13, rx + 22, yy + 80, TEXT3);
        for (int i = 0; i < lib.size(); i++) {
            String f = lib.get(i);
            float tx = rx + 22 + (i % cols) * (tw + 12), ty = yy + 58 + (i / cols) * 120;
            boolean sel = f.equals(skins.selected());
            boolean hov = Ui.hover(tx, ty, tw, 108);
            Ui.roundBordered(tx, ty, tw, 108, 10, hov ? CARD_HOVER : 0xFF0F1220, sel ? accent() : CARD_BORDER);
            skinTile(f, tx + tw / 2 - 24, ty + 14);
            Ui.textCenter(Ui.ellipsize(f.replace(".png", ""), F.SEMI, 12, tw - 16), F.SEMI, 12, tx + tw / 2, ty + 86, sel ? TEXT : TEXT2);
            Ui.onClick(tx, ty, tw, 108, b -> skins.apply(f, skins.isSlim()));
            if (hov) {
                float bx = tx + tw - 30, by = ty + 6;
                boolean dh = Ui.hover(bx, by, 24, 24);
                Ui.round(bx, by, 24, 24, 6, dh ? 0x60FF5C6C : 0x40000000);
                Ui.icon("trash", bx + 5, by + 5, 14, dh ? 0xFFFFFFFF : TEXT2);
                Ui.onClick(bx, by, 24, 24, b -> skins.delete(f));
            }
        }
        yy += libH + 12;

        card(rx, yy, rw, 126);
        cardTitle("Who sees your skin?", null, rx + 22, yy + 14);
        List<String> info = Ui.wrap("On a cracked account only you see a custom skin. Other players see it if your server runs the "
            + "SkinsRestorer plugin: get a premium player's skin above, then press the button.", F.SANS, 12, rw - 44, 3);
        for (int i = 0; i < info.size(); i++) Ui.text(info.get(i), F.SANS, 12, rx + 22, yy + 46 + i * 16, TEXT2);
        String src = skins.sourcePlayer();
        boolean canServer = minecraft.player != null && src != null;
        button(canServer ? "Set on server: /skin set " + src : "Set on server (needs a skin fetched by name, in game)", null,
            rx + 22, yy + 86, rw - 44, 30, false, () -> {
                if (canServer) {
                    minecraft.player.connection.sendCommand("skin set " + src);
                    toast("Sent /skin set " + src);
                } else toast(minecraft.player == null ? "Join a server first" : "Get a skin by player name first");
            });
        yy += 132;
        endScroll(rx, top, rw, vh, sy, yy);
    }

    private final Map<String, Identifier> thumbs = new HashMap<>();

    /** Draws the head of a library skin (loaded once per menu open). */
    private void skinTile(String file, float x, float y) {
        Identifier id = thumbs.computeIfAbsent(file, f -> {
            try (var in = Files.newInputStream(DrevClient.get().skins().dir().resolve(f))) {
                var img = com.mojang.blaze3d.platform.NativeImage.read(in);
                if (img.getWidth() != 64) {
                    img.close();
                    return null;
                }
                Identifier tid = Identifier.parse("drevclient:dynamic/thumb_" + Integer.toHexString(f.hashCode()));
                minecraft.getTextureManager().register(tid, new net.minecraft.client.renderer.texture.DynamicTexture(tid::toString, img));
                return tid;
            } catch (Exception e) {
                return null;
            }
        });
        if (id == null) {
            Ui.round(x, y, 48, 48, 6, 0xFF20253A);
            return;
        }
        Ui.round(x - 1, y - 1, 50, 50, 7, 0xFF20253A);
        Ui.textureRegion(id, x, y, 48, 48, 8, 8, 8, 8, 64, 64, 0xFFFFFFFF);
        Ui.textureRegion(id, x, y, 48, 48, 40, 8, 8, 8, 64, 64, 0xFFFFFFFF);
    }

    @Override
    public void removed() {
        for (Identifier id : thumbs.values()) if (id != null) minecraft.getTextureManager().release(id);
        thumbs.clear();
        super.removed();
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        page = Page.SKINS;
        DrevClient.get().skins().importFiles(paths);
        thumbs.clear();
    }

    // ==================================================================
    // input
    // ==================================================================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double vx = Ui.toVirtual(event.x()), vy = Ui.toVirtual(event.y());
        String wasFocused = focused;
        if (binding != null) {
            // mouse buttons can't be bound here; cancel binding
            binding = null;
            return true;
        }
        if (bindingMacro) {
            bindingMacro = false;
            return true;
        }
        if (focused != null && !focused.equals("search")) commitFocused();
        else if ("search".equals(focused)) focused = null;
        String confirmBefore = confirmDelete;
        boolean hit = Ui.dispatchClick(vx, vy, event.button());
        if (confirmBefore != null && confirmBefore.equals(confirmDelete)) confirmDelete = null;
        if (!hit && wasFocused != null) focused = null;
        return hit || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (slider != null || hueDrag != null) DrevClient.get().config().markDirty();
        slider = null;
        hueDrag = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {
        float cur = SCROLL.getOrDefault(page, 0f);
        SCROLL.put(page, Math.max(0, cur - (float) v * 60));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        boolean ctrl = (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;

        if (binding != null) {
            binding.setKey(key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE ? -1 : key);
            toast(binding.getName() + (binding.getKey() < 0 ? " unbound" : " → " + KeyUtil.name(binding.getKey())));
            binding = null;
            DrevClient.get().config().markDirty();
            return true;
        }
        if (bindingMacro) {
            macroKey = key == GLFW.GLFW_KEY_ESCAPE ? -1 : key;
            bindingMacro = false;
            return true;
        }
        if (focused != null) {
            String cur = focused.equals("search") ? search : fields.getOrDefault(focused, "");
            String next = cur;
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                commitFocused();
                return true;
            } else if (key == GLFW.GLFW_KEY_ESCAPE) {
                focused = null;
                return true;
            } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (ctrl) next = "";
                else if (!cur.isEmpty()) next = cur.substring(0, cur.offsetByCodePoints(cur.length(), -1));
            } else if (ctrl && key == GLFW.GLFW_KEY_V) {
                next = cur + minecraft.keyboardHandler.getClipboard().replace("\n", " ").replace("\r", "").trim();
            } else if (ctrl && key == GLFW.GLFW_KEY_A) {
                next = "";
            } else {
                return true;
            }
            setFocusedText(next);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (key == cfg().getKey() && key != -1 && System.currentTimeMillis() - openedAt > 250) {
            onClose();
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_F) {
            focused = "search";
            return true;
        }
        return super.keyPressed(event);
    }

    private void setFocusedText(String value) {
        if (focused == null) return;
        if (focused.equals("search")) {
            search = value;
            SCROLL.put(page, 0f);
        } else fields.put(focused, value);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        String ch = event.codepointAsString();
        if (focused == null) {
            if ((page == Page.CATEGORY || page == Page.KEYBINDS) && event.isAllowedChatCharacter() && !ch.isBlank()) {
                focused = "search";
            } else return false;
        }
        if (!event.isAllowedChatCharacter()) return true;
        String cur = focused.equals("search") ? search : fields.getOrDefault(focused, "");
        if (cur.length() < 200) setFocusedText(cur + ch);
        return true;
    }

    @Override
    public void onClose() {
        if (focused != null && !focused.equals("search")) commitFocused();
        DrevClient.get().config().save("default");
        super.onClose();
    }

    // ==================================================================
    // persistence + test hooks
    // ==================================================================

    /** Opens a module's settings (used by the automated test). */
    public static void expand(String moduleName) { EXPANDED.add(moduleName); }

    /** Shows a page by name (used by the automated test). */
    public static void showPage(String name) {
        for (Page p : Page.values()) if (p.name().equalsIgnoreCase(name)) page = p;
    }

    public static void showCategory(Category c) {
        page = Page.CATEGORY;
        category = c;
    }

    public static void setGrid(boolean value) { grid = value; }

    public static JsonObject savePanels() {
        JsonObject o = new JsonObject();
        o.addProperty("page", page.name());
        o.addProperty("category", category.name());
        o.addProperty("grid", grid);
        JsonArray exp = new JsonArray();
        EXPANDED.forEach(exp::add);
        o.add("expanded", exp);
        return o;
    }

    public static void loadPanels(JsonObject o) {
        try {
            if (o.has("page")) page = Page.valueOf(o.get("page").getAsString());
            if (o.has("category")) category = Category.valueOf(o.get("category").getAsString());
            if (o.has("grid")) grid = o.get("grid").getAsBoolean();
            if (o.has("expanded")) {
                EXPANDED.clear();
                for (JsonElement e : o.getAsJsonArray("expanded")) EXPANDED.add(e.getAsString());
            }
            if (category == Category.CLIENT) category = Category.COMBAT;
        } catch (Exception ignored) { }
    }
}

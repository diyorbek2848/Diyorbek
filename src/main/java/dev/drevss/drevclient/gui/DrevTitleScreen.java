package dev.drevss.drevclient.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.module.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * DrevClient main menu: panorama (or your own background.png), shield logo,
 * a column of buttons, a profile card, a news panel and a status bar.
 */
public class DrevTitleScreen extends Screen {
    private static final Identifier LOGO = Identifier.parse("drevclient:textures/gui/logo.png");
    private static final Identifier WORDMARK = Identifier.parse("drevclient:textures/gui/wordmark.png");
    private static final Identifier CUSTOM_BG = Identifier.parse("drevclient:dynamic/background");

    private static final int CYAN = 0xFF3FE0FF;
    private static boolean bgLoaded;
    private static boolean hasCustomBg;
    private static int bgW, bgH;
    private static Identifier skin;

    private record MenuButton(String icon, String label, int color, Runnable action) { }
    private record Hit(int x, int y, int w, int h, Runnable action) { }

    private final List<MenuButton> buttons = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private long openedAt;

    public DrevTitleScreen() {
        super(Component.literal("DrevClient"));
    }

    @Override
    protected void init() {
        openedAt = System.currentTimeMillis();
        buttons.clear();
        buttons.add(new MenuButton("▶", "START GAME", CYAN, () -> minecraft.setScreen(new SelectWorldScreen(this))));
        buttons.add(new MenuButton("☰", "MULTIPLAYER", CYAN, () -> minecraft.setScreen(new JoinMultiplayerScreen(this))));
        buttons.add(new MenuButton("⚒", "DREV MODS", CYAN, () -> minecraft.setScreen(new ClickGuiScreen())));
        buttons.add(new MenuButton("✦", "DREV AI", CYAN, () -> minecraft.setScreen(new DrevAIScreen())));
        buttons.add(new MenuButton("⚙", "OPTIONS", CYAN, () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
        buttons.add(new MenuButton("⏻", "EXIT", 0xFFFF5A5A, () -> minecraft.stop()));
        loadBackground();
        loadSkin();
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    // ------------------------------------------------------------------
    // assets
    // ------------------------------------------------------------------

    /** Optional custom background: .minecraft/config/drevclient/background.png */
    private void loadBackground() {
        if (bgLoaded) return;
        bgLoaded = true;
        Path file = DrevClient.get().config().getDir().resolve("background.png");
        if (!Files.exists(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            bgW = image.getWidth();
            bgH = image.getHeight();
            minecraft.getTextureManager().register(CUSTOM_BG, new DynamicTexture(CUSTOM_BG::toString, image));
            hasCustomBg = true;
        } catch (Exception e) {
            DrevClient.LOGGER.warn("Could not load background.png", e);
        }
    }

    private void loadSkin() {
        if (skin != null) return;
        GameProfile profile = minecraft.getGameProfile();
        try {
            skin = new PlayerInfo(profile, false).getSkin().body().texturePath();
        } catch (Exception ignored) { }
        minecraft.getSkinManager().get(profile).thenAccept(opt ->
            opt.ifPresent(s -> minecraft.execute(() -> skin = s.body().texturePath())));
    }

    // ------------------------------------------------------------------
    // rendering
    // ------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        if (hasCustomBg) {
            // cover the screen while keeping the image's aspect ratio
            float scale = Math.max(width / (float) bgW, height / (float) bgH);
            int w = (int) (bgW * scale), h = (int) (bgH * scale);
            g.blit(RenderPipelines.GUI_TEXTURED, CUSTOM_BG, (width - w) / 2, (height - h) / 2, 0f, 0f, w, h, w, h);
        } else {
            renderPanorama(g, pt);
        }
        g.fillGradient(0, 0, width, height / 3, 0x50000000, 0x00000000);
        g.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0x90000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        hits.clear();
        float fade = Math.min(1f, (System.currentTimeMillis() - openedAt) / 400f);
        int cx = width / 2;

        // logo + wordmark (textures are 2x for sharpness)
        int logoSize = Math.min(72, height / 5);
        int logoY = Math.max(6, height / 2 - 132);
        g.pose().pushMatrix();
        g.pose().translate(cx - logoSize / 2f, logoY);
        g.pose().scale(logoSize / 256f, logoSize / 256f);
        g.blit(RenderPipelines.GUI_TEXTURED, LOGO, 0, 0, 0f, 0f, 256, 256, 256, 256);
        g.pose().popMatrix();

        int wmW = 160, wmH = 25;
        int wmY = logoY + logoSize + 2;
        g.pose().pushMatrix();
        g.pose().translate(cx - wmW / 2f, wmY);
        g.pose().scale(wmW / 512f, wmH / 80f);
        g.blit(RenderPipelines.GUI_TEXTURED, WORDMARK, 0, 0, 0f, 0f, 512, 80, 512, 80);
        g.pose().popMatrix();

        String sub = "MINECRAFT MODERN EXPERIENCE ";
        String ver = "v" + DrevClient.VERSION;
        int subW = font.width(sub) + font.width(ver);
        int subY = wmY + wmH + 1;
        g.pose().pushMatrix();
        g.pose().translate(cx, subY);
        g.pose().scale(0.75f, 0.75f);
        g.drawString(font, sub, -subW / 2, 0, 0xFFFFFFFF, true);
        g.drawString(font, ver, -subW / 2 + font.width(sub), 0, CYAN, true);
        g.pose().popMatrix();

        // button column
        int bw = 150, bh = 18, gap = 4;
        int by = subY + 14;
        for (MenuButton b : buttons) {
            int bx = cx - bw / 2;
            boolean hover = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
            drawButton(g, bx, by, bw, bh, b, hover, fade);
            hits.add(new Hit(bx, by, bw, bh, b.action()));
            by += bh + gap;
        }

        // right side panels
        // panels shrink on small windows and hide if they would cover the buttons
        int free = width - (cx + bw / 2) - 16;
        int pw = Math.min(140, free);
        if (pw >= 104) {
            int px = width - pw - 8;
            int py = Math.max(8, height / 2 - 70);
            drawProfile(g, px, py, pw);
            if (py + 62 + 90 < height - 20) drawNews(g, px, py + 62, pw);
        }

        // status bar
        String name = minecraft.getUser().getName();
        long active = DrevClient.get().modules().all().stream().filter(Module::isEnabled).count();
        String status = "§7Logged in as: §b" + name + " §8| §7Status: §aOnline §8| §7Version: §f"
            + DrevClient.VERSION + " §8| §7Minecraft 1.21.11";
        g.fill(0, height - 12, font.width(status) + 8, height, 0x80000000);
        g.drawString(font, status, 4, height - 10, 0xFFFFFFFF, false);

        // quick buttons bottom right
        int qx = width - 8;
        qx = quickButton(g, qx, "✦", mouseX, mouseY, () -> minecraft.setScreen(new DrevAIScreen()));
        qx = quickButton(g, qx, "⚙", mouseX, mouseY, () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options)));
        quickButton(g, qx, "⚒", mouseX, mouseY, () -> minecraft.setScreen(new ClickGuiScreen()));
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, MenuButton b, boolean hover, float fade) {
        int alpha = (int) (fade * 255);
        int bg = hover ? 0xD0145A70 : 0xB00B1F2A;
        g.fill(x, y, x + w, y + h, withAlpha(bg, (int) (((bg >>> 24) & 0xFF) * fade)));
        int border = hover ? 0xFFFFFFFF : b.color();
        border = withAlpha(border, hover ? alpha : (int) (alpha * 0.85f));
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
        if (hover) g.fillGradient(x + 1, y + 1, x + w - 1, y + h / 2, 0x40FFFFFF, 0x00FFFFFF);
        // icon box
        g.fill(x + 2, y + 2, x + 16, y + h - 2, withAlpha(b.color(), hover ? 120 : 60));
        g.drawCenteredString(font, b.icon(), x + 9, y + (h - 8) / 2, withAlpha(0xFFFFFF, alpha));
        int labelColor = b.color() == CYAN ? 0xFFFFFF : 0xFFC0C0;
        g.drawCenteredString(font, "§l" + b.label(), x + w / 2 + 6, y + (h - 8) / 2, withAlpha(labelColor, alpha));
    }

    private void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xC00B1A24);
        g.fill(x, y, x + w, y + 1, 0xFF2AB7D6);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF16627A);
        g.fill(x, y, x + 1, y + h, 0xFF16627A);
        g.fill(x + w - 1, y, x + w, y + h, 0xFF16627A);
    }

    private void drawProfile(GuiGraphics g, int x, int y, int w) {
        int h = 56;
        panel(g, x, y, w, h);
        int face = 32;
        g.fill(x + 5, y + 5, x + 7 + face, y + 7 + face, 0xFF2AB7D6);
        if (skin != null) {
            int s = face / 8;
            g.blit(RenderPipelines.GUI_TEXTURED, skin, x + 6, y + 6, 8f * s, 8f * s, face, face, 64 * s, 64 * s);
            g.blit(RenderPipelines.GUI_TEXTURED, skin, x + 6, y + 6, 40f * s, 8f * s, face, face, 64 * s, 64 * s);
        }
        int tx = x + face + 12;
        long active = DrevClient.get().modules().all().stream().filter(Module::isEnabled).count();
        g.drawString(font, minecraft.getUser().getName(), tx, y + 7, 0xFFFFFFFF, true);
        g.drawString(font, "Modules: §b" + DrevClient.get().modules().all().size(), tx, y + 18, 0xFFB0C4CC, false);
        g.drawString(font, "Active: §a" + active, tx, y + 28, 0xFFB0C4CC, false);
        g.drawString(font, "Friends: §e" + DrevClient.get().friends().all().size(), x + 6, y + 44, 0xFFB0C4CC, false);
    }

    private void drawNews(GuiGraphics g, int x, int y, int w) {
        String[] news = {
            "§bDREV CLIENT v" + DrevClient.VERSION + "\n§fNOW LIVE!",
            "§fDrevAI: §7press J",
            "§fRight Shift = ClickGUI",
        };
        int h = 12 + news.length * 6;
        for (String n : news) h += n.contains("\n") ? 20 : 12;
        panel(g, x, y, w, h);
        g.drawString(font, "News", x + 5, y + 4, 0xFF9FD8E6, false);
        int ny = y + 15;
        for (String n : news) {
            String[] lines = n.split("\n");
            int ih = lines.length * 10 + 2;
            g.fill(x + 5, ny, x + w - 5, ny + ih, 0x9010303E);
            g.fill(x + 5, ny, x + w - 5, ny + 1, 0x802AB7D6);
            for (int i = 0; i < lines.length; i++) g.drawCenteredString(font, lines[i], x + w / 2, ny + 2 + i * 10, 0xFFFFFFFF);
            ny += ih + 4;
        }
    }

    private int quickButton(GuiGraphics g, int right, String icon, int mx, int my, Runnable action) {
        int s = 14;
        int x = right - s, y = height - s - 3;
        boolean hover = mx >= x && mx < x + s && my >= y && my < y + s;
        g.fill(x, y, x + s, y + s, hover ? 0xE0145A70 : 0xC00B1A24);
        g.fill(x, y, x + s, y + 1, 0xFF2AB7D6);
        g.drawCenteredString(font, icon, x + s / 2 + 1, y + 3, 0xFFFFFFFF);
        hits.add(new Hit(x, y, s, s, action));
        return x - 3;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (Hit h : hits) {
                if (event.x() >= h.x() && event.x() < h.x() + h.w() && event.y() >= h.y() && event.y() < h.y() + h.h()) {
                    h.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
    }
}

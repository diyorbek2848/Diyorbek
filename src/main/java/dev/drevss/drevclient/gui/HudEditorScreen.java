package dev.drevss.drevclient.gui;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.hud.HudElement;
import dev.drevss.drevclient.module.client.ClickGuiModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Drag HUD elements around; right click hides/shows one. */
public class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudElement dragging;
    private int offX, offY;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        g.fill(0, 0, width, height, 0x50000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        int accent = DrevClient.get().modules().get(ClickGuiModule.class).accent.getColor();
        if (dragging != null) dragging.setPosition(mouseX - offX, mouseY - offY);

        List<HudElement> elements = DrevClient.get().hud().elements();
        for (HudElement e : elements) {
            if (minecraft.player != null) e.render(g, pt, true);
            int x = e.drawX(), y = e.drawY();
            boolean hover = e.isHovered(mouseX, mouseY);
            int border = !e.isVisible() ? 0x80FF5555 : hover ? accent : 0x80FFFFFF;
            g.fill(x - 2, y - 2, x + e.getWidth() + 2, y - 1, border);
            g.fill(x - 2, y + e.getHeight() + 1, x + e.getWidth() + 2, y + e.getHeight() + 2, border);
            g.fill(x - 2, y - 2, x - 1, y + e.getHeight() + 2, border);
            g.fill(x + e.getWidth() + 1, y - 2, x + e.getWidth() + 2, y + e.getHeight() + 2, border);
            if (!e.isVisible()) g.fill(x, y, x + e.getWidth(), y + e.getHeight(), 0x60200000);
            if (hover) g.drawString(font, e.getName() + (e.isVisible() ? "" : " (hidden)"), x, y - 11, 0xFFFFFFFF, true);
        }

        String help = "Drag to move • Right click to hide/show • Esc to finish";
        g.drawString(font, help, width / 2 - font.width(help) / 2, height / 2 - 4, 0xFFAAAAAA, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        List<HudElement> elements = DrevClient.get().hud().elements();
        for (int i = elements.size() - 1; i >= 0; i--) {
            HudElement e = elements.get(i);
            if (!e.isHovered(event.x(), event.y())) continue;
            if (event.button() == 1) {
                e.setVisible(!e.isVisible());
            } else {
                dragging = e;
                offX = (int) event.x() - e.drawX();
                offY = (int) event.y() - e.drawY();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        DrevClient.get().config().save("default");
        minecraft.setScreen(parent);
    }
}

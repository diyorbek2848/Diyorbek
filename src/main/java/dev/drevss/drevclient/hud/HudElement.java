package dev.drevss.drevclient.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public abstract class HudElement {
    protected static final Minecraft mc = Minecraft.getInstance();

    /**
     * Position convention: x/y >= 0 are pixels from the left/top. Negative
     * values anchor to the right/bottom edge (-1 = flush). CENTER centers x.
     */
    public static final int CENTER = Integer.MIN_VALUE;

    private final String name;
    private int x, y;
    private boolean visible;
    protected int width = 20, height = 10;

    protected HudElement(String name, int x, int y, boolean visible) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.visible = visible;
    }

    /** Draws the element at its position and updates width/height. */
    public abstract void render(GuiGraphics g, float pt, boolean editing);

    public String getName() { return name; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Resolves anchors and keeps the element on screen after resizing. */
    public int drawX() {
        int sw = mc.getWindow().getGuiScaledWidth();
        int rx = x == CENTER ? (sw - width) / 2 : x < 0 ? sw - width + x + 1 - 2 : x;
        return Math.max(0, Math.min(rx, sw - width));
    }

    public int drawY() {
        int sh = mc.getWindow().getGuiScaledHeight();
        int ry = y < 0 ? sh - height + y + 1 - 1 : y;
        return Math.max(0, Math.min(ry, sh - height));
    }

    /** True when the element sits on the right half (text aligns right). */
    protected boolean rightAligned() {
        return drawX() + width / 2 > mc.getWindow().getGuiScaledWidth() / 2;
    }

    public boolean isHovered(double mx, double my) {
        return mx >= drawX() && mx <= drawX() + width && my >= drawY() && my <= drawY() + height;
    }
}

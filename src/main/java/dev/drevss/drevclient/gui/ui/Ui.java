package dev.drevss.drevclient.gui.ui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immediate-mode drawing helpers for the DrevClient GUI.
 *
 * Layout uses "virtual pixels"; one virtual pixel is {@link #u} real screen
 * pixels (1, 1.5 or 2 depending on window size). Everything is drawn in real
 * pixels so rounded corners and the TrueType fonts stay sharp at any GUI scale.
 */
public final class Ui {
    private static final Minecraft mc = Minecraft.getInstance();

    public static GuiGraphics g;
    public static float u = 1f;
    public static int gs = 1;
    public static float vw, vh;          // screen size in virtual px
    public static float mx, my;          // mouse in virtual px
    private static long lastFrame;
    public static float dt;              // seconds since last frame

    private Ui() { }

    // ------------------------------------------------------------------
    // frame
    // ------------------------------------------------------------------

    public static void begin(GuiGraphics graphics, double guiMouseX, double guiMouseY) {
        g = graphics;
        gs = Math.max(1, mc.getWindow().getGuiScale());
        int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        u = h < 850 ? 1f : h < 1250 ? 1.5f : 2f;
        while (u > 1f && w / u < 1000) u -= 0.5f;
        vw = w / u;
        vh = h / u;
        mx = (float) toVirtual(guiMouseX);
        my = (float) toVirtual(guiMouseY);
        long now = System.nanoTime();
        dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        hits.clear();
        g.pose().pushMatrix();
        g.pose().scale(1f / gs, 1f / gs);
    }

    public static void end() {
        g.pose().popMatrix();
    }

    public static double toVirtual(double gui) { return gui * gs / u; }
    public static double toGui(double virtual) { return virtual * u / gs; }
    public static int p(float v) { return Math.round(v * u); }

    // ------------------------------------------------------------------
    // colors
    // ------------------------------------------------------------------

    public static int alpha(int color, float a) {
        int base = (color >>> 24) & 0xFF;
        return ((int) (base * Math.max(0, Math.min(1, a))) << 24) | (color & 0xFFFFFF);
    }

    public static int withA(int color, int a) { return (Math.max(0, Math.min(255, a)) << 24) | (color & 0xFFFFFF); }

    public static int mix(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a = (int) (((c1 >>> 24) & 0xFF) * (1 - t) + ((c2 >>> 24) & 0xFF) * t);
        int r = (int) (((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int gr = (int) (((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    // ------------------------------------------------------------------
    // shapes
    // ------------------------------------------------------------------

    public static void rect(float x, float y, float w, float h, int color) {
        g.fill(p(x), p(y), p(x + w), p(y + h), color);
    }

    public static void gradientV(float x, float y, float w, float h, int top, int bottom) {
        g.fillGradient(p(x), p(y), p(x + w), p(y + h), top, bottom);
    }

    private static final Map<Integer, Identifier> CIRCLES = new HashMap<>();

    /** Anti-aliased white disk with a radius of r real pixels. */
    private static Identifier circle(int r) {
        return CIRCLES.computeIfAbsent(r, radius -> {
            int d = radius * 2;
            BufferedImage img = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB);
            int ss = 4;
            for (int y = 0; y < d; y++) {
                for (int x = 0; x < d; x++) {
                    int inside = 0;
                    for (int sy = 0; sy < ss; sy++) {
                        for (int sx = 0; sx < ss; sx++) {
                            double px = x + (sx + 0.5) / ss - radius, py = y + (sy + 0.5) / ss - radius;
                            if (px * px + py * py <= radius * radius) inside++;
                        }
                    }
                    int a = inside * 255 / (ss * ss);
                    img.setRGB(x, y, (a << 24) | 0xFFFFFF);
                }
            }
            Identifier id = Identifier.parse("drevclient:dynamic/circle_" + radius);
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(img, "png", out);
                NativeImage ni = NativeImage.read(new ByteArrayInputStream(out.toByteArray()));
                mc.getTextureManager().register(id, new DynamicTexture(id::toString, ni));
            } catch (Exception e) {
                throw new RuntimeException("Could not create circle texture", e);
            }
            return id;
        });
    }

    /** Rounded rectangle in real pixels. */
    private static void roundPx(int x0, int y0, int x1, int y1, int r, int color) {
        if (x1 <= x0 || y1 <= y0 || ((color >>> 24) & 0xFF) == 0) return;
        r = Math.min(r, Math.min((x1 - x0) / 2, (y1 - y0) / 2));
        if (r <= 0) {
            g.fill(x0, y0, x1, y1, color);
            return;
        }
        Identifier tex = circle(r);
        int d = r * 2;
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x0, y0, 0, 0, r, r, d, d, color);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x1 - r, y0, r, 0, r, r, d, d, color);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x0, y1 - r, 0, r, r, r, d, d, color);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x1 - r, y1 - r, r, r, r, r, d, d, color);
        g.fill(x0 + r, y0, x1 - r, y1, color);
        g.fill(x0, y0 + r, x0 + r, y1 - r, color);
        g.fill(x1 - r, y0 + r, x1, y1 - r, color);
    }

    public static void round(float x, float y, float w, float h, float r, int color) {
        roundPx(p(x), p(y), p(x + w), p(y + h), p(r), color);
    }

    /** Rounded rectangle with a 1 px border. */
    public static void roundBordered(float x, float y, float w, float h, float r, int fill, int border) {
        int x0 = p(x), y0 = p(y), x1 = p(x + w), y1 = p(y + h), rr = p(r);
        roundPx(x0, y0, x1, y1, rr, border);
        roundPx(x0 + 1, y0 + 1, x1 - 1, y1 - 1, Math.max(0, rr - 1), fill);
    }

    public static void circleFill(float cx, float cy, float r, int color) {
        round(cx - r, cy - r, r * 2, r * 2, r, color);
    }

    /** Soft drop shadow made of stacked translucent rounded rects. */
    public static void shadow(float x, float y, float w, float h, float r, int strength) {
        for (int i = 6; i >= 1; i--) {
            float s = i * 3;
            round(x - s, y - s + 4, w + s * 2, h + s * 2, r + s, withA(0x000000, strength / (i + 1)));
        }
    }

    public static void texture(Identifier tex, float x, float y, float w, float h, int texW, int texH, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, p(x), p(y), 0, 0, p(x + w) - p(x), p(y + h) - p(y), texW, texH, texW, texH, color);
    }

    /** Draws part of a texture (u, v, regionW, regionH in texture pixels) stretched over a virtual rect. */
    public static void textureRegion(Identifier tex, float x, float y, float w, float h, float tu, float tv, int rw, int rh,
                                     int texW, int texH, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, p(x), p(y), tu, tv, p(x + w) - p(x), p(y + h) - p(y), rw, rh, texW, texH, color);
    }

    private static final Identifier HFADE = Identifier.parse("drevclient:textures/gui/hfade.png");
    private static final Identifier HUE = Identifier.parse("drevclient:textures/gui/hue.png");

    /** Horizontal fade from color (left) to transparent (right). */
    public static void fadeH(float x, float y, float w, float h, int color) {
        texture(HFADE, x, y, w, h, 256, 1, color);
    }

    public static void hueBar(float x, float y, float w, float h) {
        texture(HUE, x, y, w, h, 256, 1, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------
    // icons
    // ------------------------------------------------------------------

    /** 16 virtual px icon from textures/gui/icons/{name}_{16|24|32}.png */
    public static void icon(String name, float x, float y, float size, int color) {
        int px = p(x + size) - p(x);
        int file = px <= 18 ? 16 : px <= 26 ? 24 : 32;
        Identifier id = Identifier.parse("drevclient:textures/gui/icons/" + name + "_" + file + ".png");
        g.blit(RenderPipelines.GUI_TEXTURED, id, p(x), p(y), 0, 0, px, px, file, file, file, file, color);
    }

    // ------------------------------------------------------------------
    // text
    // ------------------------------------------------------------------

    public enum F {
        SANS("sans", 0.538f), SEMI("semi", 0.538f), BOLD("bold", 0.538f), SERIF("serif", 0.56f);

        final String id;
        final float cap; // cap height / size

        F(String id, float cap) {
            this.id = id;
            this.cap = cap;
        }
    }

    /** Vertical offset between the y passed to drawString and the text baseline, in font units. */
    public static float baselineOffset = 7f;

    private static final Map<String, FontDescription> FONTS = new HashMap<>();

    public static Component comp(String s, F f, int size) {
        String tag = u <= 1f ? "10" : u <= 1.5f ? "15" : "20";
        String key = f.id + size + "_" + tag;
        FontDescription fd = FONTS.computeIfAbsent(key, k -> new FontDescription.Resource(Identifier.parse("drevclient:" + k)));
        return Component.literal(s).withStyle(st -> st.withFont(fd));
    }

    public static float width(String s, F f, int size) {
        return mc.font.width(comp(s, f, size));
    }

    /** Draws text with its baseline at y. */
    public static void textBase(String s, F f, int size, float x, float baseline, int color) {
        g.pose().pushMatrix();
        g.pose().translate(p(x), Math.round((baseline - baselineOffset) * u));
        g.pose().scale(u, u);
        g.drawString(mc.font, comp(s, f, size), 0, 0, color | 0x01000000, false);
        g.pose().popMatrix();
    }

    /** Draws text vertically centered (by cap height) on cy. */
    public static void text(String s, F f, int size, float x, float cy, int color) {
        textBase(s, f, size, x, cy + f.cap * size * 0.5f * (size >= 20 ? 1.0f : 1.0f) + capFix(f, size), color);
    }

    private static float capFix(F f, int size) { return 0; }

    public static void textRight(String s, F f, int size, float right, float cy, int color) {
        text(s, f, size, right - width(s, f, size), cy, color);
    }

    public static void textCenter(String s, F f, int size, float cx, float cy, int color) {
        text(s, f, size, cx - width(s, f, size) / 2, cy, color);
    }

    /** Cuts the string to fit maxWidth, adding an ellipsis. */
    public static String ellipsize(String s, F f, int size, float maxWidth) {
        if (width(s, f, size) <= maxWidth) return s;
        String e = "…";
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (width(s.substring(0, mid) + e, f, size) <= maxWidth) lo = mid; else hi = mid - 1;
        }
        return s.substring(0, lo) + e;
    }

    public static List<String> wrap(String s, F f, int size, float maxWidth, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : s.split(" ")) {
            String cand = cur.length() == 0 ? word : cur + " " + word;
            if (width(cand, f, size) <= maxWidth) {
                cur.setLength(0);
                cur.append(cand);
            } else {
                if (cur.length() > 0) lines.add(cur.toString());
                cur.setLength(0);
                cur.append(word);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        if (lines.size() > maxLines) {
            List<String> cut = new ArrayList<>(lines.subList(0, maxLines));
            cut.set(maxLines - 1, ellipsize(cut.get(maxLines - 1) + " " + lines.get(maxLines), f, size, maxWidth));
            return cut;
        }
        return lines;
    }

    // ------------------------------------------------------------------
    // clipping
    // ------------------------------------------------------------------

    private static final List<float[]> CLIPS = new ArrayList<>();

    public static void clip(float x, float y, float w, float h) {
        CLIPS.add(new float[] { x, y, w, h });
        g.enableScissor(p(x), p(y), p(x + w), p(y + h));
    }

    public static void unclip() {
        if (!CLIPS.isEmpty()) CLIPS.remove(CLIPS.size() - 1);
        g.disableScissor();
    }

    private static boolean insideClip(float x, float y) {
        for (float[] c : CLIPS) {
            if (x < c[0] || y < c[1] || x >= c[0] + c[2] || y >= c[1] + c[3]) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // input
    // ------------------------------------------------------------------

    public interface ClickHandler { void click(int button); }

    public record Hit(float x, float y, float w, float h, ClickHandler handler) { }

    private static final List<Hit> hits = new ArrayList<>();

    public static boolean hover(float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h && insideClip(mx, my);
    }

    /** Registers a clickable area for this frame (later registrations win). */
    public static void onClick(float x, float y, float w, float h, ClickHandler handler) {
        hits.add(new Hit(x, y, w, h, handler));
        // remember which clip was active so clipped-away parts can't be clicked
        if (!CLIPS.isEmpty()) {
            float[] c = CLIPS.get(CLIPS.size() - 1);
            float nx = Math.max(x, c[0]), ny = Math.max(y, c[1]);
            float nw = Math.min(x + w, c[0] + c[2]) - nx, nh = Math.min(y + h, c[1] + c[3]) - ny;
            hits.set(hits.size() - 1, new Hit(nx, ny, Math.max(0, nw), Math.max(0, nh), handler));
        }
    }

    public static boolean dispatchClick(double vx, double vy, int button) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (vx >= h.x() && vy >= h.y() && vx < h.x() + h.w() && vy < h.y() + h.h()) {
                h.handler().click(button);
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // animation
    // ------------------------------------------------------------------

    private static final Map<String, Float> ANIMS = new HashMap<>();

    /** Smoothly moves a named value toward target. */
    public static float anim(String key, float target, float speed) {
        float cur = ANIMS.getOrDefault(key, target);
        cur += (target - cur) * Math.min(1f, dt * speed);
        if (Math.abs(target - cur) < 0.001f) cur = target;
        ANIMS.put(key, cur);
        return cur;
    }
}

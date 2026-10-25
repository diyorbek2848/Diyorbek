package dev.drevss.drevclient.gui;

import dev.drevss.drevclient.DrevClient;
import dev.drevss.drevclient.ai.DrevAI;
import dev.drevss.drevclient.module.client.ClickGuiModule;
import dev.drevss.drevclient.module.client.DrevAIModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Chat window for DrevAI. Lines starting with ">> ." become clickable commands. */
public class DrevAIScreen extends Screen {
    private EditBox input;
    private int scroll;
    private final List<Clickable> clickables = new ArrayList<>();

    private record Clickable(int x, int y, int w, String command) { }
    private record Line(String text, int color, String command) { }

    public DrevAIScreen() {
        super(Component.literal("DrevAI"));
    }

    private int left() { return Math.max(10, width / 2 - 200); }
    private int right() { return Math.min(width - 10, width / 2 + 200); }
    private int accent() { return DrevClient.get().modules().get(ClickGuiModule.class).accent.getColor(); }

    @Override
    protected void init() {
        input = new EditBox(font, left() + 4, height - 30, right() - left() - 8, 18, Component.literal("Ask DrevAI"));
        input.setMaxLength(1000);
        input.setHint(Component.literal("Ask anything... (Enter to send)"));
        addRenderableWidget(input);
        setInitialFocus(input);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        g.fill(0, 0, width, height, 0x90000000);
    }

    private List<Line> buildLines(int maxWidth) {
        Font f = font;
        String prefix = DrevClient.get().commands().getPrefix();
        List<Line> out = new ArrayList<>();
        for (DrevAI.ChatLine cl : DrevClient.get().ai().transcript()) {
            out.add(new Line(cl.fromUser() ? "§bYou" : "§dDrevAI", 0xFFFFFFFF, null));
            for (String para : cl.text().split("\n")) {
                String trimmed = para.trim();
                if (!cl.fromUser() && trimmed.startsWith(">> ")) {
                    String cmd = trimmed.substring(3).trim();
                    if (cmd.startsWith(prefix)) {
                        out.add(new Line("▶ " + cmd, accent(), cmd));
                        continue;
                    }
                }
                for (String wrapped : wrap(f, para.replace("**", ""), maxWidth)) {
                    out.add(new Line(wrapped, cl.fromUser() ? 0xFFCFEFFF : 0xFFE8E8F0, null));
                }
            }
            out.add(new Line("", 0, null));
        }
        if (DrevClient.get().ai().isBusy()) {
            int dots = (int) (System.currentTimeMillis() / 400 % 4);
            out.add(new Line("§dDrevAI §7is thinking" + ".".repeat(dots), 0xFFFFFFFF, null));
        }
        return out;
    }

    private static List<String> wrap(Font f, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) { lines.add(""); return lines; }
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (f.width(candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (current.length() > 0) lines.add(current.toString());
            // a single word wider than the box gets cut
            String w = word;
            while (f.width(w) > maxWidth) {
                String part = f.plainSubstrByWidth(w, maxWidth);
                if (part.isEmpty()) break;
                lines.add(part);
                w = w.substring(part.length());
            }
            current.setLength(0);
            current.append(w);
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        int l = left(), r = right();
        int top = 30, bottom = height - 36;
        g.fill(l, top - 22, r, height - 8, 0xE0101018);
        g.fill(l, top - 22, r, top - 21, accent());
        g.drawString(font, "§lDrevAI", l + 6, top - 16, accent(), true);
        DrevAIModule ai = DrevClient.get().modules().get(DrevAIModule.class);
        String info = ai.model.get() + (ai.webSearch.isOn() ? " • web search on" : "");
        g.drawString(font, info, r - 6 - font.width(info), top - 16, 0xFF808090, false);

        List<Line> lines = buildLines(r - l - 16);
        if (lines.isEmpty()) {
            String hint = ai.apiKey.get().isBlank()
                ? "Set your Anthropic API key first: .ai key <key>"
                : "Ask about any module, command, or Minecraft in general.";
            g.drawString(font, hint, l + 8, top + 4, 0xFF9090A0, false);
        }

        int lineH = 10;
        int areaH = bottom - top;
        int total = lines.size() * lineH;
        int maxScroll = Math.max(0, total - areaH);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        // scroll = pixels scrolled up from the newest line
        int y = total > areaH ? bottom - total + scroll : top;

        clickables.clear();
        g.enableScissor(l, top, r, bottom);
        for (Line line : lines) {
            if (y + lineH >= top && y <= bottom) {
                if (line.command() != null) {
                    int w = font.width(line.text());
                    boolean hover = mouseX >= l + 8 && mouseX <= l + 8 + w && mouseY >= y && mouseY < y + lineH;
                    if (hover) g.fill(l + 6, y - 1, l + 10 + w, y + 9, 0x40FFFFFF);
                    clickables.add(new Clickable(l + 8, y, w, line.command()));
                }
                g.drawString(font, line.text(), l + 8, y, line.color(), false);
            }
            y += lineH;
        }
        g.disableScissor();

        super.render(g, mouseX, mouseY, pt);
    }

    private void send() {
        String q = input.getValue().trim();
        if (q.isEmpty()) return;
        input.setValue("");
        scroll = 0;
        DrevClient.get().ai().ask(q, answer -> scroll = 0);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            send();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (Clickable c : clickables) {
            if (event.x() >= c.x() && event.x() <= c.x() + c.w() && event.y() >= c.y() && event.y() < c.y() + 10) {
                String prefix = DrevClient.get().commands().getPrefix();
                DrevClient.get().commands().execute(c.command().substring(prefix.length()));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scroll += (int) (v * 20);
        return true;
    }
}

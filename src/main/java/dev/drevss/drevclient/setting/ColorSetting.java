package dev.drevss.drevclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.awt.Color;

/** ARGB color. The GUI edits it as a hue slider; chat accepts #RRGGBB or #AARRGGBB. */
public class ColorSetting extends Setting<Integer> {
    private boolean rainbow;

    public ColorSetting(String name, String description, int argb) {
        super(name, description, argb);
    }

    public int getColor() {
        if (rainbow) {
            float hue = (System.currentTimeMillis() % 4000L) / 4000f;
            int rgb = Color.HSBtoRGB(hue, 0.7f, 1f) & 0xFFFFFF;
            return (value & 0xFF000000) | rgb;
        }
        return value;
    }

    /** Same color with a different alpha (0-255). */
    public int withAlpha(int alpha) { return (alpha << 24) | (getColor() & 0xFFFFFF); }

    public float getHue() {
        int c = value;
        float[] hsb = Color.RGBtoHSB((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, null);
        return hsb[0];
    }

    public void setHue(float hue) {
        int rgb = Color.HSBtoRGB(hue, 0.75f, 1f) & 0xFFFFFF;
        value = (value & 0xFF000000) | rgb;
    }

    public boolean isRainbow() { return rainbow; }
    public void setRainbow(boolean rainbow) { this.rainbow = rainbow; }

    @Override public String getDisplayValue() { return rainbow ? "Rainbow" : String.format("#%08X", value); }

    @Override
    public boolean parse(String input) {
        if (input.equalsIgnoreCase("rainbow")) { rainbow = !rainbow; return true; }
        String s = input.startsWith("#") ? input.substring(1) : input;
        try {
            long v = Long.parseLong(s, 16);
            if (s.length() <= 6) v |= 0xFF000000L;
            value = (int) v;
            rainbow = false;
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override public JsonElement toJson() { return new JsonPrimitive((rainbow ? "R" : "") + Integer.toHexString(value)); }

    @Override
    public void fromJson(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) return;
        String s = element.getAsString();
        rainbow = s.startsWith("R");
        if (rainbow) s = s.substring(1);
        try { value = (int) Long.parseLong(s, 16); } catch (NumberFormatException ignored) { }
    }
}

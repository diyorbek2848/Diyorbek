package dev.drevss.drevclient.util;

import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Converts GLFW key codes to readable names and back (used by .bind and the GUI). */
public final class KeyUtil {
    private static final Map<String, Integer> NAMES = new LinkedHashMap<>();

    static {
        for (char c = 'A'; c <= 'Z'; c++) NAMES.put(String.valueOf(c), GLFW.GLFW_KEY_A + (c - 'A'));
        for (char c = '0'; c <= '9'; c++) NAMES.put(String.valueOf(c), GLFW.GLFW_KEY_0 + (c - '0'));
        for (int i = 1; i <= 25; i++) NAMES.put("F" + i, GLFW.GLFW_KEY_F1 + i - 1);
        for (int i = 0; i <= 9; i++) NAMES.put("NUMPAD" + i, GLFW.GLFW_KEY_KP_0 + i);
        NAMES.put("RSHIFT", GLFW.GLFW_KEY_RIGHT_SHIFT);
        NAMES.put("LSHIFT", GLFW.GLFW_KEY_LEFT_SHIFT);
        NAMES.put("RCTRL", GLFW.GLFW_KEY_RIGHT_CONTROL);
        NAMES.put("LCTRL", GLFW.GLFW_KEY_LEFT_CONTROL);
        NAMES.put("RALT", GLFW.GLFW_KEY_RIGHT_ALT);
        NAMES.put("LALT", GLFW.GLFW_KEY_LEFT_ALT);
        NAMES.put("TAB", GLFW.GLFW_KEY_TAB);
        NAMES.put("CAPSLOCK", GLFW.GLFW_KEY_CAPS_LOCK);
        NAMES.put("SPACE", GLFW.GLFW_KEY_SPACE);
        NAMES.put("ENTER", GLFW.GLFW_KEY_ENTER);
        NAMES.put("BACKSPACE", GLFW.GLFW_KEY_BACKSPACE);
        NAMES.put("DELETE", GLFW.GLFW_KEY_DELETE);
        NAMES.put("INSERT", GLFW.GLFW_KEY_INSERT);
        NAMES.put("HOME", GLFW.GLFW_KEY_HOME);
        NAMES.put("END", GLFW.GLFW_KEY_END);
        NAMES.put("PAGEUP", GLFW.GLFW_KEY_PAGE_UP);
        NAMES.put("PAGEDOWN", GLFW.GLFW_KEY_PAGE_DOWN);
        NAMES.put("UP", GLFW.GLFW_KEY_UP);
        NAMES.put("DOWN", GLFW.GLFW_KEY_DOWN);
        NAMES.put("LEFT", GLFW.GLFW_KEY_LEFT);
        NAMES.put("RIGHT", GLFW.GLFW_KEY_RIGHT);
        NAMES.put("GRAVE", GLFW.GLFW_KEY_GRAVE_ACCENT);
        NAMES.put("MINUS", GLFW.GLFW_KEY_MINUS);
        NAMES.put("EQUALS", GLFW.GLFW_KEY_EQUAL);
        NAMES.put("LBRACKET", GLFW.GLFW_KEY_LEFT_BRACKET);
        NAMES.put("RBRACKET", GLFW.GLFW_KEY_RIGHT_BRACKET);
        NAMES.put("SEMICOLON", GLFW.GLFW_KEY_SEMICOLON);
        NAMES.put("APOSTROPHE", GLFW.GLFW_KEY_APOSTROPHE);
        NAMES.put("COMMA", GLFW.GLFW_KEY_COMMA);
        NAMES.put("PERIOD", GLFW.GLFW_KEY_PERIOD);
        NAMES.put("SLASH", GLFW.GLFW_KEY_SLASH);
        NAMES.put("BACKSLASH", GLFW.GLFW_KEY_BACKSLASH);
    }

    private KeyUtil() { }

    public static String name(int key) {
        if (key < 0) return "NONE";
        for (Map.Entry<String, Integer> e : NAMES.entrySet()) {
            if (e.getValue() == key) return e.getKey();
        }
        return "KEY" + key;
    }

    /** Returns -1 for NONE / unknown. */
    public static int parse(String name) {
        String n = name.toUpperCase(Locale.ROOT);
        if (n.equals("NONE") || n.equals("CLEAR")) return -1;
        Integer k = NAMES.get(n);
        if (k != null) return k;
        if (n.startsWith("KEY")) {
            try { return Integer.parseInt(n.substring(3)); } catch (NumberFormatException ignored) { }
        }
        return -2; // invalid
    }

    /** Typed character for letters/digits/space/punctuation, or 0. */
    public static char toChar(int key, boolean shift) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            char c = (char) ('a' + (key - GLFW.GLFW_KEY_A));
            return shift ? Character.toUpperCase(c) : c;
        }
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) return (char) ('0' + (key - GLFW.GLFW_KEY_0));
        if (key == GLFW.GLFW_KEY_SPACE) return ' ';
        if (key == GLFW.GLFW_KEY_MINUS) return shift ? '_' : '-';
        if (key == GLFW.GLFW_KEY_PERIOD) return '.';
        return 0;
    }
}

package dev.drevss.drevclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String name, String description, boolean defaultValue) {
        super(name, description, defaultValue);
    }

    public boolean isOn() { return value; }
    public void toggle() { value = !value; }

    @Override public String getDisplayValue() { return value ? "ON" : "OFF"; }

    @Override
    public boolean parse(String input) {
        String s = input.toLowerCase();
        if (s.equals("true") || s.equals("on") || s.equals("yes") || s.equals("1")) { value = true; return true; }
        if (s.equals("false") || s.equals("off") || s.equals("no") || s.equals("0")) { value = false; return true; }
        if (s.equals("toggle")) { toggle(); return true; }
        return false;
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) value = element.getAsBoolean();
    }
}

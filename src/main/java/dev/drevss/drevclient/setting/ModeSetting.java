package dev.drevss.drevclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description, defaultValue);
        this.modes = Arrays.asList(modes);
    }

    public List<String> getModes() { return modes; }
    public boolean is(String mode) { return value.equalsIgnoreCase(mode); }

    public void cycle(boolean forward) {
        int i = modes.indexOf(value);
        i = forward ? (i + 1) % modes.size() : (i - 1 + modes.size()) % modes.size();
        value = modes.get(i);
    }

    @Override public String getDisplayValue() { return value; }

    @Override
    public boolean parse(String input) {
        for (String m : modes) {
            if (m.equalsIgnoreCase(input)) { value = m; return true; }
        }
        return false;
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) parse(element.getAsString());
    }
}

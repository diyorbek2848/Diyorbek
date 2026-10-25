package dev.drevss.drevclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

/** Free text, edited with .settings <module> <setting> <text>. */
public class TextSetting extends Setting<String> {
    public TextSetting(String name, String description, String defaultValue) {
        super(name, description, defaultValue);
    }

    /** Splits the text on commas, trimming each part. */
    public List<String> asList() {
        List<String> out = new ArrayList<>();
        for (String part : value.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    @Override
    public String getDisplayValue() {
        return value.length() > 18 ? value.substring(0, 16) + ".." : value;
    }

    @Override public boolean parse(String input) { value = input; return true; }
    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) value = element.getAsString();
    }
}

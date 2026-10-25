package dev.drevss.drevclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class NumberSetting extends Setting<Double> {
    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(String name, String description, double defaultValue, double min, double max, double step) {
        super(name, description, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getStep() { return step; }

    public double getValue() { return value; }
    public float getFloat() { return value.floatValue(); }
    public int getInt() { return (int) Math.round(value); }

    @Override
    public void set(Double v) {
        double clamped = Math.max(min, Math.min(max, v));
        if (step > 0) clamped = Math.round(clamped / step) * step;
        // avoid floating point noise like 0.30000000000000004
        clamped = Math.round(clamped * 10000.0) / 10000.0;
        this.value = clamped;
    }

    /** 0..1 position of the value on the slider. */
    public double getPercent() { return (value - min) / (max - min); }

    public void setPercent(double p) { set(min + (max - min) * Math.max(0, Math.min(1, p))); }

    @Override
    public String getDisplayValue() {
        if (step >= 1) return String.valueOf((long) Math.round(value));
        if (step >= 0.1) return String.format("%.1f", value);
        return String.format("%.2f", value);
    }

    @Override
    public boolean parse(String input) {
        try { set(Double.parseDouble(input)); return true; }
        catch (NumberFormatException e) { return false; }
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }

    @Override
    public void fromJson(JsonElement element) {
        if (element != null && element.isJsonPrimitive()) set(element.getAsDouble());
    }
}

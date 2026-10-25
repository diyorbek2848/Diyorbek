package dev.drevss.drevclient.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/** Base class for every module option shown in the ClickGUI and in .settings. */
public abstract class Setting<T> {
    private final String name;
    private final String description;
    protected T value;
    private final T defaultValue;
    private BooleanSupplier visibility = () -> true;

    protected Setting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }
    public T getDefault() { return defaultValue; }
    public void reset() { this.value = defaultValue; }

    public boolean isVisible() { return visibility.getAsBoolean(); }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier supplier) {
        this.visibility = supplier;
        return (S) this;
    }

    /** Text shown in the GUI / chat. */
    public abstract String getDisplayValue();

    /** Parses a value typed in chat. Returns false if the input is invalid. */
    public abstract boolean parse(String input);

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement element);
}

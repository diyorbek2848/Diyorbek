package dev.drevss.drevclient.module;

public enum Category {
    COMBAT("Combat"),
    PLAYER("Player"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    WORLD("World"),
    MISC("Misc"),
    CLIENT("Client");

    public final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }
}

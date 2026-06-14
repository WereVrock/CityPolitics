package City.ui.map;

public enum MapViewMode {
    SETTLEMENT, PHYSICAL, POLITICAL;

    public MapViewMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String label() {
        return switch (this) {
            case SETTLEMENT -> "⚑ SETTLEMENT";
            case PHYSICAL   -> "⛰ PHYSICAL";
            case POLITICAL  -> "⚔ POLITICAL";
        };
    }
}
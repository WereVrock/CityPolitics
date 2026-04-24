package main.politics;

/**
 * Represents how strongly a party holds a political view.
 * Used to scale vote score contributions.
 */
public enum ViewStrength {

    STRONGLY_FOR  ( 1.0),
    FOR           ( 0.5),
    NEUTRAL       ( 0.0),
    AGAINST       (-0.5),
    STRONGLY_AGAINST(-1.0);

    private final double multiplier;

    ViewStrength(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() { return multiplier; }
}
package City.main.nobles;

import City.main.parameters.GameParameters;

/**
 * Seven prestige levels derived from a hidden 0–100 int.
 * Display names are intentionally flavourful, not mechanical.
 */
public enum Prestige {

    EXALTED   (85, "Exalted"),
    RENOWNED  (70, "Renowned"),
    RESPECTED (55, "Respected"),
    ESTABLISHED(40, "Established"),
    DIMINISHED (25, "Diminished"),
    DISGRACED  (10, "Disgraced"),
    RUINED     (0,  "Ruined");

    private final int    threshold;   // minimum value for this level
    private final String displayName;

    Prestige(int threshold, String displayName) {
        this.threshold   = threshold;
        this.displayName = displayName;
    }

    public int    getThreshold()  { return threshold; }
    public String getDisplayName(){ return displayName; }

    /** Resolve a 0–100 prestige value to its display level. */
    public static Prestige fromValue(int value) {
        for (Prestige p : values()) {
            if (value >= p.threshold) return p;
        }
        return RUINED;
    }
}
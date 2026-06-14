package City.main.legislation;

/**
 * All legislation types the player can propose.
 */
public enum LegislationType {
    MERCENARY_ALLOWANCE_LAW("Mercenary Allowance Law",
        "Legalises the use of mercenary companies. If passed, enables voting to hire mercenaries each time."),
    MERCENARY_AUTHORIZATION_LAW("Mercenary Authorization Law",
        "Permanently authorises mercenary hiring without council approval each time. Requires Mercenary Allowance Law to already be passed."),
    WARTIME_TAXES_LAW("Wartime Taxes Law",
        "Establishes emergency taxation powers during wartime. If passed, enables Wartime Taxes action when at war.");

    private final String displayName;
    private final String description;

    LegislationType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
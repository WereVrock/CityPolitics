package City.main.nobles.council;

/**
 * All actions the player can bring to the noble council.
 */
public enum CouncilAction {

    FORTIFICATION_SUPPORT(
        "Fortification Support",
        "The player pays half the fortification cost for noble houses for 3 years. "
        + "Expansionist houses in strong positions may object.",
        3
    ),

    BORDER_FORTIFICATION(
        "Border Fortification",
        "Fortifications on zones adjacent to desolate wasteland are increased by 1. "
        + "The player pays the full cost. Rivals of benefitting houses disagree.",
        0
    ),

    UNLAWFUL_ACQUISITION(
        "Declare Unlawful Acquisition",
        "A selected zone's owner is stripped of their claim and must cede it to another claimant.",
        0
    );

    private final String displayName;
    private final String description;
    private final int    durationYears; // 0 = instant

    CouncilAction(String displayName, String description, int durationYears) {
        this.displayName   = displayName;
        this.description   = description;
        this.durationYears = durationYears;
    }

    public String getDisplayName()  { return displayName; }

public String getDescription() { return description; }

public int    getDurationYears(){ return durationYears; }
}
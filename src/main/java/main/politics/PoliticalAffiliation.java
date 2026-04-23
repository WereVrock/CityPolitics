package main.politics;

/**
 * The three political factions that pops can belong to.
 * Eligibility rules and base loyalty weights are stored here.
 */
public enum PoliticalAffiliation {

    NONE(
        "None",
        "No political alignment.",
        false, false, false, false
    ),

    HUMAN_SUPREMACISTS(
        "Human Supremacists",
        "Believe humans alone should rule. Reject all other races.",
        true,  // humans eligible
        false, // dwarves not eligible
        false, // orcs not eligible
        false  // elves not eligible
    ),

    ENVIRONMENTALISTS(
        "Environmentalists",
        "Protect the land and its creatures. Distrust industry and mining.",
        true,  // humans eligible
        false, // dwarves NOT eligible (miners/industry)
        true,  // orcs eligible
        true   // elves eligible
    ),

    WAR_MONGERERS(
        "War Mongerers",
        "Glory through conquest. Orcs flock to this cause.",
        true,  // humans eligible
        true,  // dwarves eligible
        true,  // orcs eligible (higher base weight)
        false  // elves not eligible
    );

    private final String displayName;
    private final String description;
    private final boolean humansEligible;
    private final boolean dwarvesEligible;
    private final boolean orcsEligible;
    private final boolean elvesEligible;

    PoliticalAffiliation(
            String displayName,
            String description,
            boolean humansEligible,
            boolean dwarvesEligible,
            boolean orcsEligible,
            boolean elvesEligible) {
        this.displayName     = displayName;
        this.description     = description;
        this.humansEligible  = humansEligible;
        this.dwarvesEligible = dwarvesEligible;
        this.orcsEligible    = orcsEligible;
        this.elvesEligible   = elvesEligible;
    }

    public String getDisplayName()    { return displayName; }
    public String getDescription()    { return description; }
    public boolean isHumansEligible() { return humansEligible; }
    public boolean isDwarvesEligible(){ return dwarvesEligible; }
    public boolean isOrcsEligible()   { return orcsEligible; }
    public boolean isElvesEligible()  { return elvesEligible; }
}